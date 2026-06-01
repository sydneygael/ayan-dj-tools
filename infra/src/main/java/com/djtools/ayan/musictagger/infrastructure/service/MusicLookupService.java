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

/**
 * Recherche musicale externe en chaîne : Soundcharts → Internet (DuckDuckGo) → Spotify.
 *
 * <p>Différent de {@code SongSearchService} qui cherche dans la collection locale vectorisée (Qdrant).
 * Ici on interroge des sources externes pour répondre à des questions de découverte :
 * « Qui est cet artiste ? », « Quels morceaux existe-t-il ? », « Infos sur cet album ? ».
 */
@Service
public class MusicLookupService {

    private static final Logger log = LoggerFactory.getLogger(MusicLookupService.class);
    private static final int SEARCH_LIMIT = 8;

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

        // ── 1. Soundcharts ────────────────────────────────────────────────────
        final var scTracks = soundcharts.searchByTerm(query, SEARCH_LIMIT);
        if (!scTracks.isEmpty()) {
            log.info("MusicLookup: {} track(s) via Soundcharts for '{}'", scTracks.size(), query);
            return new MusicLookupResult(true, "soundcharts", query, scTracks, null);
        }

        // ── 2. Internet (DuckDuckGo) ──────────────────────────────────────────
        final var webSummary = duckDuckGo.search(query);

        // ── 3. Spotify ────────────────────────────────────────────────────────
        final var spotifyTracks = searchSpotify(query);
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
}
