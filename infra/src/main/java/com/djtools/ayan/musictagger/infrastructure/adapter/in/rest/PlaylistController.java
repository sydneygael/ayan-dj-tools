package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.HarmonicPlaylist;
import com.djtools.ayan.musictagger.domain.model.Playlist;
import com.djtools.ayan.musictagger.infrastructure.service.PlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/playlist")
@Tag(name = "Playlist", description = "Génération de playlists (loop-mixing, harmonique Camelot, thématique) et export M3U")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @Schema(description = "Filtres BPM et genre pour la playlist loop-mixing")
    record GenerateRequest(
            @Schema(description = "BPM minimum (défaut 120)", example = "120") Integer bpmMin,
            @Schema(description = "BPM maximum (défaut 145)", example = "145") Integer bpmMax,
            @Schema(description = "Genre à filtrer, vide = tous genres", example = "techno") String genre
    ) {}

    @Operation(
        summary = "Générer une playlist loop-mixing",
        description = "Sélectionne les tracks de la bibliothèque selon les filtres BPM/genre, puis les ordonne avec la technique 3/4 (transition progressive par paliers de BPM) pour un mix fluide. Utilise le vector store Qdrant pour les suggestions."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Playlist générée avec liste de tracks et métadonnées")
    })
    @PostMapping("/generate")
    public Playlist generate(@RequestBody(required = false) GenerateRequest req) {
        return playlistService.generateLoopMixingPlaylist(
                orDefault(fieldOf(req, GenerateRequest::bpmMin), 120),
                orDefault(fieldOf(req, GenerateRequest::bpmMax), 145),
                orDefault(fieldOf(req, GenerateRequest::genre), "")
        );
    }

    @Schema(description = "Paramètres pour la playlist harmonique Camelot")
    record HarmonicRequest(
            @Schema(description = "BPM minimum (défaut 120)", example = "120") Integer bpmMin,
            @Schema(description = "BPM maximum (défaut 145)", example = "145") Integer bpmMax,
            @Schema(description = "Genre à filtrer, vide = tous genres") String genre,
            @Schema(description = "Niveau d'énergie cible 0.0–1.0 (défaut 0.6)", example = "0.7") Double targetEnergy,
            @Schema(description = "Nombre de tracks souhaité (défaut 25)", example = "20") Integer count
    ) {}

    @Operation(
        summary = "Générer une playlist harmoniquement mixée",
        description = "Ordonne les tracks selon la roue de Camelot (Camelot Wheel) pour garantir des transitions harmoniques compatibles (tonalités adjacentes ou identiques). Filtre par BPM et énergie cible pour un arc énergétique cohérent."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Playlist harmonique avec séquence de tonalités Camelot")
    })
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

    @Schema(description = "Paramètres pour la playlist thématique")
    record ThematicRequest(
            @Schema(description = "Thème textuel libre (mots-clés, ambiance, genre)", example = "dark industrial techno Berlin") String theme,
            @Schema(description = "BPM minimum (défaut 0 = pas de filtre)", example = "130") Integer bpmMin,
            @Schema(description = "BPM maximum (défaut 300 = pas de filtre)", example = "160") Integer bpmMax,
            @Schema(description = "Nombre de tracks souhaité (défaut 12)", example = "12") Integer count
    ) {}

    @Operation(
        summary = "Générer une playlist thématique",
        description = "Utilise la recherche sémantique RAG (Qdrant) pour trouver les tracks dont les thèmes lyriques et l'ambiance correspondent au texte libre fourni, puis les ordonne en arc narratif : intro → montée → peak → descente → outro."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Playlist thématique avec arc narratif en 5 phases")
    })
    @PostMapping("/generate-thematic")
    public Playlist generateThematic(@RequestBody(required = false) ThematicRequest req) {
        return playlistService.generateThematicPlaylist(
                orDefault(fieldOf(req, ThematicRequest::theme), "dance energy groove"),
                orDefault(fieldOf(req, ThematicRequest::bpmMin), 0),
                orDefault(fieldOf(req, ThematicRequest::bpmMax), 300),
                orDefault(fieldOf(req, ThematicRequest::count), 12)
        );
    }

    private static <R, T> T fieldOf(R req, java.util.function.Function<R, T> accessor) {
        return req != null ? accessor.apply(req) : null;
    }

    private static int orDefault(Integer value, int fallback) { return value != null ? value : fallback; }
    private static double orDefault(Double value, double fallback) { return value != null ? value : fallback; }
    private static String orDefault(String value, String fallback) { return value != null ? value : fallback; }
}
