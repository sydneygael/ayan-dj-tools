package com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz;

import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz.dto.MBRecording;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz.dto.MBRecordingSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Optional;

public class MusicBrainzMusicMetadataAdapter implements MusicMetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(MusicBrainzMusicMetadataAdapter.class);

    private final MusicBrainzApiClient apiClient;
    private final MusicBrainzRateLimiter rateLimiter;
    private final MusicBrainzCacheService cacheService;
    private final MusicBrainzMapper mapper;

    public MusicBrainzMusicMetadataAdapter(
            MusicBrainzApiClient apiClient,
            MusicBrainzRateLimiter rateLimiter,
            MusicBrainzCacheService cacheService,
            MusicBrainzMapper mapper
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
            log.debug("MusicBrainz cache hit for {}:{}", artist, title);
            return cached.get();
        }

        try {
            rateLimiter.acquire();

            String query = "artist:" + artist + " AND recording:" + title;
            MBRecordingSearchResponse response = apiClient.searchRecordings(query, "json", 5);

            if (response.recordings() == null || response.recordings().isEmpty()) {
                var result = EnrichmentResult.notFound();
                cacheService.put(artist, title, result);
                return result;
            }

            MBRecording bestMatch = response.recordings().stream()
                    .max(Comparator.comparingInt(MBRecording::score))
                    .orElseThrow();

            var metadata = mapper.toEnrichedMetadata(bestMatch);
            var result = EnrichmentResult.success(metadata);
            cacheService.put(artist, title, result);
            return result;

        } catch (Exception e) {
            log.error("MusicBrainz enrichment failed for {}:{}", artist, title, e);
            return EnrichmentResult.error(e.getMessage());
        }
    }
}
