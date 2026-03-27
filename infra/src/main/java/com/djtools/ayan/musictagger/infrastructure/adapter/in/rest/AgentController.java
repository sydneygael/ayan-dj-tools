package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.infrastructure.service.AyanAgentService;
import com.djtools.ayan.musictagger.infrastructure.service.ChatMessage;
import com.djtools.ayan.musictagger.infrastructure.service.ConversationHistoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

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

        final var reply = agentService.chat(conversationId, request.message());
        final var messageCount = historyService.getMessageCount(conversationId);

        return new ChatResponse(reply, conversationId, messageCount, LocalDateTime.now());
    }

    @GetMapping("/conversations/{id}/history")
    public List<ChatMessage> getHistory(@PathVariable String id) {
        return historyService.getHistory(id);
    }

    @DeleteMapping("/conversations/{id}")
    public void clearConversation(@PathVariable String id) {
        historyService.clearHistory(id);
    }

    public record ChatRequest(String message, String conversationId) {}

    public record ChatResponse(String reply, String conversationId, long messageCount, LocalDateTime timestamp) {}
}
