package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyTokenService;
import com.djtools.ayan.musictagger.infrastructure.service.ApiKeysService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.djtools.ayan.musictagger.infrastructure.service.ApiKeysService.*;

@RestController
@RequestMapping("/api/settings/keys")
public class ApiKeysController {

    private final ApiKeysService apiKeysService;
    private final SpotifyTokenService spotifyTokenService;

    public ApiKeysController(ApiKeysService apiKeysService, SpotifyTokenService spotifyTokenService) {
        this.apiKeysService = apiKeysService;
        this.spotifyTokenService = spotifyTokenService;
    }

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

    public record ApiKeyStatus(boolean configured, String masked) {}

    public record ApiKeysView(
            ApiKeyStatus soundchartsAppId,
            ApiKeyStatus soundchartsApiKey,
            ApiKeyStatus spotifyClientId,
            ApiKeyStatus spotifyClientSecret,
            ApiKeyStatus tavilyApiKey
    ) {}

    public record ApiKeysSaveRequest(
            String soundchartsAppId,
            String soundchartsApiKey,
            String spotifyClientId,
            String spotifyClientSecret,
            String tavilyApiKey
    ) {}
}
