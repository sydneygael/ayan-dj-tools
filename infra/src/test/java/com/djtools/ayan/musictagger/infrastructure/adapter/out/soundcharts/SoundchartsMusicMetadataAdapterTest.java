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
                List.of(new SoundchartsArtistRef("artist-1", "Deadmau5")),
                "2009-09-28",
                List.of(new SoundchartsGenreRef(1, "Electronic")),
                "Mau5trap",
                "USUS10900862",
                "US",
                634000L,
                new SoundchartsExternalIds("spotify:track:xyz", null, null, null)
        );
        var detailTrack = new SoundchartsTrack(
                "song-123",
                "Strobe",
                List.of(new SoundchartsArtistRef("artist-1", "Deadmau5")),
                "2009-09-28",
                List.of(new SoundchartsGenreRef(1, "Progressive House"), new SoundchartsGenreRef(2, "Electronic")),
                "Mau5trap",
                "USUS10900862",
                "US",
                634000L,
                new SoundchartsExternalIds("spotify:track:xyz", null, null, null)
        );

        when(apiClient.searchSongByName(eq("Deadmau5 Strobe"), eq(0), eq(5)))
                .thenReturn(new SoundchartsSearchResponse(List.of(searchTrack), new SoundchartsSearchPage(0, 5, 1)));
        when(apiClient.getSongMetadata("song-123"))
                .thenReturn(new SoundchartsSongResponse(detailTrack));

        var result = adapter.enrich("Deadmau5", "Strobe");

        assertThat(result).isInstanceOf(EnrichmentResult.Success.class);
        assertThat(result.data().sourceId()).isEqualTo("song-123");
        assertThat(result.data().artist()).isEqualTo("Deadmau5");
        assertThat(result.data().title()).isEqualTo("Strobe");
        assertThat(result.data().genres()).containsExactly("Progressive House", "Electronic");
        assertThat(result.data().releaseYear()).isEqualTo(2009);
        assertThat(result.data().durationMs()).isEqualTo(634000L);
        verify(apiClient).searchSongByName(eq("Deadmau5 Strobe"), eq(0), eq(5));
        verify(apiClient).getSongMetadata("song-123");
    }

    @Test
    void shouldReturnNotFoundWhenSearchHasNoItems() {
        when(apiClient.searchSongByName(eq("Unknown Song"), eq(0), eq(5)))
                .thenReturn(new SoundchartsSearchResponse(List.of(), new SoundchartsSearchPage(0, 5, 0)));

        var result = adapter.enrich("Unknown", "Song");

        assertThat(result).isInstanceOf(EnrichmentResult.NotFound.class);
    }
}
