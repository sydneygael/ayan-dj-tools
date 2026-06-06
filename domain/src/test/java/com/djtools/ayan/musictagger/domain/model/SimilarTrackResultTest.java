package com.djtools.ayan.musictagger.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimilarTrackResultTest {

    private static EnrichedTrackMetadata sampleTrack() {
        return new EnrichedTrackMetadata(
                "sp123", "Artist", "Title", "Album",
                List.of("Electronic"), List.of(), "Label", "FR",
                "ISRC123", List.of(), 2024, 80, 210000L, null, null, null, null
        );
    }

    @Test
    void shouldCreateValidResult() {
        var result = new SimilarTrackResult(sampleTrack(), 0.85);

        assertThat(result.track().artist()).isEqualTo("Artist");
        assertThat(result.similarityScore()).isEqualTo(0.85);
    }

    @Test
    void shouldRejectNullTrack() {
        assertThatThrownBy(() -> new SimilarTrackResult(null, 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Track must not be null");
    }

    @Test
    void shouldRejectInvalidScore() {
        assertThatThrownBy(() -> new SimilarTrackResult(sampleTrack(), 1.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 1");
    }
}
