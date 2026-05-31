package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.OperatingMode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AyanAgentService {

    private final ChatClient chatClient;

    public AyanAgentService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatResult chatWithToolCalls(String conversationId,
                                        String userMessage,
                                        OperatingMode mode,
                                        List<String> filePaths,
                                        String currentDir) {
        final var convId = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;

        final var chatResponse = chatClient.prompt()
                .user(buildContextPrefix(mode, filePaths, currentDir) + userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId))
                .call()
                .chatResponse();

        final var reply = chatResponse != null && chatResponse.getResult() != null
                ? chatResponse.getResult().getOutput().getText()
                : "";
        return new ChatResult(reply, convId, extractToolCalls(chatResponse));
    }

    public Flux<StreamEvent> chatStreamWithToolCalls(String conversationId,
                                                     String userMessage,
                                                     OperatingMode mode,
                                                     List<String> filePaths,
                                                     String currentDir) {
        final var convId = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;
        return chatClient.prompt()
                .user(buildContextPrefix(mode, filePaths, currentDir) + userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId))
                .stream()
                .chatResponse()
                .flatMap(this::toStreamEvents);
    }

    private String buildContextPrefix(OperatingMode mode, List<String> filePaths, String currentDir) {
        final var sb = new StringBuilder("[Contexte: mode=").append(mode.name());
        sb.append("; filePaths=[");
        if (filePaths != null && !filePaths.isEmpty()) {
            sb.append(filePaths.stream()
                    .map(p -> "\"" + p.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                    .collect(Collectors.joining(",")));
        }
        sb.append("]");
        if (currentDir != null && !currentDir.isBlank()) {
            sb.append("; currentDir=\"")
                    .append(currentDir.replace("\\", "\\\\").replace("\"", "\\\""))
                    .append("\"");
        }
        sb.append("]\n");
        return sb.toString();
    }

    private List<ToolCall> extractToolCalls(ChatResponse chatResponse) {
        if (chatResponse == null) {
            return List.of();
        }
        return chatResponse.getResults().stream()
                .map(r -> r.getOutput().getToolCalls())
                .filter(list -> list != null && !list.isEmpty())
                .flatMap(List::stream)
                .map(tc -> new ToolCall(tc.id(), tc.name(), tc.arguments()))
                .toList();
    }

    private Flux<StreamEvent> toStreamEvents(ChatResponse chunk) {
        if (chunk == null || chunk.getResult() == null) {
            return Flux.empty();
        }
        final var events = new ArrayList<StreamEvent>(2);
        final var output = chunk.getResult().getOutput();
        final var token = output.getText();
        if (token != null && !token.isEmpty()) {
            events.add(StreamEvent.token(token));
        }
        final var toolCalls = output.getToolCalls();
        if (toolCalls != null) {
            for (final var tc : toolCalls) {
                events.add(StreamEvent.toolCall(tc.id(), tc.name(), tc.arguments()));
            }
        }
        return Flux.fromIterable(events);
    }

    public record ChatResult(String reply, String conversationId, List<ToolCall> toolCalls) {
        public ChatResult {
            toolCalls = toolCalls == null ? List.of() : Collections.unmodifiableList(toolCalls);
        }
    }

    public record ToolCall(String id, String name, String argumentsJson) {}

    public enum StreamEventKind { TOKEN, TOOL_CALL }

    public record StreamEvent(
            StreamEventKind kind,
            String token,
            String toolCallId,
            String toolName,
            String toolArgsJson
    ) {
        public static StreamEvent token(String token) {
            return new StreamEvent(StreamEventKind.TOKEN, token, null, null, null);
        }
        public static StreamEvent toolCall(String id, String name, String argsJson) {
            return new StreamEvent(StreamEventKind.TOOL_CALL, null, id, name, argsJson);
        }
    }
}
