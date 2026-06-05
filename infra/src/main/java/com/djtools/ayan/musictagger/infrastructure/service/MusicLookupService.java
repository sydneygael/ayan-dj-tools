package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;
import com.djtools.ayan.musictagger.domain.port.out.ScannedTrackRepository;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.SoundchartsMusicMetadataAdapter;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyApiClient;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyMapper;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyRateLimiter;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.web.TavilySearchAdapter;
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
 * 1) Internet / Tavily + local collection in parallel — if Tavily finds something, return immediately
 * 2) Soundcharts
 * 3) Spotify — last resort
 */
@Service
public class MusicLookupService {

    private static final Logger log = LoggerFactory.getLogger(MusicLookupService.class);
    private static final int SEARCH_LIMIT = 8;
    private static final int LOCAL_TRACK_LIMIT = 20;
    private static final long SOUNDCHARTS_TIMEOUT_SECONDS = 8;
    private static final long WEB_TIMEOUT_SECONDS = 8;
    private static final long SPOTIFY_TIMEOUT_SECONDS = 12;
    private static final long LOCAL_TIMEOUT_SECONDS = 3;

    private final SoundchartsMusicMetadataAdapter soundcharts;
    private final TavilySearchAdapter tavily;
    private final SpotifyApiClient spotifyApiClient;
    private final SpotifyMapper spotifyMapper;
    private final SpotifyRateLimiter spotifyRateLimiter;
    private final ScannedTrackRepository scannedTrackRepository;

    public MusicLookupService(SoundchartsMusicMetadataAdapter soundcharts,
                              TavilySearchAdapter tavily,
                              SpotifyApiClient spotifyApiClient,
                              SpotifyMapper spotifyMapper,
                              SpotifyRateLimiter spotifyRateLimiter,
                              ScannedTrackRepository scannedTrackRepository) {
        this.soundcharts = soundcharts;
        this.tavily = tavily;
        this.spotifyApiClient = spotifyApiClient;
        this.spotifyMapper = spotifyMapper;
        this.spotifyRateLimiter = spotifyRateLimiter;
        this.scannedTrackRepository = scannedTrackRepository;
    }

    public MusicLookupResult lookup(String query) {
        if (query == null || query.isBlank()) {
            return new MusicLookupResult(false, "none", query, List.of(), null, List.of());
        }
        log.info("MusicLookup: '{}'", query);

        // Phase 1: Internet (Tavily) + local in parallel — fastest sources
        final Optional<String> webSummary;
        final List<MusicFileInfo> localTracks;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var webFuture = CompletableFuture
                    .supplyAsync(() -> searchWebSafely(query), executor)
                    .completeOnTimeout(Optional.empty(), WEB_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .exceptionally(error -> {
                        log.warn("Web lookup failed for '{}': {}", query, rootMessage(error));
                        return Optional.empty();
                    });
            final var localFuture = CompletableFuture
                    .supplyAsync(() -> scannedTrackRepository.findByArtist(query, LOCAL_TRACK_LIMIT), executor)
                    .completeOnTimeout(List.of(), LOCAL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .exceptionally(error -> {
                        log.warn("Local lookup failed for '{}': {}", query, rootMessage(error));
                        return List.of();
                    });
            webSummary = webFuture.join();
            localTracks = localFuture.join();
        }

        if (webSummary.isPresent()) {
            log.info("MusicLookup: web result for '{}', {} local", query, localTracks.size());
            return new MusicLookupResult(true, "web", query, List.of(), webSummary.get(), localTracks);
        }

        // Phase 2: Soundcharts
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
            log.info("MusicLookup: {} track(s) via Soundcharts for '{}', {} local", soundchartsTracks.size(), query, localTracks.size());
            return new MusicLookupResult(true, "soundcharts", query, soundchartsTracks, null, localTracks);
        }

        // Phase 3: Spotify — last resort
        final List<EnrichedTrackMetadata> spotifyTracks;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            spotifyTracks = CompletableFuture
                    .supplyAsync(() -> searchSpotify(query), executor)
                    .completeOnTimeout(List.of(), SPOTIFY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .exceptionally(error -> {
                        log.warn("Spotify lookup failed for '{}': {}", query, rootMessage(error));
                        return List.of();
                    })
                    .join();
        }

        if (!spotifyTracks.isEmpty()) {
            log.info("MusicLookup: {} track(s) via Spotify for '{}', {} local", spotifyTracks.size(), query, localTracks.size());
            return new MusicLookupResult(true, "spotify", query, spotifyTracks, null, localTracks);
        }

        if (!localTracks.isEmpty()) {
            log.info("MusicLookup: local-only result for '{}', {} track(s)", query, localTracks.size());
            return new MusicLookupResult(true, "local", query, List.of(), null, localTracks);
        }

        log.info("MusicLookup: nothing found for '{}'", query);
        return new MusicLookupResult(false, "none", query, List.of(), null, List.of());
    }

    private Optional<String> searchWebSafely(String query) {
        try {
            return tavily.search(query);
        } catch (Exception e) {
            log.warn("Tavily search failed for '{}': {}", query, e.getMessage());
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
