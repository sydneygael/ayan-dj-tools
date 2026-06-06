package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.OperatingMode;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence.RedisChatMemoryRepository;
import com.djtools.ayan.musictagger.infrastructure.service.AyanAgentService;
import com.djtools.ayan.musictagger.infrastructure.service.ChatMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    private final RedisChatMemoryRepository memoryStore;

    public AgentController(AyanAgentService agentService, RedisChatMemoryRepository memoryStore) {
        this.agentService = agentService;
        this.memoryStore = memoryStore;
    }

    @Operation(
        summary = "Chat avec l'agent Ayan",
        description = "Envoie un message à l'agent IA (LangChain4j + Ollama). Le message est d'abord classifié par intention (FICHIERS, PLANIFICATION, RECHERCHE, PLAYLIST, DECOUVERTE, GENERAL) pour router vers l'assistant spécialisé approprié. Si `conversationId` est omis, une nouvelle conversation est créée. La mémoire est gérée via `MessageWindowChatMemory` Redis (TTL 24h, fenêtre 20 messages)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Réponse de l'agent avec conversationId et liste des tool calls déclenchés")
    })
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        final var conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID().toString();
        final var mode = request.mode() != null ? request.mode() : OperatingMode.PLAN;
        final var result = agentService.chatWithToolCalls(
                conversationId, request.message(), mode, request.filePaths(), request.currentDir());
        final var messageCount = memoryStore.getMessages(result.conversationId()).size();
        final var toolCalls = result.toolCalls().stream()
                .map(tc -> new ToolCallInfo(tc.id(), tc.name(), tc.argumentsJson()))
                .toList();
        return new ChatResponse(result.reply(), result.conversationId(), messageCount, LocalDateTime.now(), toolCalls);
    }

    @Operation(
        summary = "Chat en streaming SSE",
        description = "Même sémantique que POST /chat mais retourne un flux Server-Sent Events avec événements JSON. Types : `thinking` (heartbeat), `chunk` (fragment token — generalAssistant uniquement), `done` (réponse complète + intent), `error`. Note : les assistants avec tools utilisent un virtual thread synchrone — seul generalAssistant génère de vrais chunks token-by-token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Flux text/event-stream — chaque data: contient un fragment de réponse")
    })
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        final var mode = request.mode() != null ? request.mode() : OperatingMode.PLAN;
        return agentService.streamChat(
                request.conversationId(), request.message(), mode,
                request.filePaths(), request.currentDir());
    }

    @Operation(
        summary = "Historique d'une conversation",
        description = "Retourne tous les messages (user + assistant) stockés dans Redis pour cette conversation (clé `chat-memory:{id}`), triés chronologiquement. La conversation expire après 24h d'inactivité (TTL Redis). Fenêtre glissante de 20 messages."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste de messages ordonnés (peut être vide si la conversation a expiré)")
    })
    @GetMapping("/conversations/{id}/history")
    public List<ChatMessage> getHistory(
            @Parameter(description = "Identifiant UUID de la conversation") @PathVariable String id) {
        return memoryStore.getMessages(id).stream()
                .map(msg -> new ChatMessage(role(msg), text(msg), null))
                .toList();
    }

    @Operation(
        summary = "Supprimer une conversation",
        description = "Efface l'historique Redis de la conversation. La prochaine interaction avec ce conversationId repartira d'un contexte vide."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conversation supprimée")
    })
    @DeleteMapping("/conversations/{id}")
    public void clearConversation(
            @Parameter(description = "Identifiant UUID de la conversation") @PathVariable String id) {
        memoryStore.deleteMessages(id);
    }

    private static String role(dev.langchain4j.data.message.ChatMessage msg) {
        return msg.type() == ChatMessageType.USER ? "user" : "assistant";
    }

    private static String text(dev.langchain4j.data.message.ChatMessage msg) {
        return switch (msg) {
            case UserMessage um -> um.singleText();
            case AiMessage am -> am.text() != null ? am.text() : "";
            default -> "";
        };
    }

    @Schema(description = "Corps de la requête de chat")
    public record ChatRequest(
            @Schema(description = "Message de l'utilisateur", example = "Enrichis ce fichier : /music/track.mp3")
            String message,
            @Schema(description = "ID de la conversation existante. Omis = nouvelle conversation.", example = "a3f2c1d0-...")
            String conversationId,
            @Schema(description = "Mode d'opération de l'agent (PLAN par défaut)", example = "PLAN")
            OperatingMode mode,
            @Schema(description = "Chemins absolus des fichiers sélectionnés par l'utilisateur")
            List<String> filePaths,
            @Schema(description = "Répertoire courant ouvert dans le navigateur de fichiers", example = "/home/user/music")
            String currentDir
    ) {}

    @Schema(description = "Réponse de l'agent IA")
    public record ChatResponse(
            @Schema(description = "Texte de la réponse générée par l'agent")
            String reply,
            @Schema(description = "Identifiant UUID de la conversation (nouveau ou existant)")
            String conversationId,
            @Schema(description = "Nombre total de messages dans la conversation (user + assistant)")
            long messageCount,
            @Schema(description = "Horodatage de la réponse (ISO-8601)")
            LocalDateTime timestamp,
            @Schema(description = "Liste des @Tool functions appelées (toujours vide — LangChain4j n'expose pas les tool calls dans la réponse finale)")
            List<ToolCallInfo> toolCalls
    ) {}

    @Schema(description = "Détail d'un appel de @Tool function par l'agent")
    public record ToolCallInfo(
            @Schema(description = "Identifiant unique du tool call") String id,
            @Schema(description = "Nom de la fonction appelée", example = "enrichWithSpotify") String name,
            @Schema(description = "Arguments passés à la fonction (JSON sérialisé)", example = "{\"artist\":\"Daft Punk\",\"title\":\"Around the World\"}")
            String argumentsJson
    ) {}
}
