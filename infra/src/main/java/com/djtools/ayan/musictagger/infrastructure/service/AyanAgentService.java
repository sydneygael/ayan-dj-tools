package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.OperatingMode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AyanAgentService {

    private static final int MAX_HISTORY_MESSAGES = 4;

    private final ChatClient chatClient;
    private final ConversationHistoryService historyService;

    public AyanAgentService(ChatClient chatClient, ConversationHistoryService historyService) {
        this.chatClient = chatClient;
        this.historyService = historyService;
    }

    /** Chat bloquant — utilisé par le fallback REST. */
    public String chat(String conversationId, String userMessage) {
        return chat(conversationId, userMessage, OperatingMode.PLAN);
    }

    /** Chat bloquant avec mode — utilisé par le fallback REST. */
    public String chat(String conversationId, String userMessage, OperatingMode mode) {
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }
        final var messages = prepareMessages(conversationId, userMessage, mode);
        final var reply = chatClient.prompt()
                .messages(messages)
                .call()
                .content();
        historyService.saveMessage(conversationId, new ChatMessage("assistant", reply, LocalDateTime.now()));
        return reply;
    }

    /** Chat en streaming — utilisé par le WebSocket. Émet les tokens au fil de la génération. */
    public Flux<String> chatStream(String conversationId, String userMessage) {
        return chatStream(conversationId, userMessage, OperatingMode.PLAN);
    }

    /** Chat en streaming avec mode. Sauvegarde l'historique à la fin du flux. */
    public Flux<String> chatStream(String conversationId, String userMessage, OperatingMode mode) {
        final var messages = prepareMessages(conversationId, userMessage, mode);
        final var accumulator = new StringBuilder();
        return chatClient.prompt()
                .messages(messages)
                .stream()
                .content()
                .doOnNext(accumulator::append)
                .doOnComplete(() -> historyService.saveMessage(conversationId,
                        new ChatMessage("assistant", accumulator.toString(), LocalDateTime.now())));
    }

    public String getConversationId(String conversationId) {
        return (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;
    }

    /** Sauvegarde le message utilisateur, récupère l'historique, construit la liste de messages Spring AI. */
    private List<Message> prepareMessages(String conversationId, String userMessage, OperatingMode mode) {
        historyService.saveMessage(conversationId, new ChatMessage("user", userMessage, LocalDateTime.now()));
        final var history = historyService.getHistory(conversationId);
        return buildMessages(history, userMessage, mode);
    }

    /**
     * Construit la liste de messages Spring AI à partir de l'historique Redis.
     * Exclut le dernier message (l'actuel, déjà sauvegardé) pour éviter la duplication.
     * Limite à MAX_HISTORY_MESSAGES échanges précédents pour réduire le contexte.
     */
    private List<Message> buildMessages(List<ChatMessage> history, String userMessage, OperatingMode mode) {
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
        messages.add(new UserMessage("[Mode: " + mode.name() + "] " + userMessage));
        return messages;
    }
}
