package com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs;

import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs.dto.*;
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
class DiscogsMusicMetadataAdapterTest {

    @Mock DiscogsApiClient apiClient;
    @Mock DiscogsRateLimiter rateLimiter;
    @Mock DiscogsCacheService cacheService;

    private DiscogsMusicMetadataAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DiscogsMusicMetadataAdapter(apiClient, rateLimiter, cacheService, new DiscogsMapper());
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

        var searchResult = new DiscogsSearchResult(
                123, "Artist - Title", 2024,
                List.of("Electronic"), List.of("House"),
                "UK", "CAT001", "url"
        );
        when(apiClient.search(anyString(), eq("release"), eq(5)))
                .thenReturn(new DiscogsSearchResponse(
                        new DiscogsPagination(1, 1, 5, 1),
                        List.of(searchResult)
                ));
        when(apiClient.getRelease(123)).thenReturn(new DiscogsRelease(
                123, "Title",
                List.of("Electronic"), List.of("House", "Deep House"),
                "UK", null, 2024,
                List.of(new DiscogsLabel(1, "Defected", "CAT001")),
                List.of()
        ));

        var result = adapter.enrich("Artist", "Title");

        assertThat(result.isSuccess()).isTrue();
        var data = result.data();
        assertThat(data.artist()).isEqualTo("Artist");
        assertThat(data.styles()).containsExactly("House", "Deep House");
        assertThat(data.label()).isEqualTo("Defected");
        verify(cacheService).put(eq("Artist"), eq("Title"), any(EnrichmentResult.class));
    }

    @Test
    void shouldReturnNotFoundWhenNoResults() {
        when(cacheService.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(apiClient.search(anyString(), eq("release"), eq(5)))
                .thenReturn(new DiscogsSearchResponse(
                        new DiscogsPagination(1, 0, 5, 0),
                        List.of()
                ));

        var result = adapter.enrich("Unknown", "Song");

        assertThat(result).isInstanceOf(EnrichmentResult.NotFound.class);
        verify(cacheService).put(eq("Unknown"), eq("Song"), any(EnrichmentResult.class));
    }

    @Test
    void shouldReturnErrorOnApiException() {
        when(cacheService.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(apiClient.search(anyString(), eq("release"), eq(5)))
                .thenThrow(new RuntimeException("Connection refused"));

        var result = adapter.enrich("Artist", "Title");

        assertThat(result).isInstanceOf(EnrichmentResult.Error.class);
        assertThat(((EnrichmentResult.Error) result).message()).contains("Connection refused");
    }
}
