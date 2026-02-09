package com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiscogsCacheServiceTest {

    private final DiscogsCacheService cacheService = new DiscogsCacheService(60);

    @Test
    void shouldReturnEmptyForUnknownKey() {
        assertThat(cacheService.get("Unknown", "Song")).isEmpty();
    }

    @Test
    void shouldReturnCachedResult() {
        var result = EnrichmentResult.success(sampleMetadata());
        cacheService.put("Artist", "Title", result);

        var cached = cacheService.get("Artist", "Title");

        assertThat(cached).isPresent().contains(result);
    }

    @Test
    void shouldBeCaseInsensitive() {
        var result = EnrichmentResult.success(sampleMetadata());
        cacheService.put("ARTIST", "TITLE", result);

        assertThat(cacheService.get("artist", "title")).isPresent().contains(result);
        assertThat(cacheService.get("Artist", "Title")).isPresent().contains(result);
    }

    private static EnrichedTrackMetadata sampleMetadata() {
        return new EnrichedTrackMetadata(
                "discogs:1", "Artist", "Title", null,
                List.of("Electronic"), List.of("House"), "Label", "UK", null, List.of(),
                2024, 0, 0, null
        );
    }
}
