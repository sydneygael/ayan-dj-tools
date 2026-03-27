package com.djtools.ayan.musictagger.infrastructure.adapter.in.ws;

import com.djtools.ayan.musictagger.infrastructure.service.AyanAgentService;
import com.djtools.ayan.musictagger.infrastructure.service.ChatMessage;
import com.djtools.ayan.musictagger.infrastructure.service.ConversationHistoryService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class AgentWebSocketController {

    private static final System.Logger log = System.getLogger(AgentWebSocketController.class.getName());

    private final AyanAgentService agentService;
    private final ConversationHistoryService historyService;
    private final SimpMessagingTemplate messagingTemplate;
    /** Flux actifs par conversationId — permet l'annulation à la demande. */
    private final ConcurrentHashMap<String, Disposable> activeStreams = new ConcurrentHashMap<>();

    public AgentWebSocketController(AyanAgentService agentService, ConversationHistoryService historyService,
                                    SimpMessagingTemplate messagingTemplate) {
        this.agentService = agentService;
        this.historyService = historyService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Reçoit un message chat via STOMP et stream la réponse token par token.
     * Chaque token est publié sur {@code /topic/responses/{conversationId}} en tant que {@code chunk}.
     * Un événement {@code done} final est envoyé à la fin avec la réponse complète.
     * Le {@link Disposable} du flux est conservé pour permettre l'annulation via {@code /app/chat/stop}.
     */
    @MessageMapping("/chat")
    public void handleChat(ChatRequest request) {
        final var conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID().toString();
        final var topic = "/topic/responses/" + conversationId;
        final var fullReply = new StringBuilder();

        final var disposable = agentService.chatStream(conversationId, request.message())
                .doOnNext(chunk -> {
                    fullReply.append(chunk);
                    messagingTemplate.convertAndSend(topic,
                            new ChatStreamEvent("chunk", chunk, null, conversationId, null, null));
                })
                .doOnComplete(() -> {
                    activeStreams.remove(conversationId);
                    historyService.saveMessage(conversationId,
                            new ChatMessage("assistant", fullReply.toString(), LocalDateTime.now()));
                    final var messageCount = historyService.getMessageCount(conversationId);
                    messagingTemplate.convertAndSend(topic,
                            new ChatStreamEvent("done", null, fullReply.toString(), conversationId, messageCount, LocalDateTime.now()));
                })
                .doOnError(error -> {
                    activeStreams.remove(conversationId);
                    log.log(System.Logger.Level.WARNING, "Erreur streaming conversation {0} : {1}",
                            conversationId, error.getMessage());
                    messagingTemplate.convertAndSend(topic,
                            new ChatStreamEvent("error", error.getMessage(), null, conversationId, null, null));
                })
                .doOnCancel(() -> {
                    activeStreams.remove(conversationId);
                    if (!fullReply.isEmpty()) {
                        historyService.saveMessage(conversationId,
                                new ChatMessage("assistant", fullReply.toString(), LocalDateTime.now()));
                    }
                    messagingTemplate.convertAndSend(topic,
                            new ChatStreamEvent("interrupted", null, fullReply.toString(), conversationId, null, null));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();

        activeStreams.put(conversationId, disposable);
    }

    /** Annule le flux de génération en cours pour la conversation donnée. */
    @MessageMapping("/chat/stop")
    public void stopChat(StopRequest request) {
        final var disposable = activeStreams.remove(request.conversationId());
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    public record ChatRequest(String message, String conversationId) {}
    public record StopRequest(String conversationId) {}

    public record ChatStreamEvent(
            String type,
            String token,
            String reply,
            String conversationId,
            Long messageCount,
            LocalDateTime timestamp) {}
}
