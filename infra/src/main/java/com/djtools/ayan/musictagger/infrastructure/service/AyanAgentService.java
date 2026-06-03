package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.OperatingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

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
    private static final int CHAT_TIMEOUT_SECONDS = 180;

    private final ChatClient chatClient;

    public AyanAgentService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatResult chatWithToolCalls(String conversationId,
                                        String userMessage,
                                        OperatingMode mode,
                                        List<String> filePaths,
                                        String currentDir) {
        final var convId = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;

        final var shortId = convId.substring(0, Math.min(8, convId.length()));
        final var prompt = buildContextPrefix(mode, filePaths, currentDir) + userMessage;
        log.info("Chat [{}] mode={} — {}", shortId, mode, userMessage.substring(0, Math.min(60, userMessage.length())));

        // Virtual thread + timeout — évite que le thread Tomcat soit bloqué si Ollama
        // ou un tool call externe (Soundcharts, DuckDuckGo, Spotify) ne répond pas
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var future = executor.submit(() ->
                    chatClient.prompt()
                            .user(prompt)
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId))
                            .call()
                            .content());
            try {
                final var content = future.get(CHAT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                final var reply = (content != null && !content.isBlank())
                        ? content
                        : "Je n'ai pas pu générer de réponse. Reformule ta question ou vérifie qu'Ollama est démarré.";
                log.info("Reply [{}] — {} chars", shortId, reply.length());
                return new ChatResult(reply, convId, List.of());
            } catch (TimeoutException e) {
                future.cancel(true);
                log.warn("Chat timeout [{}] après {}s", shortId, CHAT_TIMEOUT_SECONDS);
                return new ChatResult(
                        "Le modèle a dépassé le délai de %ds. Réessaie ou reformule ta question.".formatted(CHAT_TIMEOUT_SECONDS),
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

    private String buildUserFacingError(Exception e) {
        final var msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("connect") || msg.contains("connection refused") || msg.contains("refused"))
            return "Impossible de joindre Ollama. Vérifie que docker-compose est démarré (docker-compose up -d).";
        if (msg.contains("timeout") || msg.contains("timed out"))
            return "Ollama met trop longtemps à répondre. Essaie un modèle plus léger ou augmente le timeout.";
        if (msg.contains("model") && msg.contains("not found"))
            return "Modèle Ollama introuvable. Lance : docker exec -it dj-tagger-ollama ollama pull mistral";
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
