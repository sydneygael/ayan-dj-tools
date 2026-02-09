package com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "musicbrainz")
public record MusicBrainzProperties(
        String baseUrl,
        String userAgent,
        RateLimit rateLimit
) {

    public record RateLimit(
            double requestsPerSecond,
            long cacheTtlMinutes
    ) {}
}
