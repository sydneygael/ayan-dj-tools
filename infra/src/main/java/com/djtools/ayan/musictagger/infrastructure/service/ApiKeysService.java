package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence.AppSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApiKeysService {

    public static final String SOUNDCHARTS_APP_ID    = "soundcharts.app-id";
    public static final String SOUNDCHARTS_API_KEY   = "soundcharts.api-key";
    public static final String SPOTIFY_CLIENT_ID     = "spotify.client-id";
    public static final String SPOTIFY_CLIENT_SECRET = "spotify.client-secret";
    public static final String TAVILY_API_KEY        = "tavily.api-key";

    private final AppSettingsRepository repo;

    @Value("${soundcharts.app-id:}")    private String envSoundchartsAppId;
    @Value("${soundcharts.api-key:}")   private String envSoundchartsApiKey;
    @Value("${spotify.client-id:}")     private String envSpotifyClientId;
    @Value("${spotify.client-secret:}") private String envSpotifyClientSecret;
    @Value("${tavily.api-key:}")        private String envTavilyApiKey;

    public ApiKeysService(AppSettingsRepository repo) {
        this.repo = repo;
    }

    public String getSoundchartsAppId()    { return resolve(SOUNDCHARTS_APP_ID,    envSoundchartsAppId); }
    public String getSoundchartsApiKey()   { return resolve(SOUNDCHARTS_API_KEY,   envSoundchartsApiKey); }
    public String getSpotifyClientId()     { return resolve(SPOTIFY_CLIENT_ID,     envSpotifyClientId); }
    public String getSpotifyClientSecret() { return resolve(SPOTIFY_CLIENT_SECRET, envSpotifyClientSecret); }
    public String getTavilyApiKey()        { return resolve(TAVILY_API_KEY,        envTavilyApiKey); }

    public void save(String key, String value) {
        if (value != null && !value.isBlank()) {
            repo.put(key, value.strip());
        }
    }

    private String resolve(String key, String envFallback) {
        return repo.get(key)
                .filter(v -> !v.isBlank())
                .orElse(envFallback != null ? envFallback : "");
    }
}
