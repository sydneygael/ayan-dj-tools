package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts;

import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoundchartsMusicMetadataAdapterTest {

    @Mock SoundchartsApiClient apiClient;

    private SoundchartsMusicMetadataAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SoundchartsMusicMetadataAdapter(apiClient, 5);
    }

    @Test
    void shouldSearchThenGetMetadata() {
        var searchTrack = new SoundchartsTrack(
                "song-123",
                "Strobe",
                "Deadmau5",
                List.of(new SoundchartsArtistRef("artist-1", "Deadmau5")),
                "2009-09-28",
                List.of(new SoundchartsGenreRef("electro", List.of("electronic"))),
                List.of(new SoundchartsLabel("Mau5trap", "indie")),
                new SoundchartsIsrc("USUS10900862", "US", "United States"),
                634L,
                null,
                new SoundchartsExternalIds("spotify:track:xyz", null, null, null),
                null,
                false
        );
        var detailTrack = new SoundchartsTrack(
                "song-123",
                "Strobe",
                "Deadmau5",
                List.of(new SoundchartsArtistRef("artist-1", "Deadmau5")),
                "2009-09-28",
                List.of(new SoundchartsGenreRef("electro", List.of("progressive house", "electronic"))),
                List.of(new SoundchartsLabel("Mau5trap", "indie")),
                new SoundchartsIsrc("USUS10900862", "US", "United States"),
                634L,
                new SoundchartsAudio(0.02, 0.5, 0.8, 0.9, 7, 0.1, -7.0, 0, 0.03, 128.0, 4, 0.3),
                new SoundchartsExternalIds("spotify:track:xyz", null, null, null),
                null,
                false
        );

        when(apiClient.searchSongByName(eq("Strobe"), eq(0), eq(10)))
                .thenReturn(new SoundchartsSearchResponse(List.of(searchTrack), new SoundchartsSearchPage(0, 10, 1)));
        when(apiClient.getSongMetadata("song-123"))
                .thenReturn(new SoundchartsSongResponse(detailTrack));
        when(apiClient.getLyricsAnalysis(anyString())).thenReturn(null);

        var result = adapter.enrich("Deadmau5", "Strobe");

        assertThat(result).isInstanceOf(EnrichmentResult.Success.class);
        assertThat(result.data().sourceId()).isEqualTo("song-123");
        assertThat(result.data().artist()).isEqualTo("Deadmau5");
        assertThat(result.data().title()).isEqualTo("Strobe");
        assertThat(result.data().genres()).containsExactly("electro");
        assertThat(result.data().styles()).containsExactly("progressive house", "electronic");
        assertThat(result.data().label()).isEqualTo("Mau5trap");
        assertThat(result.data().isrc()).isEqualTo("USUS10900862");
        assertThat(result.data().country()).isEqualTo("US");
        assertThat(result.data().releaseYear()).isEqualTo(2009);
        assertThat(result.data().durationMs()).isEqualTo(634000L);
        assertThat(result.data().audioFeatures()).isNotNull();
        assertThat(result.data().audioFeatures().bpm()).isEqualTo(128.0);
        assertThat(result.data().audioFeatures().musicalKey()).isEqualTo("G");
        assertThat(result.data().audioFeatures().mode()).isEqualTo("minor");
        verify(apiClient).searchSongByName(eq("Strobe"), eq(0), eq(10));
        verify(apiClient).getSongMetadata("song-123");
    }

    @Test
    void shouldReturnNotFoundWhenSearchHasNoItems() {
        when(apiClient.searchSongByName(eq("Song"), eq(0), eq(10)))
                .thenReturn(new SoundchartsSearchResponse(List.of(), new SoundchartsSearchPage(0, 10, 0)));

        var result = adapter.enrich("Unknown", "Song");

        assertThat(result).isInstanceOf(EnrichmentResult.NotFound.class);
    }
}
