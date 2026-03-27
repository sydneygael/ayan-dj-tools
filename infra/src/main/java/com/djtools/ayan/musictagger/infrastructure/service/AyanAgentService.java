package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.OperatingMode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AyanAgentService {

    private static final int MAX_HISTORY_MESSAGES = 10;

    private final ChatClient chatClient;
    private final ConversationHistoryService historyService;

    public AyanAgentService(ChatClient chatClient, ConversationHistoryService historyService) {
        this.chatClient = chatClient;
        this.historyService = historyService;
    }

    public String chat(String conversationId, String userMessage) {
        return chat(conversationId, userMessage, OperatingMode.PLAN);
    }

    public String chat(String conversationId, String userMessage, OperatingMode mode) {
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        historyService.saveMessage(conversationId, new ChatMessage("user", userMessage, LocalDateTime.now()));

        final var history = historyService.getHistory(conversationId);
        final var contextualPrompt = buildPromptWithHistory(history, userMessage, mode);

        final var reply = chatClient.prompt()
                .user(contextualPrompt)
                .call()
                .content();

        historyService.saveMessage(conversationId, new ChatMessage("assistant", reply, LocalDateTime.now()));

        return reply;
    }

    public String getConversationId(String conversationId) {
        return (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;
    }

    private String buildPromptWithHistory(List<ChatMessage> history, String currentMessage, OperatingMode mode) {
        final var modePrefix = "[Mode actif : %s]\n\n".formatted(mode.name());

        if (history.size() <= 1) {
            return modePrefix + currentMessage;
        }

        final var recentHistory = history.size() > MAX_HISTORY_MESSAGES
                ? history.subList(history.size() - MAX_HISTORY_MESSAGES, history.size() - 1)
                : history.subList(0, history.size() - 1);

        final var context = recentHistory.stream()
                .map(msg -> "[%s]: %s".formatted(msg.role(), msg.content()))
                .collect(Collectors.joining("\n"));

        return """
                %sContexte de la conversation précédente :
                %s

                Nouveau message de l'utilisateur :
                %s""".formatted(modePrefix, context, currentMessage);
    }
}
