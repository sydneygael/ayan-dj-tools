package com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp;

import com.djtools.ayan.musictagger.domain.model.SimilarTrackResult;
import com.djtools.ayan.musictagger.domain.model.SongSearchCriteria;
import com.djtools.ayan.musictagger.domain.model.SongSearchResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchTools {

    private final AyanMusicTools tools;

    public SearchTools(AyanMusicTools tools) {
        this.tools = tools;
    }

    @Tool(description = "Recherche des morceaux similaires dans la collection vectorisée (RAG)")
    public List<SimilarTrackResult> findSimilarTracks(
            @ToolParam(description = "Requête de recherche (artiste, genre, ambiance, etc.)") String query,
            @ToolParam(description = "Nombre max de résultats") int limit) {
        return tools.findSimilarTracks(query, limit);
    }

    @Tool(description = "Recherche des morceaux dans la collection par critères : genre, BPM, énergie, années, ambiance. Tous les critères sont optionnels.")
    public SongSearchResult searchSongs(
            @ToolParam(required = false, description = "Genre musical (ex: house, techno, afrobeats)") String genre,
            @ToolParam(required = false, description = "Ambiance en texte libre (ex: dark, festif, mélancolique)") String mood,
            @ToolParam(required = false, description = "BPM minimum") Integer bpmMin,
            @ToolParam(required = false, description = "BPM maximum") Integer bpmMax,
            @ToolParam(required = false, description = "Énergie minimum, 0.0–1.0") Double energyMin,
            @ToolParam(required = false, description = "Énergie maximum, 0.0–1.0") Double energyMax,
            @ToolParam(required = false, description = "Année de sortie minimum") Integer yearMin,
            @ToolParam(required = false, description = "Année de sortie maximum") Integer yearMax,
            @ToolParam(required = false, description = "Nombre de morceaux souhaité (défaut 10, max 50)") Integer limit) {
        return tools.searchSongs(genre, mood, bpmMin, bpmMax, energyMin, energyMax, yearMin, yearMax, limit);
    }
}
