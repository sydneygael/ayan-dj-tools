package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify;

import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@ExtendWith(MockitoExtension.class)
class SpotifyMusicMetadataAdapterTest {

    @Mock SpotifyApiClient apiClient;
    @Mock SpotifyRateLimiter rateLimiter;
    @Mock SpotifyCacheService cacheService;

    private SpotifyMusicMetadataAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SpotifyMusicMetadataAdapter(apiClient, rateLimiter, cacheService, new SpotifyMapper());
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

        var track = trackItem("t1", "Title", "Artist", "a1", "Album", "2024-01-15", 80);
        when(apiClient.searchTracks(anyString(), eq("track"), eq(5)))
                .thenReturn(searchResponse(track));
        when(apiClient.getAudioFeatures("t1"))
                .thenReturn(audioFeatures());
        when(apiClient.getArtist("a1"))
                .thenReturn(new SpotifyArtist("a1", "Artist", List.of("Electronic", "House"), 75, new SpotifyFollowers(1000)));

        var result = adapter.enrich("Artist", "Title");

        assertThat(result.isSuccess()).isTrue();
        var data = result.data();
        assertThat(data.artist()).isEqualTo("Artist");
        assertThat(data.title()).isEqualTo("Title");
        assertThat(data.album()).isEqualTo("Album");
        assertThat(data.genres()).containsExactly("Electronic", "House");
        assertThat(data.audioFeatures()).isNotNull();
        assertThat(data.audioFeatures().bpm()).isEqualTo(128.0);
        verify(cacheService).put(eq("Artist"), eq("Title"), any(EnrichmentResult.class));
    }

    @Test
    void shouldReturnNotFoundWhenNoResults() {
        when(cacheService.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(apiClient.searchTracks(anyString(), eq("track"), eq(5)))
                .thenReturn(new SpotifySearchResponse(new SpotifySearchTracks(List.of(), 0)));

        var result = adapter.enrich("Unknown", "Song");

        assertThat(result).isInstanceOf(EnrichmentResult.NotFound.class);
        verify(cacheService).put(eq("Unknown"), eq("Song"), any(EnrichmentResult.class));
    }

    @Test
    void shouldReturnErrorOnApiException() {
        when(cacheService.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(apiClient.searchTracks(anyString(), eq("track"), eq(5)))
                .thenThrow(new RuntimeException("Connection timeout"));

        var result = adapter.enrich("Artist", "Title");

        assertThat(result).isInstanceOf(EnrichmentResult.Error.class);
        assertThat(((EnrichmentResult.Error) result).message()).contains("Connection timeout");
    }

    @Test
    void shouldGracefullyDegradeOnAudioFeatures403() {
        when(cacheService.get(anyString(), anyString())).thenReturn(Optional.empty());

        var track = trackItem("t1", "Title", "Artist", "a1", "Album", "2024-01-15", 80);
        when(apiClient.searchTracks(anyString(), eq("track"), eq(5)))
                .thenReturn(searchResponse(track));
        when(apiClient.getAudioFeatures("t1"))
                .thenThrow(HttpClientErrorException.create(FORBIDDEN, "Forbidden", null, null, null));
        when(apiClient.getArtist("a1"))
                .thenReturn(new SpotifyArtist("a1", "Artist", List.of("Pop"), 50, new SpotifyFollowers(500)));

        var result = adapter.enrich("Artist", "Title");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data().audioFeatures()).isNull();
        assertThat(result.data().genres()).containsExactly("Pop");
    }

    private static SpotifyTrackItem trackItem(String id, String name, String artist, String artistId, String album, String releaseDate, int popularity) {
        return new SpotifyTrackItem(
                id, name,
                List.of(new SpotifyArtistItem(artistId, artist)),
                new SpotifyAlbum(albumId(album), album, releaseDate, List.of()),
                210000, popularity
        );
    }

    private static String albumId(String name) {
        return "album-" + name.toLowerCase().replace(" ", "-");
    }

    private static SpotifySearchResponse searchResponse(SpotifyTrackItem... tracks) {
        return new SpotifySearchResponse(new SpotifySearchTracks(List.of(tracks), tracks.length));
    }

    private static SpotifyAudioFeatures audioFeatures() {
        return new SpotifyAudioFeatures("t1", 0.8f, 0.7f, 0.6f, 0.1f, 0.05f, 0.03f, 128.0f, 0, 1, 4);
    }
}
