package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp.DiscoveryTools;
import com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp.FileOpsTools;
import com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp.PlaylistTools;
import com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp.PlanTools;
import com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp.SearchTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class AgentDispatcher {

    private static final String FILE_OPS_PROMPT = """
            Tu es un agent spécialisé dans les opérations sur fichiers audio pour DJs.
            Scanner, détecter les tags manquants, enrichir via Spotify, prévisualiser, suggérer des tags.
            Réponds en français sans markdown. Utilise – pour les listes, ─ pour séparer les sections.
            Format par fichier : Artiste : X  |  Titre : Y  |  BPM : Z
            Symboles : ✓ succès  ✗ manquant  ⚠ avertissement
            """;

    private static final String PLAN_PROMPT = """
            Tu es un agent spécialisé dans la gestion des plans de modification de tags pour DJs.
            Créer des plans, appliquer des tags, mode manuel (fichier par fichier), historique des modifications.
            Réponds en français sans markdown.
            """;

    private static final String SEARCH_PROMPT = """
            Tu es un agent spécialisé dans la recherche musicale par critères dans la collection locale.
            Chercher des morceaux similaires (RAG) ou filtrer par genre, BPM, énergie, ambiance, années.
            Réponds en français sans markdown. Par résultat : Artiste | Titre | BPM | Tonalité
            """;

    private static final String PLAYLIST_PROMPT = """
            Tu es un agent spécialisé dans la génération de playlists pour DJs.
            Playlists loop mixing (plage de BPM) ou harmoniques via la roue de Camelot.
            Réponds en français sans markdown. Numérote chaque morceau avec BPM et tonalité.
            """;

    private static final String DISCOVERY_PROMPT = """
            Tu es un agent spécialisé dans la découverte musicale via sources externes.
            Informations sur artistes, albums, morceaux via Soundcharts, Internet et Spotify.
            Réponds en français sans markdown.
            """;

    private final ChatClient fileOpsClient;
    private final ChatClient planClient;
    private final ChatClient searchClient;
    private final ChatClient playlistClient;
    private final ChatClient discoveryClient;

    public AgentDispatcher(ChatModel chatModel,
                           FileOpsTools fileOpsTools,
                           PlanTools planTools,
                           SearchTools searchTools,
                           PlaylistTools playlistTools,
                           DiscoveryTools discoveryTools) {
        this.fileOpsClient = ChatClient.builder(chatModel)
                .defaultSystem(FILE_OPS_PROMPT)
                .defaultTools(fileOpsTools)
                .build();
        this.planClient = ChatClient.builder(chatModel)
                .defaultSystem(PLAN_PROMPT)
                .defaultTools(planTools)
                .build();
        this.searchClient = ChatClient.builder(chatModel)
                .defaultSystem(SEARCH_PROMPT)
                .defaultTools(searchTools)
                .build();
        this.playlistClient = ChatClient.builder(chatModel)
                .defaultSystem(PLAYLIST_PROMPT)
                .defaultTools(playlistTools)
                .build();
        this.discoveryClient = ChatClient.builder(chatModel)
                .defaultSystem(DISCOVERY_PROMPT)
                .defaultTools(discoveryTools)
                .build();
    }

    @Tool(description = "Agent pour les opérations sur fichiers audio : scanner, analyser, enrichir via Spotify, prévisualiser et suggérer des tags")
    public String fileOpsAgent(
            @ToolParam(description = "Demande détaillée incluant les chemins de fichiers à traiter") String request) {
        return call(fileOpsClient, request);
    }

    @Tool(description = "Agent pour la gestion des plans de tags : créer un plan, appliquer, mode manuel (fichier par fichier), historique")
    public String planAgent(
            @ToolParam(description = "Demande incluant les fichiers cibles et le mode (PLAN/MANUAL/APPLY)") String request) {
        return call(planClient, request);
    }

    @Tool(description = "Agent pour la recherche musicale dans la collection : morceaux similaires ou filtrage par critères (genre, BPM, énergie, humeur)")
    public String searchAgent(
            @ToolParam(description = "Critères de recherche : genre, BPM min/max, énergie, humeur, années") String request) {
        return call(searchClient, request);
    }

    @Tool(description = "Agent pour la génération de playlists DJ : loop mixing par plage de BPM ou mix harmonique via roue de Camelot")
    public String playlistAgent(
            @ToolParam(description = "Paramètres : genre, BPM min/max, énergie cible, nombre de morceaux") String request) {
        return call(playlistClient, request);
    }

    @Tool(description = "Agent pour la découverte musicale externe : informations sur un artiste, un album ou un morceau via Soundcharts et Spotify")
    public String discoveryAgent(
            @ToolParam(description = "Nom de l'artiste, du titre ou de l'album à rechercher") String request) {
        return call(discoveryClient, request);
    }

    private String call(ChatClient client, String request) {
        var result = client.prompt().user(request).call().content();
        return result != null ? result : "L'agent n'a pas pu traiter la demande.";
    }
}
