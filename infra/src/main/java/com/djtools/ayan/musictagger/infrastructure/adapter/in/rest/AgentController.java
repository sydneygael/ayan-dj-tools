package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.OperatingMode;
import com.djtools.ayan.musictagger.infrastructure.service.AyanAgentService;
import com.djtools.ayan.musictagger.infrastructure.service.ChatMessage;
import com.djtools.ayan.musictagger.infrastructure.service.ConversationHistoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agent")
@Tag(name = "Agent", description = "Chat IA Ayan — enrichissement conversationnel")
public class AgentController {

    private final AyanAgentService agentService;
    private final ConversationHistoryService historyService;

    public AgentController(AyanAgentService agentService, ConversationHistoryService historyService) {
        this.agentService = agentService;
        this.historyService = historyService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID().toString();

        final var mode = request.mode() != null ? request.mode() : OperatingMode.PLAN;
        final var result = agentService.chatWithToolCalls(
                conversationId, request.message(), mode, request.filePaths(), request.currentDir());
        final var messageCount = historyService.getMessageCount(conversationId);
        final var toolCalls = result.toolCalls().stream()
                .map(tc -> new ToolCallInfo(tc.id(), tc.name(), tc.argumentsJson()))
                .toList();

        return new ChatResponse(result.reply(), conversationId, messageCount, LocalDateTime.now(), toolCalls);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatStreamEvent> chatStream(@RequestBody ChatRequest request) {
        final var conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID().toString();

        final var mode = request.mode() != null ? request.mode() : OperatingMode.PLAN;
        final var fullReply = new StringBuilder();

        return agentService.chatStreamWithToolCalls(
                        conversationId, request.message(), mode, request.filePaths(), request.currentDir())
                .map(event -> switch (event.kind()) {
                    case TOKEN -> {
                        fullReply.append(event.token());
                        yield new ChatStreamEvent(
                                "chunk", event.token(), null, conversationId, null, null,
                                null, null, null, null);
                    }
                    case TOOL_CALL -> new ChatStreamEvent(
                            "tool-call", null, null, conversationId, null, LocalDateTime.now(),
                            event.toolCallId(), event.toolName(), event.toolArgsJson(), null);
                })
                .concatWith(Mono.fromSupplier(() -> {
                    final var reply = fullReply.toString();
                    historyService.saveMessage(conversationId,
                            new ChatMessage("assistant", reply, LocalDateTime.now()));
                    final var messageCount = historyService.getMessageCount(conversationId);
                    return new ChatStreamEvent(
                            "done", null, reply, conversationId, messageCount, LocalDateTime.now(),
                            null, null, null, null);
                }))
                .onErrorResume(error -> Flux.just(
                        new ChatStreamEvent(
                                "error", error.getMessage(), null, conversationId, null, null,
                                null, null, null, null)
                ));
    }

    @GetMapping("/conversations/{id}/history")
    public List<ChatMessage> getHistory(@PathVariable String id) {
        return historyService.getHistory(id);
    }

    @DeleteMapping("/conversations/{id}")
    public void clearConversation(@PathVariable String id) {
        historyService.clearHistory(id);
    }

    public record ChatRequest(
            String message,
            String conversationId,
            OperatingMode mode,
            List<String> filePaths,
            String currentDir
    ) {}

    public record ChatResponse(
            String reply,
            String conversationId,
            long messageCount,
            LocalDateTime timestamp,
            List<ToolCallInfo> toolCalls
    ) {}

    public record ToolCallInfo(String id, String name, String argumentsJson) {}

    public record ChatStreamEvent(
            String type,
            String token,
            String reply,
            String conversationId,
            Long messageCount,
            LocalDateTime timestamp,
            String toolCallId,
            String toolName,
            String toolArgsJson,
            String toolResultJson
    ) {}
}
