package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.OperatingMode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AyanAgentService {

    private static final int MAX_HISTORY_MESSAGES = 4;

    private final ChatClient chatClient;
    private final ConversationHistoryService historyService;

    public AyanAgentService(ChatClient chatClient, ConversationHistoryService historyService) {
        this.chatClient = chatClient;
        this.historyService = historyService;
    }

    /**
     * Chat bloquant retournant la réponse texte + la liste des tool calls invoqués par le LLM.
     * Le contexte (filePaths, currentDir) est injecté en préfixe du UserMessage.
     */
    public ChatResult chatWithToolCalls(String conversationId,
                                        String userMessage,
                                        OperatingMode mode,
                                        List<String> filePaths,
                                        String currentDir) {
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }
        final var messages = prepareMessages(conversationId, userMessage, mode, filePaths, currentDir);

        final var chatResponse = chatClient.prompt()
                .messages(messages)
                .call()
                .chatResponse();

        final var reply = chatResponse != null && chatResponse.getResult() != null
                ? chatResponse.getResult().getOutput().getText()
                : "";
        final var toolCalls = extractToolCalls(chatResponse);

        historyService.saveMessage(conversationId, new ChatMessage("assistant", reply, LocalDateTime.now()));
        return new ChatResult(reply, toolCalls);
    }

    /**
     * Chat en streaming retournant un Flux d'événements : TOKEN (chunk de texte) ou TOOL_CALL.
     * Le contexte (filePaths, currentDir) est injecté en préfixe du UserMessage.
     */
    public Flux<StreamEvent> chatStreamWithToolCalls(String conversationId,
                                                     String userMessage,
                                                     OperatingMode mode,
                                                     List<String> filePaths,
                                                     String currentDir) {
        final var messages = prepareMessages(conversationId, userMessage, mode, filePaths, currentDir);
        return chatClient.prompt()
                .messages(messages)
                .stream()
                .chatResponse()
                .flatMap(this::toStreamEvents);
    }

    /** Sauvegarde le message utilisateur (brut, sans préfixe), récupère l'historique, construit la liste Spring AI. */
    private List<Message> prepareMessages(String conversationId,
                                          String userMessage,
                                          OperatingMode mode,
                                          List<String> filePaths,
                                          String currentDir) {
        historyService.saveMessage(conversationId, new ChatMessage("user", userMessage, LocalDateTime.now()));
        final var history = historyService.getHistory(conversationId);
        return buildMessages(history, userMessage, mode, filePaths, currentDir);
    }

    /**
     * Construit la liste de messages Spring AI à partir de l'historique Redis.
     * Exclut le dernier message (l'actuel, déjà sauvegardé) pour éviter la duplication.
     * Limite à MAX_HISTORY_MESSAGES échanges précédents pour réduire le contexte.
     * Préfixe le message courant avec [Contexte: ...] pour donner mode + sélection à l'agent.
     */
    private List<Message> buildMessages(List<ChatMessage> history,
                                        String userMessage,
                                        OperatingMode mode,
                                        List<String> filePaths,
                                        String currentDir) {
        final var previous = history.size() > 1 ? history.subList(0, history.size() - 1) : List.<ChatMessage>of();
        final var recent = previous.size() > MAX_HISTORY_MESSAGES
                ? previous.subList(previous.size() - MAX_HISTORY_MESSAGES, previous.size())
                : previous;

        final var messages = new ArrayList<Message>(recent.size() + 1);
        for (final var msg : recent) {
            messages.add("user".equals(msg.role())
                    ? new UserMessage(msg.content())
                    : new AssistantMessage(msg.content()));
        }
        messages.add(new UserMessage(buildContextPrefix(mode, filePaths, currentDir) + userMessage));
        return messages;
    }

    /** Bâtit la ligne `[Contexte: mode=X; filePaths=[...]; currentDir="..."]\n` injectée au LLM. */
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

    /** Extrait les ToolCall d'une ChatResponse non-streaming. */
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

    /** Mappe un chunk ChatResponse de stream vers une séquence d'événements (token + tool calls). */
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

    /** Résultat d'un chat bloquant : texte de réponse + tool calls invoqués. */
    public record ChatResult(String reply, List<ToolCall> toolCalls) {
        public ChatResult {
            toolCalls = toolCalls == null ? List.of() : Collections.unmodifiableList(toolCalls);
        }
    }

    public record ToolCall(String id, String name, String argumentsJson) {}

    public enum StreamEventKind { TOKEN, TOOL_CALL }

    /** Événement émis pendant le streaming : soit un fragment de texte, soit une demande de tool call. */
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
