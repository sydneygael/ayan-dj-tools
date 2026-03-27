package com.djtools.ayan.musictagger.infrastructure.adapter.in.ws;

import com.djtools.ayan.musictagger.infrastructure.adapter.in.rest.AgentController.ChatResponse;
import com.djtools.ayan.musictagger.infrastructure.service.AyanAgentService;
import com.djtools.ayan.musictagger.infrastructure.service.ConversationHistoryService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
public class AgentWebSocketController {

    private final AyanAgentService agentService;
    private final ConversationHistoryService historyService;

    public AgentWebSocketController(AyanAgentService agentService, ConversationHistoryService historyService) {
        this.agentService = agentService;
        this.historyService = historyService;
    }

    @MessageMapping("/chat")
    @SendTo("/topic/responses")
    public ChatResponse handleChat(ChatRequest request) {
        String conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID().toString();

        final var reply = agentService.chat(conversationId, request.message());
        final var messageCount = historyService.getMessageCount(conversationId);

        return new ChatResponse(reply, conversationId, messageCount, LocalDateTime.now());
    }

    public record ChatRequest(String message, String conversationId) {}
}
