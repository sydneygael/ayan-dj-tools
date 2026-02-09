package com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs;

import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs.dto.DiscogsRelease;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs.dto.DiscogsSearchResponse;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs.dto.DiscogsSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class DiscogsMusicMetadataAdapter implements MusicMetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(DiscogsMusicMetadataAdapter.class);

    private final DiscogsApiClient apiClient;
    private final DiscogsRateLimiter rateLimiter;
    private final DiscogsCacheService cacheService;
    private final DiscogsMapper mapper;

    public DiscogsMusicMetadataAdapter(
            DiscogsApiClient apiClient,
            DiscogsRateLimiter rateLimiter,
            DiscogsCacheService cacheService,
            DiscogsMapper mapper
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
            log.debug("Discogs cache hit for {}:{}", artist, title);
            return cached.get();
        }

        try {
            rateLimiter.acquire();

            String query = artist + " " + title;
            DiscogsSearchResponse searchResponse = apiClient.search(query, "release", 5);

            if (searchResponse.results() == null || searchResponse.results().isEmpty()) {
                var result = EnrichmentResult.notFound();
                cacheService.put(artist, title, result);
                return result;
            }

            DiscogsSearchResult bestMatch = searchResponse.results().getFirst();

            rateLimiter.acquire();
            DiscogsRelease release = apiClient.getRelease(bestMatch.id());

            var metadata = mapper.toEnrichedMetadata(bestMatch, release);
            var result = EnrichmentResult.success(metadata);
            cacheService.put(artist, title, result);
            return result;

        } catch (Exception e) {
            log.error("Discogs enrichment failed for {}:{}", artist, title, e);
            return EnrichmentResult.error(e.getMessage());
        }
    }
}
