package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.in.AudioFileReader;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.domain.port.out.VectorStorePort;
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
class TrackVectorizationServiceTest {

    @Mock VectorStorePort vectorStorePort;
    @Mock AudioFileReader audioFileReader;
    @Mock MusicMetadataProvider musicMetadataProvider;

    private TrackVectorizationService service;

    @BeforeEach
    void setUp() {
        service = new TrackVectorizationService(vectorStorePort, audioFileReader, musicMetadataProvider);
    }

    private EnrichedTrackMetadata sampleTrack() {
        return new EnrichedTrackMetadata(
                "sp123", "Daft Punk", "Around The World", "Homework",
                List.of("Electronic"), List.of(), "Virgin", "FR",
                "ISRC123", List.of(), 1997, 82, 420000, null
        );
    }

    @Test
    void findSimilarTracks_delegatesToPort() {
        var expected = List.of(new SimilarTrackResult(sampleTrack(), 0.9));
        when(vectorStorePort.findSimilar("electronic", 5)).thenReturn(expected);

        List<SimilarTrackResult> results = service.findSimilarTracks("electronic", 5);

        assertThat(results).hasSize(1);
        verify(vectorStorePort).findSimilar("electronic", 5);
    }

    @Test
    void store_shouldIgnoreErrors() {
        doThrow(new RuntimeException("Connection refused")).when(vectorStorePort).store(any());

        service.store(sampleTrack());

        verify(vectorStorePort).store(any());
    }

    @Test
    void smartSuggestTags_combinesSpotifyAndRag() {
        var fileInfo = new MusicFileInfo(new Filepath("/test.mp3"), "test.mp3", "Daft Punk", "Around The World",
                null, null, null, null, 0, 0);
        when(audioFileReader.readTags(any())).thenReturn(Optional.of(fileInfo));

        var metadata = sampleTrack();
        when(musicMetadataProvider.enrich("Daft Punk", "Around The World"))
                .thenReturn(EnrichmentResult.success(metadata));
        when(vectorStorePort.findSimilar(anyString(), eq(5)))
                .thenReturn(List.of(new SimilarTrackResult(sampleTrack(), 0.9)));

        SmartTagSuggestion suggestion = service.smartSuggestTags("/test.mp3");

        assertThat(suggestion.filepath()).isEqualTo("/test.mp3");
        assertThat(suggestion.source()).isEqualTo("spotify+rag");
        assertThat(suggestion.confidence()).isGreaterThan(0.7);
        assertThat(suggestion.suggestedTags()).containsKey("genre");
    }

    @Test
    void smartSuggestTags_emptyWhenNoArtistOrTitle() {
        var fileInfo = new MusicFileInfo(new Filepath("/test.mp3"), "test.mp3", null, null,
                null, null, null, null, 0, 0);
        when(audioFileReader.readTags(any())).thenReturn(Optional.of(fileInfo));

        SmartTagSuggestion suggestion = service.smartSuggestTags("/test.mp3");

        assertThat(suggestion.source()).isEqualTo("none");
        assertThat(suggestion.confidence()).isEqualTo(0.0);
    }

    @Test
    void smartSuggestTags_fallsBackToRagOnly() {
        var fileInfo = new MusicFileInfo(new Filepath("/test.mp3"), "test.mp3", "Unknown", "Track",
                null, null, null, null, 0, 0);
        when(audioFileReader.readTags(any())).thenReturn(Optional.of(fileInfo));
        when(musicMetadataProvider.enrich("Unknown", "Track")).thenReturn(EnrichmentResult.notFound());
        when(vectorStorePort.findSimilar(anyString(), eq(5)))
                .thenReturn(List.of(new SimilarTrackResult(sampleTrack(), 0.8)));

        SmartTagSuggestion suggestion = service.smartSuggestTags("/test.mp3");

        assertThat(suggestion.source()).isEqualTo("rag");
        assertThat(suggestion.suggestedTags()).containsKey("genre");
    }
}
