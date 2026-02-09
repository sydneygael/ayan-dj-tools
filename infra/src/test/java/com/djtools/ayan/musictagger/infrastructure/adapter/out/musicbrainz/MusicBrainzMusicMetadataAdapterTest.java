package com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz;

import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MusicBrainzMusicMetadataAdapterTest {

    @Mock MusicBrainzApiClient apiClient;
    @Mock MusicBrainzRateLimiter rateLimiter;
    @Mock MusicBrainzCacheService cacheService;

    private MusicBrainzMusicMetadataAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MusicBrainzMusicMetadataAdapter(apiClient, rateLimiter, cacheService, new MusicBrainzMapper());
    }

    @Test
    void shouldReturnCachedResult() {
        var cached = EnrichmentResult.notFound();
        when(cacheService.get("Artist", "Title")).thenReturn(Optional.of(cached));

        var result = adapter.enrich("Artist", "Title");

        assertThat(result).isSameAs(cached);
        verifyNoInteractions(apiClient);
    }

    @Test
    void shouldReturnSuccessOnFullEnrichment() {
        when(cacheService.get(anyString(), anyString())).thenReturn(Optional.empty());

        var recording = new MBRecording(
                "mb-1", 100, "Title", 210000,
                List.of(new MBArtistCredit(new MBArtist("a1", "Artist", "Artist"), "Artist", null)),
                List.of(new MBRelease("r1", "Album", "Official", "2024-06-15", null)),
                List.of("GBAYE1234567"),
                List.of(new MBTag(10, "electronic"))
        );
        when(apiClient.searchRecordings(anyString(), eq("json"), eq(5)))
                .thenReturn(new MBRecordingSearchResponse(1, 0, List.of(recording)));

        var result = adapter.enrich("Artist", "Title");

        assertThat(result.isSuccess()).isTrue();
        var data = result.data();
        assertThat(data.artist()).isEqualTo("Artist");
        assertThat(data.title()).isEqualTo("Title");
        assertThat(data.isrc()).isEqualTo("GBAYE1234567");
        assertThat(data.tags()).containsExactly("electronic");
        verify(cacheService).put(eq("Artist"), eq("Title"), any(EnrichmentResult.class));
    }

    @Test
    void shouldReturnNotFoundWhenNoResults() {
        when(cacheService.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(apiClient.searchRecordings(anyString(), eq("json"), eq(5)))
                .thenReturn(new MBRecordingSearchResponse(0, 0, List.of()));

        var result = adapter.enrich("Unknown", "Song");

        assertThat(result).isInstanceOf(EnrichmentResult.NotFound.class);
        verify(cacheService).put(eq("Unknown"), eq("Song"), any(EnrichmentResult.class));
    }

    @Test
    void shouldReturnErrorOnApiException() {
        when(cacheService.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(apiClient.searchRecordings(anyString(), eq("json"), eq(5)))
                .thenThrow(new RuntimeException("Service unavailable"));

        var result = adapter.enrich("Artist", "Title");

        assertThat(result).isInstanceOf(EnrichmentResult.Error.class);
        assertThat(((EnrichmentResult.Error) result).message()).contains("Service unavailable");
    }

    @Test
    void shouldSelectBestMatchByScore() {
        when(cacheService.get(anyString(), anyString())).thenReturn(Optional.empty());

        var lowScore = new MBRecording(
                "mb-low", 50, "Wrong Title", 100000,
                List.of(new MBArtistCredit(new MBArtist("a1", "Artist", "Artist"), "Artist", null)),
                List.of(), null, null
        );
        var highScore = new MBRecording(
                "mb-high", 100, "Title", 210000,
                List.of(new MBArtistCredit(new MBArtist("a1", "Artist", "Artist"), "Artist", null)),
                List.of(new MBRelease("r1", "Album", "Official", "2024", null)),
                List.of("ISRC123"),
                List.of(new MBTag(5, "pop"))
        );
        when(apiClient.searchRecordings(anyString(), eq("json"), eq(5)))
                .thenReturn(new MBRecordingSearchResponse(2, 0, List.of(lowScore, highScore)));

        var result = adapter.enrich("Artist", "Title");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data().sourceId()).isEqualTo("musicbrainz:mb-high");
        assertThat(result.data().isrc()).isEqualTo("ISRC123");
    }
}
