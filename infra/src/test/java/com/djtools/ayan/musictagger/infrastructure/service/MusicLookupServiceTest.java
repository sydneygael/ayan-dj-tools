package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.out.ScannedTrackRepository;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.SoundchartsMusicMetadataAdapter;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyApiClient;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyMapper;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyRateLimiter;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifyArtistItem;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifySearchResponse;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifySearchTracks;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifyTrackItem;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.web.TavilySearchAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

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
    @Mock TavilySearchAdapter tavily;
    @Mock SpotifyApiClient spotifyApiClient;
    @Mock SpotifyRateLimiter spotifyRateLimiter;
    @Mock ScannedTrackRepository scannedTrackRepository;
    @Mock StringRedisTemplate stringRedis;
    @Mock ValueOperations<String, String> valueOps;

    private MusicLookupService service;

    @BeforeEach
    void setUp() {
        when(scannedTrackRepository.findByArtist(anyString(), anyInt())).thenReturn(List.of());
        when(stringRedis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        service = new MusicLookupService(soundcharts, tavily, spotifyApiClient, new SpotifyMapper(), spotifyRateLimiter, scannedTrackRepository, stringRedis);
    }

    private EnrichedTrackMetadata track(String id, String artist, String title) {
        return new EnrichedTrackMetadata(id, artist, title, null,
                List.of(), List.of(), null, null, null, List.of("soundcharts"), null, null, null, null, null, null, null);
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
        verifyNoInteractions(tavily, spotifyApiClient);
    }

    @Test
    void lookup_fallsBackToWebWhenSoundchartsEmpty() {
        when(soundcharts.searchByTerm(any(), anyInt())).thenReturn(List.of());
        when(tavily.search("unknown artist")).thenReturn(Optional.of("Some web summary about music"));
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
        when(tavily.search(anyString())).thenReturn(Optional.empty());
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
        when(tavily.search(anyString())).thenReturn(Optional.empty());
        when(spotifyApiClient.searchTracks(anyString(), anyString(), anyInt()))
                .thenReturn(new SpotifySearchResponse(new SpotifySearchTracks(List.of(), 0)));

        var result = service.lookup("xyzabc123");

        assertThat(result.found()).isFalse();
        assertThat(result.source()).isEqualTo("none");
        assertThat(result.tracks()).isEmpty();
        assertThat(result.webSummary()).isNull();
    }

    @Test
    void lookup_localTracksIncludedWhenSoundchartsReturnsResults() {
        var localFile = new MusicFileInfo(new Filepath("/music/dua-lipa-levitating.mp3"),
                "dua-lipa-levitating.mp3", "Dua Lipa", "Levitating", null, null, null, null, 0L, 0L, false);
        when(soundcharts.searchByTerm("dua lipa", 8))
                .thenReturn(List.of(track("sc-1", "Dua Lipa", "Levitating")));
        when(scannedTrackRepository.findByArtist("dua lipa", 20)).thenReturn(List.of(localFile));

        var result = service.lookup("dua lipa");

        assertThat(result.source()).isEqualTo("soundcharts");
        assertThat(result.localTracks()).hasSize(1);
        assertThat(result.localTracks().getFirst().artist()).isEqualTo("Dua Lipa");
    }

    @Test
    void lookup_localOnlyWhenAllExternalSourcesEmpty() {
        var localFile = new MusicFileInfo(new Filepath("/music/unknown-track.mp3"),
                "unknown-track.mp3", "Rare Artist", "Track 1", null, null, null, null, 0L, 0L, false);
        when(soundcharts.searchByTerm(any(), anyInt())).thenReturn(List.of());
        when(tavily.search(anyString())).thenReturn(Optional.empty());
        when(spotifyApiClient.searchTracks(anyString(), anyString(), anyInt()))
                .thenReturn(new SpotifySearchResponse(new SpotifySearchTracks(List.of(), 0)));
        when(scannedTrackRepository.findByArtist("Rare Artist", 20)).thenReturn(List.of(localFile));

        var result = service.lookup("Rare Artist");

        assertThat(result.found()).isTrue();
        assertThat(result.source()).isEqualTo("local");
        assertThat(result.localTracks()).hasSize(1);
    }

    @Test
    void lookup_emptyQuery_returnsNotFoundWithoutCallingApis() {
        var result = service.lookup("  ");

        assertThat(result.found()).isFalse();
        verifyNoInteractions(soundcharts, tavily, spotifyApiClient);
    }

    @Test
    void lookup_soundchartsException_continuesChain() {
        when(soundcharts.searchByTerm(any(), anyInt())).thenReturn(List.of()); // already handles internally
        when(tavily.search(anyString())).thenReturn(Optional.of("web result"));
        when(spotifyApiClient.searchTracks(anyString(), anyString(), anyInt()))
                .thenReturn(new SpotifySearchResponse(new SpotifySearchTracks(List.of(), 0)));

        var result = service.lookup("some query");

        assertThat(result.found()).isTrue();
        assertThat(result.source()).isEqualTo("web");
        verify(tavily).search("some query");
    }
}
