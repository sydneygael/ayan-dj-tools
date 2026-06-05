package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.OperatingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AyanAgentService {

    private static final Logger log = LoggerFactory.getLogger(AyanAgentService.class);
    private static final int HEARTBEAT_INTERVAL_SECONDS = 4;

    private final Map<IntentType, AyanAssistant> assistants;
    private final Function<String, IntentType> intentClassifier;
    private final MusicLookupService musicLookupService;
    private final int chatTimeoutSeconds;
    private final int simpleTimeoutSeconds;

    public AyanAgentService(
            @Qualifier("fichiersAssistant")    AyanAssistant fichiersAssistant,
            @Qualifier("planAssistant")        AyanAssistant planAssistant,
            @Qualifier("rechercheAssistant")   AyanAssistant rechercheAssistant,
            @Qualifier("playlistAssistant")    AyanAssistant playlistAssistant,
            @Qualifier("decouverteAssistant")  AyanAssistant decouverteAssistant,
            @Qualifier("generalAssistant")     AyanAssistant generalAssistant,
            Function<String, IntentType> intentClassifierFunction,
            MusicLookupService musicLookupService,
            @Value("${dj-tagger.chat.timeout-seconds:600}") int chatTimeoutSeconds,
            @Value("${dj-tagger.chat.simple-timeout-seconds:60}") int simpleTimeoutSeconds) {

        this.assistants = Map.of(
                IntentType.FICHIERS,     fichiersAssistant,
                IntentType.PLANIFICATION, planAssistant,
                IntentType.RECHERCHE,    rechercheAssistant,
                IntentType.PLAYLIST,     playlistAssistant,
                IntentType.DECOUVERTE,   decouverteAssistant,
                IntentType.GENERAL,      generalAssistant
        );
        this.intentClassifier = intentClassifierFunction;
        this.musicLookupService = musicLookupService;
        this.chatTimeoutSeconds = chatTimeoutSeconds;
        this.simpleTimeoutSeconds = simpleTimeoutSeconds;
    }

    public ChatResult chatWithToolCalls(String conversationId,
                                        String userMessage,
                                        OperatingMode mode,
                                        List<String> filePaths,
                                        String currentDir) {
        final var convId = resolveConvId(conversationId);
        final var shortId = shortId(convId);
        final var prompt = buildContextPrefix(mode, filePaths, currentDir) + userMessage;

        final var intent = classifyIntent(userMessage, mode);
        final var timeoutSecs = isSimpleIntent(intent) ? simpleTimeoutSeconds : chatTimeoutSeconds;
        log.info("Chat [{}] intent={} mode={} - {}", shortId, intent, mode,
                userMessage.substring(0, Math.min(60, userMessage.length())));

        // DECOUVERTE: pre-fetch lookup, then let general assistant format — avoids tool-call loop
        final var assistant = assistants.get(intent);
        final var effectivePrompt = intent == IntentType.DECOUVERTE
                ? buildDiscoveryPrompt(userMessage, prompt)
                : prompt;
        final var effectiveAssistant = intent == IntentType.DECOUVERTE
                ? assistants.get(IntentType.GENERAL)
                : assistant;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var future = executor.submit(() -> effectiveAssistant.chatSync(convId, effectivePrompt));
            try {
                final var content = future.get(timeoutSecs, TimeUnit.SECONDS);
                final var reply = (content != null && !content.isBlank())
                        ? content
                        : "Je n'ai pas pu générer de réponse. Reformule ta question ou vérifie qu'Ollama est démarré.";
                log.info("Reply [{}] - {} chars", shortId, reply.length());
                return new ChatResult(reply, convId, List.of());
            } catch (TimeoutException e) {
                future.cancel(true);
                log.warn("Chat timeout [{}] après {}s", shortId, timeoutSecs);
                return new ChatResult(
                        "Le modèle a dépassé le délai de %ds. Réessaie ou reformule ta question.".formatted(timeoutSecs),
                        convId, List.of());
            } catch (ExecutionException e) {
                final var cause = e.getCause() instanceof Exception ex ? ex : e;
                log.error("Chat error [{}]: {}", shortId, cause.getMessage(), cause);
                return new ChatResult(buildUserFacingError(cause), convId, List.of());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ChatResult("Requête interrompue.", convId, List.of());
        }
    }

    public SseEmitter streamChat(String conversationId,
                                 String userMessage,
                                 OperatingMode mode,
                                 List<String> filePaths,
                                 String currentDir) {
        final var emitter = new SseEmitter(0L);
        final var convId = resolveConvId(conversationId);
        final var shortId = shortId(convId);

        // Immediate thinking indicator — visible in < 1s
        sendSse(emitter, "{\"type\":\"thinking\",\"conversationId\":\"" + convId + "\"}");

        // Classify intent synchronously (fast model, ~1-2s)
        final var intent = classifyIntent(userMessage, mode);
        log.info("Stream [{}] intent={} mode={} - {}", shortId, intent, mode,
                userMessage.substring(0, Math.min(60, userMessage.length())));

        // DECOUVERTE: pre-fetch lookup, route to general assistant — no tool-call loop
        final var basePrompt = buildContextPrefix(mode, filePaths, currentDir) + userMessage;
        final var prompt = intent == IntentType.DECOUVERTE
                ? buildDiscoveryPrompt(userMessage, basePrompt)
                : basePrompt;
        final var assistant = intent == IntentType.DECOUVERTE
                ? assistants.get(IntentType.GENERAL)
                : assistants.get(intent);

        final var heartbeat = Executors.newSingleThreadScheduledExecutor();
        heartbeat.scheduleAtFixedRate(
                () -> sendSse(emitter, "{\"type\":\"thinking\",\"conversationId\":\"" + convId + "\"}"),
                HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);

        if (intent == IntentType.GENERAL) {
            // GENERAL has no tools — real token-by-token streaming
            final var accumulated = new StringBuilder();
            assistant.chatStream(convId, prompt)
                    .onNext(token -> {
                        accumulated.append(token);
                        sendSse(emitter, "{\"type\":\"chunk\",\"token\":" + jsonEscape(token)
                                + ",\"conversationId\":\"" + convId + "\"}");
                    })
                    .onComplete(response -> {
                        heartbeat.shutdownNow();
                        log.info("Stream done [{}] intent={} - {} chars", shortId, intent, accumulated.length());
                        sendSse(emitter, "{\"type\":\"done\",\"reply\":" + jsonEscape(accumulated.toString())
                                + ",\"conversationId\":\"" + convId
                                + "\",\"intent\":\"" + intent.name()
                                + "\",\"timestamp\":\"" + LocalDateTime.now() + "\"}");
                        emitter.complete();
                    })
                    .onError(error -> {
                        heartbeat.shutdownNow();
                        log.error("Stream error [{}]: {}", shortId, error.getMessage());
                        final var msg = buildUserFacingError(error instanceof Exception ex ? ex : new RuntimeException(error));
                        sendSse(emitter, "{\"type\":\"error\",\"reply\":" + jsonEscape(msg)
                                + ",\"conversationId\":\"" + convId + "\"}");
                        emitter.complete();
                    })
                    .start();
        } else {
            // Tool-using agents — OllamaStreamingChatModel doesn't support tools in 0.36
            // Use sync in a virtual thread; heartbeat keeps the UX alive during tool calls
            Thread.ofVirtual().start(() -> {
                try {
                    final var reply = assistant.chatSync(convId, prompt);
                    heartbeat.shutdownNow();
                    log.info("Stream done [{}] intent={} - {} chars", shortId, intent,
                            reply == null ? 0 : reply.length());
                    final var safe = (reply != null && !reply.isBlank()) ? reply
                            : "Je n'ai pas pu générer de réponse. Reformule ta question ou vérifie qu'Ollama est démarré.";
                    sendSse(emitter, "{\"type\":\"done\",\"reply\":" + jsonEscape(safe)
                            + ",\"conversationId\":\"" + convId
                            + "\",\"intent\":\"" + intent.name()
                            + "\",\"timestamp\":\"" + LocalDateTime.now() + "\"}");
                    emitter.complete();
                } catch (Exception e) {
                    heartbeat.shutdownNow();
                    log.error("Stream error [{}]: {}", shortId, e.getMessage(), e);
                    sendSse(emitter, "{\"type\":\"error\",\"reply\":" + jsonEscape(buildUserFacingError(e))
                            + ",\"conversationId\":\"" + convId + "\"}");
                    emitter.complete();
                }
            });
        }

        return emitter;
    }

    // ── Discovery pre-fetch ───────────────────────────────────────────────────

    private String buildDiscoveryPrompt(String userMessage, String basePrompt) {
        try {
            final var result = musicLookupService.lookup(userMessage);
            final var summary = result.toSummary();
            log.info("Pre-fetch lookup for discovery: '{}' → source={}", userMessage, result.source());
            return basePrompt
                    + "\n\n[Résultat de recherche externe :]\n" + summary
                    + "\n\n[Réponds à l'utilisateur en français en te basant uniquement sur ces informations.]";
        } catch (Exception e) {
            log.warn("Pre-fetch lookup failed for '{}': {}", userMessage, e.getMessage());
            return basePrompt;
        }
    }

    // ── Intent resolution ─────────────────────────────────────────────────────

    private IntentType classifyIntent(String message, OperatingMode mode) {
        // Force planning agents for non-PLAN modes
        if (mode == OperatingMode.APPLY) return IntentType.PLANIFICATION;
        if (mode == OperatingMode.MANUAL) return IntentType.PLANIFICATION;
        return intentClassifier.apply(message);
    }

    private static boolean isSimpleIntent(IntentType intent) {
        return intent == IntentType.GENERAL || intent == IntentType.DECOUVERTE;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveConvId(String conversationId) {
        return (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;
    }

    private static String shortId(String convId) {
        return convId.substring(0, Math.min(8, convId.length()));
    }

    private static void sendSse(SseEmitter emitter, String json) {
        try {
            emitter.send(SseEmitter.event().data(json));
        } catch (IOException ignored) {
            // client disconnected
        }
    }

    private static String jsonEscape(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    private String buildUserFacingError(Exception e) {
        final var msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("connect") || msg.contains("connection refused"))
            return "Impossible de joindre Ollama. Vérifie que docker-compose est démarré.";
        if (msg.contains("timeout") || msg.contains("timed out"))
            return "Ollama met trop longtemps à répondre. Essaie un modèle plus léger ou augmente le timeout.";
        if (msg.contains("model") && msg.contains("not found"))
            return "Modèle Ollama introuvable. Lance : docker exec -it dj-tagger-ollama ollama pull llama3.1:8b";
        return "Erreur : " + e.getMessage();
    }

    private String buildContextPrefix(OperatingMode mode, List<String> filePaths, String currentDir) {
        final var sb = new StringBuilder("[mode=").append(mode.name());
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

    public record ChatResult(String reply, String conversationId, List<ToolCall> toolCalls) {
        public ChatResult {
            toolCalls = toolCalls == null ? List.of() : Collections.unmodifiableList(toolCalls);
        }
    }

    public record ToolCall(String id, String name, String argumentsJson) {}
}
