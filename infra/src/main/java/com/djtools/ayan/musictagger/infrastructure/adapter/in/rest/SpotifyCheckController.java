package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyApiClient;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vérifie la connectivité et les permissions de l'API Spotify.
 * GET /api/spotify/check → rapport de santé des endpoints disponibles.
 */
@RestController
@RequestMapping("/api/spotify")
@Tag(name = "Spotify", description = "Diagnostic de connectivité et quota de l'API Spotify")
class SpotifyCheckController {

    private static final Logger log = LoggerFactory.getLogger(SpotifyCheckController.class);

    // Track test : "Around the World" de Daft Punk — très connu, toujours disponible
    private static final String TEST_TRACK_ID    = "2bJvI42r8EF3wxjOuDav4r";
    private static final String TEST_ARTIST_ID   = "4tZwfgrHOc3mvqYlEYSvVi";
    private static final String TEST_QUERY       = "artist:Daft Punk track:Around the World";

    private final SpotifyTokenService tokenService;
    private final SpotifyApiClient    apiClient;

    SpotifyCheckController(SpotifyTokenService tokenService, SpotifyApiClient apiClient) {
        this.tokenService = tokenService;
        this.apiClient    = apiClient;
    }

    @Schema(description = "Résultat de test d'un endpoint Spotify")
    record EndpointStatus(
            @Schema(description = "Endpoint testé", example = "GET /search") String endpoint,
            @Schema(description = "true si l'appel a réussi (HTTP 2xx)") boolean ok,
            @Schema(description = "Détail du résultat ou message d'erreur") String detail
    ) {}

    @Schema(description = "Rapport de santé de l'intégration Spotify")
    record SpotifyCheckReport(
            @Schema(description = "true si un token OAuth2 a pu être obtenu") boolean tokenOk,
            @Schema(description = "Détail de l'obtention du token") String tokenDetail,
            @Schema(description = "Statut de l'endpoint GET /search") EndpointStatus search,
            @Schema(description = "Statut de GET /audio-features/{id} (peut être 403 sans Extended Quota Mode depuis nov. 2024)")
            EndpointStatus audioFeatures,
            @Schema(description = "Statut de l'endpoint GET /artists/{id}") EndpointStatus artist,
            @Schema(description = "Résumé lisible de la santé globale de l'intégration") String summary
    ) {}

    @Operation(
        summary = "Diagnostic de connectivité Spotify",
        description = "Teste séquentiellement : (1) obtention du token OAuth2 client-credentials, (2) endpoint `/search`, (3) `/audio-features/{id}`, (4) `/artists/{id}`. L'endpoint `audio-features` peut retourner 403 pour les apps sans Extended Quota Mode (comportement normal depuis novembre 2024 — BPM/clé non disponibles via Spotify dans ce cas)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rapport de santé complet — tokenOk + statut de chaque endpoint")
    })
    @GetMapping("/check")
    SpotifyCheckReport check() {
        log.info("Running Spotify API connectivity check…");

        // ── Token ────────────────────────────────────────────────────────────
        boolean tokenOk;
        String  tokenDetail;
        try {
            final var token = tokenService.getAccessToken();
            tokenOk    = token != null && !token.isBlank();
            tokenDetail = tokenOk ? "OK (token obtenu)" : "VIDE — vérifier client_id / client_secret";
            log.info("Spotify token check: {}", tokenDetail);
        } catch (Exception e) {
            tokenOk    = false;
            tokenDetail = "ERREUR : " + e.getMessage();
            log.error("Spotify token check FAILED: {}", e.getMessage());
        }

        // ── Search ───────────────────────────────────────────────────────────
        EndpointStatus search = checkEndpoint("GET /search", () -> {
            var resp = apiClient.searchTracks(TEST_QUERY, "track", 1);
            int count = resp.tracks() != null && resp.tracks().items() != null
                    ? resp.tracks().items().size() : 0;
            return count + " résultat(s)";
        });

        // ── Audio Features ───────────────────────────────────────────────────
        EndpointStatus audioFeatures = checkEndpoint("GET /audio-features/{id}", () -> {
            var af = apiClient.getAudioFeatures(TEST_TRACK_ID);
            return af != null
                    ? "OK (tempo=" + af.tempo() + ", key=" + af.key() + ")"
                    : "Réponse null";
        });

        // ── Artist ───────────────────────────────────────────────────────────
        EndpointStatus artist = checkEndpoint("GET /artists/{id}", () -> {
            var a = apiClient.getArtist(TEST_ARTIST_ID);
            return "OK (name=" + a.name() + ", genres=" + a.genres() + ")";
        });

        // ── Summary ──────────────────────────────────────────────────────────
        String summary = buildSummary(tokenOk, search, audioFeatures, artist);
        log.info("Spotify check summary: {}", summary);

        return new SpotifyCheckReport(tokenOk, tokenDetail, search, audioFeatures, artist, summary);
    }

    private EndpointStatus checkEndpoint(String name, CheckFn fn) {
        try {
            String detail = fn.run();
            log.info("Spotify {} : {}", name, detail);
            return new EndpointStatus(name, true, detail);
        } catch (Exception e) {
            String detail = e.getClass().getSimpleName() + " : " + e.getMessage();
            log.warn("Spotify {} FAILED : {}", name, detail);
            return new EndpointStatus(name, false, detail);
        }
    }

    private static String buildSummary(boolean tokenOk, EndpointStatus... statuses) {
        if (!tokenOk) return "CRITIQUE — authentification échouée, vérifier SPOTIFY_CLIENT_ID / SPOTIFY_CLIENT_SECRET";
        long failed = java.util.Arrays.stream(statuses).filter(s -> !s.ok()).count();
        if (failed == 0) return "Tout fonctionne.";
        // Audio features 403 est attendu pour les apps sans Extended Quota Mode
        boolean onlyAudioFeatures = java.util.Arrays.stream(statuses)
                .filter(s -> !s.ok())
                .allMatch(s -> s.endpoint().contains("audio-features"));
        if (onlyAudioFeatures) return
                "Search + Artist OK. Audio Features indisponible (403) — "
                + "endpoint restreint depuis nov. 2024 pour les apps sans Extended Quota Mode "
                + "(https://developer.spotify.com/documentation/web-api/concepts/quota-modes). "
                + "BPM / tonalité ne seront pas enrichis par Spotify.";
        return failed + " endpoint(s) en erreur — voir détails.";
    }

    @FunctionalInterface
    private interface CheckFn { String run() throws Exception; }
}
