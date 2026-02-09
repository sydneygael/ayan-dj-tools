package com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MusicBrainzCacheServiceTest {

    private final MusicBrainzCacheService cacheService = new MusicBrainzCacheService(120);

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
                "mb:1", "Artist", "Title", "Album",
                List.of(), List.of(), null, null, "GBAYE1234567", List.of("electronic"),
                2024, 0, 210000, null
        );
    }
}
