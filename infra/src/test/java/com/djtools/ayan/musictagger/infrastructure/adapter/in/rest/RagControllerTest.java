package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.SimilarTrackResult;
import com.djtools.ayan.musictagger.infrastructure.service.TrackVectorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagControllerTest {

    @Mock TrackVectorizationService vectorizationService;
    @InjectMocks RagController controller;

    private SimilarTrackResult sampleResult() {
        var track = new EnrichedTrackMetadata(
                "sp123", "Artist", "Title", "Album",
                List.of("Electronic"), List.of(), null, null,
                null, List.of(), 2024, 80, 210000L, null, null, null
        );
        return new SimilarTrackResult(track, 0.85);
    }

    @Test
    void shouldReturnSimilarTracks() {
        when(vectorizationService.findSimilarTracks("electronic dance", 3))
                .thenReturn(List.of(sampleResult()));

        List<SimilarTrackResult> result = controller.findSimilar("electronic dance", 3);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().track().artist()).isEqualTo("Artist");
    }

    @Test
    void shouldReturnEmptyList() {
        when(vectorizationService.findSimilarTracks("unknown query", 5))
                .thenReturn(List.of());

        List<SimilarTrackResult> result = controller.findSimilar("unknown query", 5);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldUseDefaultLimit() {
        when(vectorizationService.findSimilarTracks("techno", 5))
                .thenReturn(List.of(sampleResult()));

        List<SimilarTrackResult> result = controller.findSimilar("techno", 5);

        assertThat(result).hasSize(1);
    }
}
