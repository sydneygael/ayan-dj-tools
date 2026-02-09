package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify;

import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Optional;

public class SpotifyCacheService {

    private final Cache<String, EnrichmentResult> cache;

    public SpotifyCacheService(long ttlMinutes) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
                .build();
    }

    public Optional<EnrichmentResult> get(String artist, String title) {
        return Optional.ofNullable(cache.getIfPresent(cacheKey(artist, title)));
    }

    public void put(String artist, String title, EnrichmentResult result) {
        cache.put(cacheKey(artist, title), result);
    }

    private static String cacheKey(String artist, String title) {
        return (artist + ":" + title).toLowerCase();
    }
}
