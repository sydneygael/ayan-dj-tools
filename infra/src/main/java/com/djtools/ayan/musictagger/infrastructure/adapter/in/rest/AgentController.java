package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.OperatingMode;
import com.djtools.ayan.musictagger.infrastructure.service.AyanAgentService;
import com.djtools.ayan.musictagger.infrastructure.service.ChatMessage;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agent")
@Tag(name = "Agent", description = "Chat IA Ayan — enrichissement conversationnel")
public class AgentController {

    private final AyanAgentService agentService;
    private final ChatMemory chatMemory;

    public AgentController(AyanAgentService agentService, ChatMemory chatMemory) {
        this.agentService = agentService;
        this.chatMemory = chatMemory;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        final var conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID().toString();

        final var mode = request.mode() != null ? request.mode() : OperatingMode.PLAN;
        final var result = agentService.chatWithToolCalls(
                conversationId, request.message(), mode, request.filePaths(), request.currentDir());
        final var messageCount = chatMemory.get(result.conversationId()).size();
        final var toolCalls = result.toolCalls().stream()
                .map(tc -> new ToolCallInfo(tc.id(), tc.name(), tc.argumentsJson()))
                .toList();

        return new ChatResponse(result.reply(), result.conversationId(), messageCount, LocalDateTime.now(), toolCalls);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        final var mode = request.mode() != null ? request.mode() : OperatingMode.PLAN;
        return agentService.streamChat(
                request.conversationId(), request.message(), mode,
                request.filePaths(), request.currentDir());
    }

    @GetMapping("/conversations/{id}/history")
    public List<ChatMessage> getHistory(@PathVariable String id) {
        return chatMemory.get(id).stream()
                .map(msg -> new ChatMessage(msg.getMessageType().getValue(), msg.getText(), null))
                .toList();
    }

    @DeleteMapping("/conversations/{id}")
    public void clearConversation(@PathVariable String id) {
        chatMemory.clear(id);
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
}
