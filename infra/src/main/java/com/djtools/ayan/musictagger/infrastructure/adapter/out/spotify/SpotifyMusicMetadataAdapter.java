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
import java.util.Optional;

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
        Optional<EnrichmentResult> cached = cacheService.get(artist, title);
        if (cached.isPresent()) {
            log.debug("Cache hit for {}:{}", artist, title);
            return cached.get();
        }

        try {
            rateLimiter.acquire();

            String query = "artist:" + artist + " track:" + title;
            SpotifySearchResponse searchResponse = apiClient.searchTracks(query, "track", 5);

            if (searchResponse.tracks() == null || searchResponse.tracks().items().isEmpty()) {
                var result = EnrichmentResult.notFound();
                cacheService.put(artist, title, result);
                return result;
            }

            SpotifyTrackItem bestMatch = searchResponse.tracks().items().stream()
                    .max(Comparator.comparingInt(SpotifyTrackItem::popularity))
                    .orElseThrow();

            SpotifyAudioFeatures audioFeatures = fetchAudioFeaturesSafely(bestMatch.id());

            List<String> genres = fetchGenresSafely(bestMatch);

            var metadata = mapper.toEnrichedMetadata(bestMatch, audioFeatures, genres);
            var result = EnrichmentResult.success(metadata);
            cacheService.put(artist, title, result);
            return result;

        } catch (Exception e) {
            log.error("Spotify enrichment failed for {}:{}", artist, title, e);
            return EnrichmentResult.error(e.getMessage());
        }
    }

    private SpotifyAudioFeatures fetchAudioFeaturesSafely(String trackId) {
        try {
            rateLimiter.acquire();
            return apiClient.getAudioFeatures(trackId);
        } catch (HttpClientErrorException.Forbidden e) {
            log.warn("Audio features unavailable (403) for track {}", trackId);
            return null;
        } catch (SpotifyApiException e) {
            if (e.getStatusCode() == 403) {
                log.warn("Audio features unavailable (403) for track {}", trackId);
                return null;
            }
            throw e;
        }
    }

    private List<String> fetchGenresSafely(SpotifyTrackItem track) {
        String artistId = track.primaryArtistId();
        if (artistId == null) {
            return List.of();
        }
        try {
            rateLimiter.acquire();
            var artist = apiClient.getArtist(artistId);
            return artist.genres() != null ? artist.genres() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch genres for artist {}", artistId, e);
            return List.of();
        }
    }
}
