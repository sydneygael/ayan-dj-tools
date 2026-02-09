package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spotify")
public record SpotifyProperties(
        String clientId,
        String clientSecret,
        String authUrl,
        RateLimit rateLimit
) {

    public record RateLimit(
            double requestsPerSecond,
            long cacheTtlMinutes
    ) {}
}
