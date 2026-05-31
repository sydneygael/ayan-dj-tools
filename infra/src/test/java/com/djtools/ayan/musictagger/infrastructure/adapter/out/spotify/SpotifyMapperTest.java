package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify;

import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpotifyMapperTest {

    private final SpotifyMapper mapper = new SpotifyMapper();

    @Test
    void shouldMapFullTrackWithAudioFeatures() {
        var track = new SpotifyTrackItem(
                "t1", "Song",
                List.of(new SpotifyArtistItem("a1", "DJ Test")),
                new SpotifyAlbum("alb1", "Album", "2024-06-15", List.of()),
                210000L, 85
        );
        var audioFeatures = new SpotifyAudioFeatures("t1", 0.8f, 0.7f, 0.6f, 0.1f, 0.05f, 0.03f, 128.0f, 0, 1, 4);
        var genres = List.of("Electronic", "House");

        var result = mapper.toEnrichedMetadata(track, audioFeatures, genres);

        assertThat(result.sourceId()).isEqualTo("t1");
        assertThat(result.artist()).isEqualTo("DJ Test");
        assertThat(result.title()).isEqualTo("Song");
        assertThat(result.album()).isEqualTo("Album");
        assertThat(result.releaseYear()).isEqualTo(2024);
        assertThat(result.popularity()).isEqualTo(85);
        assertThat(result.durationMs()).isEqualTo(210000L);
        assertThat(result.genres()).containsExactly("Electronic", "House");
        assertThat(result.audioFeatures()).isNotNull();
        assertThat(result.audioFeatures().bpm()).isEqualTo(128.0);
        assertThat(result.audioFeatures().musicalKey()).isEqualTo("C");
        assertThat(result.audioFeatures().mode()).isEqualTo("Major");
    }

    @Test
    void shouldMapTrackWithNullAudioFeatures() {
        var track = new SpotifyTrackItem(
                "t1", "Song",
                List.of(new SpotifyArtistItem("a1", "DJ Test")),
                new SpotifyAlbum("alb1", "Album", "2024", List.of()),
                210000L, 85
        );

        var result = mapper.toEnrichedMetadata(track, null, List.of("Pop"));

        assertThat(result.audioFeatures()).isNull();
        assertThat(result.artist()).isEqualTo("DJ Test");
        assertThat(result.genres()).containsExactly("Pop");
    }
}
