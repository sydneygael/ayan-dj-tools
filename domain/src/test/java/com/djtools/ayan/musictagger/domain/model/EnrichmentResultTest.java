package com.djtools.ayan.musictagger.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnrichmentResultTest {

    private static final EnrichedTrackMetadata SAMPLE_METADATA = new EnrichedTrackMetadata(
            "spotify:123", "Artist", "Title", "Album",
            List.of("Electronic"), List.of(), null, null, null, List.of(),
            2024, 80, 210000, null
    );

    @Test
    void successShouldReturnData() {
        var result = EnrichmentResult.success(SAMPLE_METADATA);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data()).isEqualTo(SAMPLE_METADATA);
    }

    @Test
    void notFoundShouldNotBeSuccess() {
        var result = EnrichmentResult.notFound();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result).isInstanceOf(EnrichmentResult.NotFound.class);
    }

    @Test
    void errorShouldNotBeSuccess() {
        var result = EnrichmentResult.error("API failure");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result).isInstanceOf(EnrichmentResult.Error.class);
    }

    @Test
    void dataShouldThrowOnNotFound() {
        var result = EnrichmentResult.notFound();

        assertThatThrownBy(result::data)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No data available");
    }

    @Test
    void dataShouldThrowOnError() {
        var result = EnrichmentResult.error("fail");

        assertThatThrownBy(result::data)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void errorShouldContainMessage() {
        var result = EnrichmentResult.error("API failure");

        assertThat(result).isInstanceOf(EnrichmentResult.Error.class);
        assertThat(((EnrichmentResult.Error) result).message()).isEqualTo("API failure");
    }
}
