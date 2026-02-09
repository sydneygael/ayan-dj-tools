package com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz;

import com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz.dto.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MusicBrainzMapperTest {

    private final MusicBrainzMapper mapper = new MusicBrainzMapper();

    @Test
    void shouldMapFullRecordingWithIsrcAndTags() {
        var recording = new MBRecording(
                "mb-id-1", 100, "Title", 210000,
                List.of(new MBArtistCredit(new MBArtist("a1", "Artist", "Artist"), "Artist", null)),
                List.of(new MBRelease("r1", "Album", "Official", "2024-01-15", new MBReleaseGroup("rg1", "Album"))),
                List.of("GBAYE1234567"),
                List.of(new MBTag(10, "electronic"), new MBTag(5, "house"), new MBTag(2, "deep house"))
        );

        var result = mapper.toEnrichedMetadata(recording);

        assertThat(result.sourceId()).isEqualTo("musicbrainz:mb-id-1");
        assertThat(result.artist()).isEqualTo("Artist");
        assertThat(result.title()).isEqualTo("Title");
        assertThat(result.album()).isEqualTo("Album");
        assertThat(result.isrc()).isEqualTo("GBAYE1234567");
        assertThat(result.tags()).containsExactly("electronic", "house", "deep house");
        assertThat(result.releaseYear()).isEqualTo(2024);
        assertThat(result.durationMs()).isEqualTo(210000);
        assertThat(result.genres()).isEmpty();
        assertThat(result.audioFeatures()).isNull();
    }

    @Test
    void shouldHandleRecordingWithNoIsrcOrTags() {
        var recording = new MBRecording(
                "mb-id-2", 80, "Track", null,
                List.of(new MBArtistCredit(new MBArtist("a2", "DJ", "DJ"), "DJ", null)),
                List.of(),
                null,
                null
        );

        var result = mapper.toEnrichedMetadata(recording);

        assertThat(result.sourceId()).isEqualTo("musicbrainz:mb-id-2");
        assertThat(result.artist()).isEqualTo("DJ");
        assertThat(result.title()).isEqualTo("Track");
        assertThat(result.album()).isNull();
        assertThat(result.isrc()).isNull();
        assertThat(result.tags()).isEmpty();
        assertThat(result.releaseYear()).isEqualTo(0);
        assertThat(result.durationMs()).isEqualTo(0);
    }
}
