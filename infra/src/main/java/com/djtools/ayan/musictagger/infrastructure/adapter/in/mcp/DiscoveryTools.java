package com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class DiscoveryTools {

    private final AyanMusicTools tools;

    public DiscoveryTools(AyanMusicTools tools) {
        this.tools = tools;
    }

    @Tool("Recherche des informations musicales sur un artiste, un album ou un morceau "
            + "via Internet → Soundcharts → Spotify. "
            + "Retourne un résumé textuel complet — ne pas appeler d'autres outils après. "
            + "À utiliser pour les questions de découverte externe (« qui est X ? », « infos sur Y »).")
    public String lookupMusicInfo(
            @P("Requête libre : nom d'artiste, titre, album ou combinaison") String query,
            @P("Nom de l'artiste si connu séparément — optionnel") String artist,
            @P("Titre du morceau si connu séparément — optionnel") String song,
            @P("Nom de l'album si connu séparément — optionnel") String album) {
        return tools.lookupMusicInfo(query, artist, song, album);
    }
}
