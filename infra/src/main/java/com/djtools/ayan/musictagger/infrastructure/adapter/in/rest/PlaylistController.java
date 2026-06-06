package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.HarmonicPlaylist;
import com.djtools.ayan.musictagger.domain.model.Playlist;
import com.djtools.ayan.musictagger.infrastructure.service.PlaylistService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/playlist")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    record GenerateRequest(Integer bpmMin, Integer bpmMax, String genre) {}

    /** Génère une playlist loop-mixing (technique 3/4) filtrée par BPM et genre. */
    @PostMapping("/generate")
    public Playlist generate(@RequestBody(required = false) GenerateRequest req) {
        return playlistService.generateLoopMixingPlaylist(
                orDefault(fieldOf(req, GenerateRequest::bpmMin), 120),
                orDefault(fieldOf(req, GenerateRequest::bpmMax), 145),
                orDefault(fieldOf(req, GenerateRequest::genre), "")
        );
    }

    record HarmonicRequest(Integer bpmMin, Integer bpmMax, String genre, Double targetEnergy, Integer count) {}

    /** Génère une playlist mixée harmoniquement via la roue de Camelot. */
    @PostMapping("/generate-harmonic")
    public HarmonicPlaylist generateHarmonic(@RequestBody(required = false) HarmonicRequest req) {
        return playlistService.generateHarmonicPlaylist(
                orDefault(fieldOf(req, HarmonicRequest::bpmMin), 120),
                orDefault(fieldOf(req, HarmonicRequest::bpmMax), 145),
                orDefault(fieldOf(req, HarmonicRequest::genre), ""),
                orDefault(fieldOf(req, HarmonicRequest::targetEnergy), 0.6),
                orDefault(fieldOf(req, HarmonicRequest::count), 25)
        );
    }

    record ThematicRequest(String theme, Integer bpmMin, Integer bpmMax, Integer count) {}

    /** Génère une playlist en arc narratif (intro → montée → peak → outro) basée sur un thème. */
    @PostMapping("/generate-thematic")
    public Playlist generateThematic(@RequestBody(required = false) ThematicRequest req) {
        return playlistService.generateThematicPlaylist(
                orDefault(fieldOf(req, ThematicRequest::theme), "dance energy groove"),
                orDefault(fieldOf(req, ThematicRequest::bpmMin), 0),
                orDefault(fieldOf(req, ThematicRequest::bpmMax), 300),
                orDefault(fieldOf(req, ThematicRequest::count), 12)
        );
    }

    /** Extrait un champ d'une requête nullable via un accessor. */
    private static <R, T> T fieldOf(R req, java.util.function.Function<R, T> accessor) {
        return req != null ? accessor.apply(req) : null;
    }

    private static int orDefault(Integer value, int fallback) { return value != null ? value : fallback; }
    private static double orDefault(Double value, double fallback) { return value != null ? value : fallback; }
    private static String orDefault(String value, String fallback) { return value != null ? value : fallback; }
}
