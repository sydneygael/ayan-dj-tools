package com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp;

import com.djtools.ayan.musictagger.domain.model.HarmonicPlaylist;
import com.djtools.ayan.musictagger.domain.model.Playlist;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class PlaylistTools {

    private final AyanMusicTools tools;

    public PlaylistTools(AyanMusicTools tools) {
        this.tools = tools;
    }

    @Tool("Génère une playlist de loop mixing (technique 3/4) : morceaux dans une plage de BPM, sélectionnés par similarité sémantique et danceability")
    public Playlist generateLoopMixingPlaylist(
            @P("BPM minimum") int bpmMin,
            @P("BPM maximum") int bpmMax,
            @P("Genre principal (optionnel, peut être vide)") String genre) {
        return tools.generateLoopMixingPlaylist(bpmMin, bpmMax, genre);
    }

    @Tool("Génère une playlist mixée harmoniquement via la roue de Camelot : chaque transition reste dans une tonalité compatible, écart de ±6 BPM")
    public HarmonicPlaylist generateHarmonicMixedPlaylist(
            @P("Genre principal (optionnel, peut être vide)") String genre,
            @P("BPM minimum") int minBpm,
            @P("BPM maximum") int maxBpm,
            @P("Énergie cible 0.0–1.0") double targetEnergy,
            @P("Nombre de morceaux souhaité") int count) {
        return tools.generateHarmonicMixedPlaylist(genre, minBpm, maxBpm, targetEnergy, count);
    }
}
