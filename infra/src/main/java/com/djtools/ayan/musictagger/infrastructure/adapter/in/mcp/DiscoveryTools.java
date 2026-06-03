package com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp;

import com.djtools.ayan.musictagger.infrastructure.service.MusicLookupResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class DiscoveryTools {

    private final AyanMusicTools tools;

    public DiscoveryTools(AyanMusicTools tools) {
        this.tools = tools;
    }

    @Tool(description = "Recherche des informations musicales sur un artiste, un album ou un morceau "
            + "en interrogeant des sources externes dans l'ordre : Soundcharts → Internet → Spotify. "
            + "Si rien n'est trouvé, répond simplement qu'aucun résultat n'a été trouvé.")
    public MusicLookupResult lookupMusicInfo(
            @ToolParam(description = "Requête libre : nom d'artiste, titre, album ou combinaison") String query,
            @ToolParam(required = false, description = "Nom de l'artiste (si connu séparément)") String artist,
            @ToolParam(required = false, description = "Titre du morceau (si connu séparément)") String song,
            @ToolParam(required = false, description = "Nom de l'album (si connu séparément)") String album) {
        return tools.lookupMusicInfo(query, artist, song, album);
    }
}
