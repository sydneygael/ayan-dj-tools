package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.SimilarTrackResult;
import com.djtools.ayan.musictagger.domain.model.SongSearchCriteria;
import com.djtools.ayan.musictagger.domain.model.SongSearchResult;
import com.djtools.ayan.musictagger.domain.usecase.SearchSongsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SongSearchServiceTest {

    @Mock TrackVectorizationService trackVectorizationService;

    private SongSearchService service;

    @BeforeEach
    void setUp() {
        // Use case réel : on teste le pipeline service (requête RAG) → use case domaine (filtrage/classement).
        service = new SongSearchService(trackVectorizationService, new SearchSongsUseCase());
    }

    private EnrichedTrackMetadata track(String id, double bpm, List<String> genres) {
        return new EnrichedTrackMetadata(
                id, "Artist " + id, "Title " + id, "Album",
                genres, List.of(), null, null, null, List.of(), 2020, 50, 200000L,
                new AudioFeatures(0.8, 0.7, 0.5, null, null, null, bpm, "A", "minor", 4));
    }

    @Test
    void search_filtersRagCandidatesByCriteria() {
        when(trackVectorizationService.findSimilarTracks(anyString(), anyInt())).thenReturn(List.of(
                new SimilarTrackResult(track("inRange", 125, List.of("house")), 0.9),
                new SimilarTrackResult(track("tooFast", 200, List.of("house")), 0.85)
        ));

        var criteria = new SongSearchCriteria("house", null, 120, 130, null, null, null, null, 10);
        SongSearchResult result = service.search(criteria);

        var ids = result.matches().stream().map(m -> m.track().sourceId()).toList();
        assertThat(ids).contains("inRange").doesNotContain("tooFast");
    }

    @Test
    void search_buildsSemanticQueryFromCriteria() {
        when(trackVectorizationService.findSimilarTracks(anyString(), anyInt())).thenReturn(List.of());

        var criteria = new SongSearchCriteria("house", "festif", 120, 130, 0.7, 0.9, null, null, 10);
        service.search(criteria);

        var queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(trackVectorizationService).findSimilarTracks(queryCaptor.capture(), anyInt());
        var query = queryCaptor.getValue().toLowerCase();
        assertThat(query).contains("house").contains("festif").contains("125").contains("bpm").contains("energetic");
    }

    @Test
    void search_emptyCriteria_usesFallbackQuery() {
        when(trackVectorizationService.findSimilarTracks(anyString(), anyInt())).thenReturn(List.of());

        var criteria = new SongSearchCriteria(null, null, null, null, null, null, null, null, 10);
        service.search(criteria);

        var queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(trackVectorizationService).findSimilarTracks(queryCaptor.capture(), anyInt());
        assertThat(queryCaptor.getValue()).isEqualTo("dj music track");
    }
}
