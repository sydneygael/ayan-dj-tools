package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify;

import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifyAudioFeatures;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifySearchResponse;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifyTrackItem;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.exception.SpotifyApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Comparator;
import java.util.List;

public class SpotifyMusicMetadataAdapter implements MusicMetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(SpotifyMusicMetadataAdapter.class);

    private final SpotifyApiClient apiClient;
    private final SpotifyRateLimiter rateLimiter;
    private final SpotifyCacheService cacheService;
    private final SpotifyMapper mapper;

    public SpotifyMusicMetadataAdapter(
            SpotifyApiClient apiClient,
            SpotifyRateLimiter rateLimiter,
            SpotifyCacheService cacheService,
            SpotifyMapper mapper
    ) {
        this.apiClient = apiClient;
        this.rateLimiter = rateLimiter;
        this.cacheService = cacheService;
        this.mapper = mapper;
    }

    @Override
    public EnrichmentResult enrich(String artist, String title) {
        log.info("Spotify enrich → query: artist='{}' title='{}'", artist, title);

        final var cached = cacheService.get(artist, title);
        if (cached.isPresent()) {
            log.info("Spotify cache hit for '{}' – '{}'", artist, title);
            return cached.get();
        }

        try {
            rateLimiter.acquire();

            final var query = "artist:" + artist + " track:" + title;
            log.debug("Spotify search query: {}", query);
            final var searchResponse = apiClient.searchTracks(query, "track", 5);

            if (searchResponse.tracks() == null || searchResponse.tracks().items() == null) {
                log.warn("Spotify search returned null tracks for '{} – {}'", artist, title);
                var result = EnrichmentResult.notFound();
                cacheService.put(artist, title, result);
                return result;
            }

            final var items = searchResponse.tracks().items();
            log.debug("Spotify search: {} result(s) for '{} – {}'", items.size(), artist, title);

            if (items.isEmpty()) {
                log.info("Spotify NOT_FOUND for '{} – {}'", artist, title);
                var result = EnrichmentResult.notFound();
                cacheService.put(artist, title, result);
                return result;
            }

            final var bestMatch = items.stream()
                    .max(Comparator.comparingInt(t -> t.popularity() != null ? t.popularity() : 0))
                    .orElseThrow();

            log.info("Spotify best match: '{}' by '{}' (id={}, popularity={})",
                    bestMatch.name(), bestMatch.primaryArtist(), bestMatch.id(), bestMatch.popularity());

            final var audioFeatures = fetchAudioFeaturesSafely(bestMatch.id());
            final var genres = fetchGenresSafely(bestMatch);

            var metadata = mapper.toEnrichedMetadata(bestMatch, audioFeatures, genres);
            log.info("Spotify enrichment OK for '{} – {}': album='{}', genres={}, bpm={}",
                    artist, title, metadata.album(), metadata.genres(),
                    audioFeatures != null ? audioFeatures.bpm() : "—");

            var result = EnrichmentResult.success(metadata);
            cacheService.put(artist, title, result);
            return result;

        } catch (Exception e) {
            log.error("Spotify enrichment FAILED for '{} – {}': {}", artist, title, e.getMessage(), e);
            return EnrichmentResult.error(e.getMessage());
        }
    }

    private SpotifyAudioFeatures fetchAudioFeaturesSafely(String trackId) {
        log.debug("Fetching audio features for track {}", trackId);
        try {
            rateLimiter.acquire();
            final var features = apiClient.getAudioFeatures(trackId);
            if (features != null) {
                log.debug("Audio features for {}: tempo={}, key={}, mode={}", trackId,
                        features.tempo(), features.key(), features.mode());
            } else {
                log.warn("Audio features response is null for track {}", trackId);
            }
            return features;
        } catch (HttpClientErrorException.Forbidden e) {
            log.warn("Audio features 403 for track {} — endpoint restreint depuis nov. 2024 "
                    + "(Extended Quota Mode requis). BPM/tonalité non disponibles. "
                    + "Voir GET /api/spotify/check pour diagnostiquer.", trackId);
            return null;
        } catch (SpotifyApiException e) {
            if (e.getStatusCode() == 403) {
                log.warn("Audio features 403 for track {} — Extended Quota Mode requis. "
                        + "Voir GET /api/spotify/check.", trackId);
                return null;
            }
            log.error("SpotifyApiException fetching audio features for track {}: {}", trackId, e.getMessage());
            throw e;
        }
    }

    private List<String> fetchGenresSafely(SpotifyTrackItem track) {
        final var artistId = track.primaryArtistId();
        if (artistId == null) {
            log.debug("No primary artist id for track '{}', skipping genre fetch", track.name());
            return List.of();
        }
        log.debug("Fetching genres for artist id={}", artistId);
        try {
            rateLimiter.acquire();
            var artist = apiClient.getArtist(artistId);
            final List<String> genres = artist.genres() != null ? artist.genres() : List.of();
            log.debug("Genres for artist {}: {}", artist.name(), genres);
            return genres;
        } catch (Exception e) {
            log.warn("Failed to fetch genres for artist {} ({}): {}", track.primaryArtist(), artistId, e.getMessage());
            return List.of();
        }
    }
}
