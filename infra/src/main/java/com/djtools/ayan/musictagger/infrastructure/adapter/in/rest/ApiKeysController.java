package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyTokenService;
import com.djtools.ayan.musictagger.infrastructure.service.ApiKeysService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.djtools.ayan.musictagger.infrastructure.service.ApiKeysService.*;

@RestController
@RequestMapping("/api/settings/keys")
@Tag(name = "Settings", description = "Configuration des clés API tierces (Soundcharts, Spotify, Tavily)")
public class ApiKeysController {

    private final ApiKeysService apiKeysService;
    private final SpotifyTokenService spotifyTokenService;

    public ApiKeysController(ApiKeysService apiKeysService, SpotifyTokenService spotifyTokenService) {
        this.apiKeysService = apiKeysService;
        this.spotifyTokenService = spotifyTokenService;
    }

    @Operation(
        summary = "Lire le statut des clés API",
        description = "Retourne pour chaque clé : si elle est configurée (`configured`) et les 4 premiers caractères masqués (`masked`). La valeur en clair n'est jamais retournée. Les clés sont stockées dans Redis (clé-valeur, pas de TTL) ou via variables d'environnement."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statut masqué de chaque clé API")
    })
    @GetMapping
    public ApiKeysView getKeys() {
        return new ApiKeysView(
                status(apiKeysService.getSoundchartsAppId()),
                status(apiKeysService.getSoundchartsApiKey()),
                status(apiKeysService.getSpotifyClientId()),
                status(apiKeysService.getSpotifyClientSecret()),
                status(apiKeysService.getTavilyApiKey())
        );
    }

    @Operation(
        summary = "Mettre à jour les clés API",
        description = "Persiste les clés fournies dans Redis. Les champs null ou vides sont ignorés (clés existantes conservées). Si les credentials Spotify sont modifiés, le token OAuth2 en cache est invalidé immédiatement."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Clés sauvegardées")
    })
    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveKeys(@RequestBody ApiKeysSaveRequest req) {
        saveIfPresent(SOUNDCHARTS_APP_ID,  req.soundchartsAppId());
        saveIfPresent(SOUNDCHARTS_API_KEY, req.soundchartsApiKey());
        saveIfPresent(TAVILY_API_KEY,      req.tavilyApiKey());
        boolean spotifyChanged = false;
        if (req.spotifyClientId() != null && !req.spotifyClientId().isBlank()) {
            apiKeysService.save(SPOTIFY_CLIENT_ID, req.spotifyClientId());
            spotifyChanged = true;
        }
        if (req.spotifyClientSecret() != null && !req.spotifyClientSecret().isBlank()) {
            apiKeysService.save(SPOTIFY_CLIENT_SECRET, req.spotifyClientSecret());
            spotifyChanged = true;
        }
        if (spotifyChanged) {
            spotifyTokenService.invalidateToken();
        }
    }

    private void saveIfPresent(String key, String value) {
        apiKeysService.save(key, value);
    }

    private static ApiKeyStatus status(String value) {
        if (value == null || value.isBlank()) return new ApiKeyStatus(false, null);
        final var masked = value.length() <= 4 ? "****" : value.substring(0, 4) + "****";
        return new ApiKeyStatus(true, masked);
    }

    @Schema(description = "Statut masqué d'une clé API")
    public record ApiKeyStatus(
            @Schema(description = "true si la clé est configurée (non vide)") boolean configured,
            @Schema(description = "4 premiers caractères suivis de '****', null si non configurée", example = "SA34****", nullable = true)
            String masked
    ) {}

    @Schema(description = "Statut de toutes les clés API configurées")
    public record ApiKeysView(
            @Schema(description = "Soundcharts App ID") ApiKeyStatus soundchartsAppId,
            @Schema(description = "Soundcharts API Key") ApiKeyStatus soundchartsApiKey,
            @Schema(description = "Spotify Client ID") ApiKeyStatus spotifyClientId,
            @Schema(description = "Spotify Client Secret") ApiKeyStatus spotifyClientSecret,
            @Schema(description = "Tavily API Key (recherche web pour l'agent)") ApiKeyStatus tavilyApiKey
    ) {}

    @Schema(description = "Corps de la requête de mise à jour des clés API (champs null = ignorés)")
    public record ApiKeysSaveRequest(
            @Schema(description = "Soundcharts App ID (ex: SA34-API_XXXXXXXX)", nullable = true) String soundchartsAppId,
            @Schema(description = "Soundcharts API Key (hex 16 chars)", nullable = true) String soundchartsApiKey,
            @Schema(description = "Spotify Client ID (32 chars hex)", nullable = true) String spotifyClientId,
            @Schema(description = "Spotify Client Secret (32 chars hex)", nullable = true) String spotifyClientSecret,
            @Schema(description = "Tavily API Key (tvly-...)", nullable = true) String tavilyApiKey
    ) {}
}
