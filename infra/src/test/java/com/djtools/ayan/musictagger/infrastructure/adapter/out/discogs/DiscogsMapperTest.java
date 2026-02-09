package com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs;

import com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs.dto.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiscogsMapperTest {

    private final DiscogsMapper mapper = new DiscogsMapper();

    @Test
    void shouldMapFullReleaseWithLabelsAndStyles() {
        var searchResult = new DiscogsSearchResult(
                12345, "Daft Punk - Random Access Memories", 2013,
                List.of("Electronic"), List.of("Disco", "Synth-pop"),
                "France", "88883716861", "https://api.discogs.com/releases/12345"
        );
        var release = new DiscogsRelease(
                12345, "Random Access Memories",
                List.of("Electronic", "Pop"), List.of("Disco", "Synth-pop", "Funk"),
                "France", null, 2013,
                List.of(new DiscogsLabel(1, "Columbia", "88883716861")),
                List.of(new DiscogsTrack("A1", "Give Life Back to Music", "4:35"))
        );

        var result = mapper.toEnrichedMetadata(searchResult, release);

        assertThat(result.sourceId()).isEqualTo("discogs:12345");
        assertThat(result.artist()).isEqualTo("Daft Punk");
        assertThat(result.title()).isEqualTo("Random Access Memories");
        assertThat(result.genres()).containsExactly("Electronic", "Pop");
        assertThat(result.styles()).containsExactly("Disco", "Synth-pop", "Funk");
        assertThat(result.label()).isEqualTo("Columbia");
        assertThat(result.country()).isEqualTo("France");
        assertThat(result.releaseYear()).isEqualTo(2013);
        assertThat(result.isrc()).isNull();
        assertThat(result.audioFeatures()).isNull();
    }

    @Test
    void shouldHandleReleaseWithNoLabelsOrStyles() {
        var searchResult = new DiscogsSearchResult(
                99, "Unknown - Track", null,
                List.of(), null, null, null, null
        );
        var release = new DiscogsRelease(
                99, "Track", List.of(), null, null, null, 2020,
                List.of(), List.of()
        );

        var result = mapper.toEnrichedMetadata(searchResult, release);

        assertThat(result.sourceId()).isEqualTo("discogs:99");
        assertThat(result.artist()).isEqualTo("Unknown");
        assertThat(result.title()).isEqualTo("Track");
        assertThat(result.genres()).isEmpty();
        assertThat(result.styles()).isEmpty();
        assertThat(result.label()).isNull();
        assertThat(result.releaseYear()).isEqualTo(2020);
    }
}
