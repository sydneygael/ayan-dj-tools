package com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp;

import com.djtools.ayan.musictagger.domain.model.SimilarTrackResult;
import com.djtools.ayan.musictagger.domain.model.SmartTagSuggestion;
import com.djtools.ayan.musictagger.domain.model.SongSearchResult;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/** Tools for RECHERCHE intent — max 4 */
@Component
public class SearchTools {

    private final AyanMusicTools tools;

    public SearchTools(AyanMusicTools tools) {
        this.tools = tools;
    }

    @Tool("Cherche des morceaux similaires dans la collection vectorisée (RAG) à partir d'une description textuelle")
    public List<SimilarTrackResult> findSimilarTracks(
            @P("Requête libre : artiste, genre, ambiance, tonalité…") String query,
            @P("Nombre max de résultats") int limit) {
        return tools.findSimilarTracks(query, limit);
    }

    @Tool("Cherche des morceaux dans la collection par critères combinés : genre, BPM, énergie, années, ambiance. Tous optionnels.")
    public SongSearchResult searchSongs(
            @P("Genre musical — optionnel") String genre,
            @P("Ambiance en texte libre — optionnel") String mood,
            @P("BPM minimum — optionnel") Integer bpmMin,
            @P("BPM maximum — optionnel") Integer bpmMax,
            @P("Énergie minimum 0.0–1.0 — optionnel") Double energyMin,
            @P("Énergie maximum 0.0–1.0 — optionnel") Double energyMax,
            @P("Année de sortie minimum — optionnel") Integer yearMin,
            @P("Année de sortie maximum — optionnel") Integer yearMax,
            @P("Nombre de résultats souhaité, défaut 10 — optionnel") Integer limit) {
        return tools.searchSongs(genre, mood, bpmMin, bpmMax, energyMin, energyMax, yearMin, yearMax, limit);
    }

    @Tool("Suggestions intelligentes de tags pour un fichier : combine enrichissement Soundcharts et morceaux similaires (RAG)")
    public SmartTagSuggestion smartSuggestTags(@P("Chemin absolu du fichier audio") String filepath) {
        return tools.smartSuggestTags(filepath);
    }
}
