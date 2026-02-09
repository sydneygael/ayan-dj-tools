package com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discogs")
public record DiscogsProperties(
        String token,
        String baseUrl,
        RateLimit rateLimit
) {

    public record RateLimit(
            double requestsPerSecond,
            long cacheTtlMinutes
    ) {}
}
