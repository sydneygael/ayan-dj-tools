package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.OperatingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
public class AyanAgentService {

    private static final Logger log = LoggerFactory.getLogger(AyanAgentService.class);

    private final ChatClient chatClient;
    private final int chatTimeoutSeconds;

    public AyanAgentService(ChatClient chatClient,
                            @Value("${dj-tagger.chat.timeout-seconds:120}") int chatTimeoutSeconds) {
        this.chatClient = chatClient;
        this.chatTimeoutSeconds = chatTimeoutSeconds;
    }

    public ChatResult chatWithToolCalls(String conversationId,
                                        String userMessage,
                                        OperatingMode mode,
                                        List<String> filePaths,
                                        String currentDir) {
        final var convId = resolveConvId(conversationId);
        final var shortId = convId.substring(0, Math.min(8, convId.length()));
        final var prompt = buildContextPrefix(mode, filePaths, currentDir) + userMessage;
        log.info("Chat [{}] mode={} - {}", shortId, mode, userMessage.substring(0, Math.min(60, userMessage.length())));

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var future = executor.submit(() ->
                    chatClient.prompt()
                            .user(prompt)
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId))
                            .call()
                            .content());

            try {
                final var content = future.get(chatTimeoutSeconds, TimeUnit.SECONDS);
                final var reply = (content != null && !content.isBlank())
                        ? content
                        : "Je n'ai pas pu generer de reponse. Reformule ta question ou verifie qu'Ollama est demarre.";
                log.info("Reply [{}] - {} chars", shortId, reply.length());
                return new ChatResult(reply, convId, List.of());
            } catch (TimeoutException e) {
                future.cancel(true);
                log.warn("Chat timeout [{}] apres {}s", shortId, chatTimeoutSeconds);
                return new ChatResult(
                        "Le modele a depasse le delai de %ds. Reessaie ou reformule ta question."
                                .formatted(chatTimeoutSeconds),
                        convId,
                        List.of()
                );
            } catch (ExecutionException e) {
                final var cause = e.getCause() instanceof Exception ex ? ex : e;
                log.error("Chat error [{}]: {}", shortId, cause.getMessage(), cause);
                return new ChatResult(buildUserFacingError(cause), convId, List.of());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ChatResult("Requete interrompue.", convId, List.of());
        }
    }

    public SseEmitter streamChat(String conversationId,
                                 String userMessage,
                                 OperatingMode mode,
                                 List<String> filePaths,
                                 String currentDir) {
        final var emitter = new SseEmitter(300_000L);
        final var convId = resolveConvId(conversationId);
        final var prompt = buildContextPrefix(mode, filePaths, currentDir) + userMessage;
        final var shortId = convId.substring(0, Math.min(8, convId.length()));
        log.info("Stream [{}] mode={} - {}", shortId, mode, userMessage.substring(0, Math.min(60, userMessage.length())));

        final var accumulated = new StringBuilder();

        chatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId))
                .stream()
                .content()
                .timeout(Duration.ofSeconds(chatTimeoutSeconds))
                .subscribe(
                        token -> {
                            accumulated.append(token);
                            sendSse(emitter, "{\"type\":\"chunk\",\"token\":" + jsonEscape(token)
                                    + ",\"conversationId\":\"" + convId + "\"}");
                        },
                        error -> {
                            log.error("Stream error [{}]: {}", shortId, error.getMessage());
                            final var msg = buildUserFacingError(error instanceof Exception ex ? ex : new RuntimeException(error));
                            sendSse(emitter, "{\"type\":\"error\",\"reply\":" + jsonEscape(msg)
                                    + ",\"conversationId\":\"" + convId + "\"}");
                            emitter.completeWithError(error);
                        },
                        () -> {
                            log.info("Stream done [{}] - {} chars", shortId, accumulated.length());
                            sendSse(emitter, "{\"type\":\"done\",\"reply\":" + jsonEscape(accumulated.toString())
                                    + ",\"conversationId\":\"" + convId + "\",\"timestamp\":\"" + LocalDateTime.now() + "\"}");
                            emitter.complete();
                        }
                );

        return emitter;
    }

    private String resolveConvId(String conversationId) {
        return (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;
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
        if (msg.contains("connect") || msg.contains("connection refused") || msg.contains("refused")) {
            return "Impossible de joindre Ollama. Verifie que docker-compose est demarre (docker-compose up -d).";
        }
        if (msg.contains("timeout") || msg.contains("timed out")) {
            return "Ollama met trop longtemps a repondre. Essaie un modele plus leger ou augmente le timeout.";
        }
        if (msg.contains("model") && msg.contains("not found")) {
            return "Modele Ollama introuvable. Lance : docker exec -it dj-tagger-ollama ollama pull mistral";
        }
        return "Erreur : " + e.getMessage();
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

    public record ChatResult(String reply, String conversationId, List<ToolCall> toolCalls) {
        public ChatResult {
            toolCalls = toolCalls == null ? List.of() : Collections.unmodifiableList(toolCalls);
        }
    }

    public record ToolCall(String id, String name, String argumentsJson) {}
}
