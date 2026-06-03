package com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp;

import com.djtools.ayan.musictagger.domain.model.HarmonicPlaylist;
import com.djtools.ayan.musictagger.domain.model.Playlist;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class PlaylistTools {

    private final AyanMusicTools tools;

    public PlaylistTools(AyanMusicTools tools) {
        this.tools = tools;
    }

    @Tool(description = "Génère une playlist de loop mixing (technique 3/4) : morceaux dans une plage de BPM, sélectionnés par similarité sémantique et danceability")
    public Playlist generateLoopMixingPlaylist(
            @ToolParam(description = "BPM minimum") int bpmMin,
            @ToolParam(description = "BPM maximum") int bpmMax,
            @ToolParam(description = "Genre principal (optionnel, peut être vide)") String genre) {
        return tools.generateLoopMixingPlaylist(bpmMin, bpmMax, genre);
    }

    @Tool(description = "Génère une playlist mixée harmoniquement via la roue de Camelot : chaque transition reste dans une tonalité compatible, écart de ±6 BPM")
    public HarmonicPlaylist generateHarmonicMixedPlaylist(
            @ToolParam(description = "Genre principal (optionnel, peut être vide)") String genre,
            @ToolParam(description = "BPM minimum") int minBpm,
            @ToolParam(description = "BPM maximum") int maxBpm,
            @ToolParam(description = "Énergie cible 0.0–1.0") double targetEnergy,
            @ToolParam(description = "Nombre de morceaux souhaité") int count) {
        return tools.generateHarmonicMixedPlaylist(genre, minBpm, maxBpm, targetEnergy, count);
    }
}
