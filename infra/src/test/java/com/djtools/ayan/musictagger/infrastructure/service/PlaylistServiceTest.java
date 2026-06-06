package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.HarmonicPlaylist;
import com.djtools.ayan.musictagger.domain.model.SimilarTrackResult;
import com.djtools.ayan.musictagger.domain.usecase.BuildHarmonicPlaylistUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock TrackVectorizationService trackVectorizationService;

    private PlaylistService service;

    @BeforeEach
    void setUp() {
        // Use case réel : on teste le pipeline complet service → use case domaine.
        service = new PlaylistService(trackVectorizationService, new BuildHarmonicPlaylistUseCase());
    }

    private EnrichedTrackMetadata track(String id, double bpm, String key, String mode) {
        return new EnrichedTrackMetadata(
                id, "Artist " + id, "Title " + id, "Album",
                List.of(), List.of(), null, null, null, List.of(), 2020, 50, 200000L,
                new AudioFeatures(0.8, 0.6, 0.5, null, null, null, bpm, key, mode, 4, null, null),
                null, null, null);
    }

    @Test
    void generateHarmonicPlaylist_buildsCompatibleChainFromRagCandidates() {
        when(trackVectorizationService.findSimilarTracks(anyString(), anyInt())).thenReturn(List.of(
                new SimilarTrackResult(track("t1", 128, "A", "minor"), 0.9),  // 8A
                new SimilarTrackResult(track("t2", 127, "E", "minor"), 0.85), // 9A
                new SimilarTrackResult(track("t3", 126, "C", "major"), 0.8)   // 8B
        ));

        HarmonicPlaylist playlist = service.generateHarmonicPlaylist(120, 130, "house", 0.6, 10);

        assertThat(playlist.name()).contains("house");
        assertThat(playlist.tracks()).isNotEmpty();
        assertThat(playlist.tracks().getFirst().camelotKey()).isNotBlank();
    }

    @Test
    void generateHarmonicPlaylist_filtersOutOfRangeBpm() {
        when(trackVectorizationService.findSimilarTracks(anyString(), anyInt())).thenReturn(List.of(
                new SimilarTrackResult(track("inRange", 125, "A", "minor"), 0.9),
                new SimilarTrackResult(track("tooFast", 200, "E", "minor"), 0.85)
        ));

        HarmonicPlaylist playlist = service.generateHarmonicPlaylist(120, 130, "house", 0.6, 10);

        var ids = playlist.tracks().stream().map(pt -> pt.track().sourceId()).toList();
        assertThat(ids).contains("inRange").doesNotContain("tooFast");
    }
}
