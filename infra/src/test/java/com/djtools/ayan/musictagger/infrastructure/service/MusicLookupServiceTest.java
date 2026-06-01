package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.SoundchartsMusicMetadataAdapter;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyApiClient;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyMapper;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyRateLimiter;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifyArtistItem;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifySearchResponse;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifySearchTracks;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifyTrackItem;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.web.DuckDuckGoSearchAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MusicLookupServiceTest {

    @Mock SoundchartsMusicMetadataAdapter soundcharts;
    @Mock DuckDuckGoSearchAdapter duckDuckGo;
    @Mock SpotifyApiClient spotifyApiClient;
    @Mock SpotifyRateLimiter spotifyRateLimiter;

    private MusicLookupService service;

    @BeforeEach
    void setUp() {
        service = new MusicLookupService(soundcharts, duckDuckGo, spotifyApiClient, new SpotifyMapper(), spotifyRateLimiter);
    }

    private EnrichedTrackMetadata track(String id, String artist, String title) {
        return new EnrichedTrackMetadata(id, artist, title, null,
                List.of(), List.of(), null, null, null, List.of("soundcharts"), null, null, null, null);
    }

    @Test
    void lookup_returnsSoundchartsResultFirst() {
        when(soundcharts.searchByTerm("dua lipa levitating", 8))
                .thenReturn(List.of(track("sc-1", "Dua Lipa", "Levitating")));

        var result = service.lookup("dua lipa levitating");

        assertThat(result.found()).isTrue();
        assertThat(result.source()).isEqualTo("soundcharts");
        assertThat(result.tracks()).hasSize(1);
        assertThat(result.tracks().getFirst().artist()).isEqualTo("Dua Lipa");
        verifyNoInteractions(duckDuckGo, spotifyApiClient);
    }

    @Test
    void lookup_fallsBackToWebWhenSoundchartsEmpty() {
        when(soundcharts.searchByTerm(any(), anyInt())).thenReturn(List.of());
        when(duckDuckGo.search("unknown artist")).thenReturn(Optional.of("Some web summary about music"));
        when(spotifyApiClient.searchTracks(anyString(), anyString(), anyInt()))
                .thenReturn(new SpotifySearchResponse(new SpotifySearchTracks(List.of(), 0)));

        var result = service.lookup("unknown artist");

        assertThat(result.found()).isTrue();
        assertThat(result.source()).isEqualTo("web");
        assertThat(result.webSummary()).isEqualTo("Some web summary about music");
        assertThat(result.tracks()).isEmpty();
    }

    @Test
    void lookup_fallsBackToSpotifyWhenSoundchartsAndWebEmpty() {
        when(soundcharts.searchByTerm(any(), anyInt())).thenReturn(List.of());
        when(duckDuckGo.search(anyString())).thenReturn(Optional.empty());
        var spotifyItem = new SpotifyTrackItem("sp-1", "Song Title",
                List.of(new SpotifyArtistItem("art-1", "Artist")), null, 200000L, 70);
        when(spotifyApiClient.searchTracks(anyString(), anyString(), anyInt()))
                .thenReturn(new SpotifySearchResponse(new SpotifySearchTracks(List.of(spotifyItem), 1)));

        var result = service.lookup("some query");

        assertThat(result.found()).isTrue();
        assertThat(result.source()).isEqualTo("spotify");
        assertThat(result.tracks()).hasSize(1);
        assertThat(result.tracks().getFirst().title()).isEqualTo("Song Title");
    }

    @Test
    void lookup_returnsNotFoundWhenAllSourcesEmpty() {
        when(soundcharts.searchByTerm(any(), anyInt())).thenReturn(List.of());
        when(duckDuckGo.search(anyString())).thenReturn(Optional.empty());
        when(spotifyApiClient.searchTracks(anyString(), anyString(), anyInt()))
                .thenReturn(new SpotifySearchResponse(new SpotifySearchTracks(List.of(), 0)));

        var result = service.lookup("xyzabc123");

        assertThat(result.found()).isFalse();
        assertThat(result.source()).isEqualTo("none");
        assertThat(result.tracks()).isEmpty();
        assertThat(result.webSummary()).isNull();
    }

    @Test
    void lookup_emptyQuery_returnsNotFoundWithoutCallingApis() {
        var result = service.lookup("  ");

        assertThat(result.found()).isFalse();
        verifyNoInteractions(soundcharts, duckDuckGo, spotifyApiClient);
    }

    @Test
    void lookup_soundchartsException_continuesChain() {
        when(soundcharts.searchByTerm(any(), anyInt())).thenReturn(List.of()); // already handles internally
        when(duckDuckGo.search(anyString())).thenReturn(Optional.of("web result"));
        when(spotifyApiClient.searchTracks(anyString(), anyString(), anyInt()))
                .thenReturn(new SpotifySearchResponse(new SpotifySearchTracks(List.of(), 0)));

        var result = service.lookup("some query");

        assertThat(result.found()).isTrue();
        assertThat(result.source()).isEqualTo("web");
        verify(duckDuckGo).search("some query");
    }
}
