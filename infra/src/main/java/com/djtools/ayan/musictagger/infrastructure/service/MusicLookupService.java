package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.SoundchartsMusicMetadataAdapter;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyApiClient;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyMapper;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyRateLimiter;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.web.DuckDuckGoSearchAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * External music lookup chain:
 * 1) Soundcharts
 * 2) DuckDuckGo + Spotify in parallel if Soundcharts is empty.
 */
@Service
public class MusicLookupService {

    private static final Logger log = LoggerFactory.getLogger(MusicLookupService.class);
    private static final int SEARCH_LIMIT = 8;
    private static final long SOUNDCHARTS_TIMEOUT_SECONDS = 8;
    private static final long WEB_TIMEOUT_SECONDS = 8;
    private static final long SPOTIFY_TIMEOUT_SECONDS = 12;

    private final SoundchartsMusicMetadataAdapter soundcharts;
    private final DuckDuckGoSearchAdapter duckDuckGo;
    private final SpotifyApiClient spotifyApiClient;
    private final SpotifyMapper spotifyMapper;
    private final SpotifyRateLimiter spotifyRateLimiter;

    public MusicLookupService(SoundchartsMusicMetadataAdapter soundcharts,
                              DuckDuckGoSearchAdapter duckDuckGo,
                              SpotifyApiClient spotifyApiClient,
                              SpotifyMapper spotifyMapper,
                              SpotifyRateLimiter spotifyRateLimiter) {
        this.soundcharts = soundcharts;
        this.duckDuckGo = duckDuckGo;
        this.spotifyApiClient = spotifyApiClient;
        this.spotifyMapper = spotifyMapper;
        this.spotifyRateLimiter = spotifyRateLimiter;
    }

    public MusicLookupResult lookup(String query) {
        if (query == null || query.isBlank()) {
            return new MusicLookupResult(false, "none", query, List.of(), null);
        }
        log.info("MusicLookup: '{}'", query);

        final List<EnrichedTrackMetadata> soundchartsTracks;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            soundchartsTracks = CompletableFuture
                    .supplyAsync(() -> soundcharts.searchByTerm(query, SEARCH_LIMIT), executor)
                    .completeOnTimeout(List.of(), SOUNDCHARTS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .exceptionally(error -> {
                        log.warn("Soundcharts lookup failed for '{}': {}", query, rootMessage(error));
                        return List.of();
                    })
                    .join();
        }

        if (!soundchartsTracks.isEmpty()) {
            log.info("MusicLookup: {} track(s) via Soundcharts for '{}'", soundchartsTracks.size(), query);
            return new MusicLookupResult(true, "soundcharts", query, soundchartsTracks, null);
        }

        final Optional<String> webSummary;
        final List<EnrichedTrackMetadata> spotifyTracks;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var webFuture = CompletableFuture
                    .supplyAsync(() -> searchWebSafely(query), executor)
                    .completeOnTimeout(Optional.empty(), WEB_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .exceptionally(error -> {
                        log.warn("Web lookup failed for '{}': {}", query, rootMessage(error));
                        return Optional.empty();
                    });

            final var spotifyFuture = CompletableFuture
                    .supplyAsync(() -> searchSpotify(query), executor)
                    .completeOnTimeout(List.of(), SPOTIFY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .exceptionally(error -> {
                        log.warn("Spotify lookup failed for '{}': {}", query, rootMessage(error));
                        return List.of();
                    });

            webSummary = webFuture.join();
            spotifyTracks = spotifyFuture.join();
        }

        if (!spotifyTracks.isEmpty()) {
            log.info("MusicLookup: {} track(s) via Spotify for '{}'", spotifyTracks.size(), query);
            return new MusicLookupResult(true, "spotify", query, spotifyTracks, webSummary.orElse(null));
        }

        if (webSummary.isPresent()) {
            log.info("MusicLookup: web-only result for '{}'", query);
            return new MusicLookupResult(true, "web", query, List.of(), webSummary.get());
        }

        log.info("MusicLookup: nothing found for '{}'", query);
        return new MusicLookupResult(false, "none", query, List.of(), null);
    }

    private Optional<String> searchWebSafely(String query) {
        try {
            return duckDuckGo.search(query);
        } catch (Exception e) {
            log.warn("DuckDuckGo search failed for '{}': {}", query, e.getMessage());
            return Optional.empty();
        }
    }

    private List<EnrichedTrackMetadata> searchSpotify(String query) {
        try {
            spotifyRateLimiter.acquire();
            final var response = spotifyApiClient.searchTracks(query, "track", SEARCH_LIMIT);
            if (response == null || response.tracks() == null || response.tracks().items() == null) {
                return List.of();
            }
            return response.tracks().items().stream()
                    .filter(Objects::nonNull)
                    .map(item -> spotifyMapper.toEnrichedMetadata(item, null, List.of()))
                    .toList();
        } catch (Exception e) {
            log.warn("Spotify search failed for '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
