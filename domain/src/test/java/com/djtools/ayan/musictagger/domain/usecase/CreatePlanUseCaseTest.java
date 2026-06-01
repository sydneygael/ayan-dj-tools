package com.djtools.ayan.musictagger.domain.usecase;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.in.AudioFileReader;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePlanUseCaseTest {

    @Mock
    private AudioFileReader audioFileReader;

    @Mock
    private MusicMetadataProvider metadataProvider;

    private CreatePlanUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreatePlanUseCase(audioFileReader, metadataProvider);
    }

    @Test
    void shouldCreatePlanWithEnrichedSuggestions() {
        var path = new Filepath("/music/Daft Punk - Around The World.mp3");
        var fileInfo = new MusicFileInfo(path, "Daft Punk - Around The World.mp3",
                null, null, null, null, null, null, 1000, 0, false);

        when(audioFileReader.readTags(path)).thenReturn(Optional.of(fileInfo));

        var metadata = new EnrichedTrackMetadata(
                "sp-123", "Daft Punk", "Around The World", "Homework",
                List.of("Electronic", "House"), List.of(), "Virgin", "FR",
                "ISRC123", List.of(), 1997, 80, 420000L,
                new AudioFeatures(0.8, 0.9, 0.7, 0.1, 0.5, 0.1, 128.0, "A", "minor", 4)
        );
        when(metadataProvider.enrich("Daft Punk", "Around The World"))
                .thenReturn(EnrichmentResult.success(metadata));

        TaggingPlan plan = useCase.execute("plan-1", List.of(path));

        assertThat(plan.planId()).isEqualTo("plan-1");
        assertThat(plan.status()).isEqualTo(PlanStatus.READY_FOR_REVIEW);
        assertThat(plan.totalFiles()).isEqualTo(1);
        assertThat(plan.filesWithMissingTags()).isEqualTo(1);
        assertThat(plan.operations()).hasSize(1);

        TagOperation op = plan.operations().getFirst();
        assertThat(op.suggestedTags()).containsEntry("artist", "Daft Punk");
        assertThat(op.suggestedTags()).containsEntry("title", "Around The World");
        assertThat(op.suggestedTags()).containsEntry("album", "Homework");
        assertThat(op.suggestedTags()).containsEntry("genre", "Electronic, House");
        assertThat(op.suggestedTags()).containsEntry("bpm", "128");
        assertThat(op.suggestedTags()).containsEntry("key", "A minor");
    }

    @Test
    void shouldCreatePlanForFileWithExistingTags() {
        var path = new Filepath("/music/track.mp3");
        var fileInfo = new MusicFileInfo(path, "track.mp3",
                "Artist", "Title", "Album", "Techno", "130", "Cm", 1000, 0, false);

        when(audioFileReader.readTags(path)).thenReturn(Optional.of(fileInfo));
        when(metadataProvider.enrich("Artist", "Title")).thenReturn(EnrichmentResult.success(
                new EnrichedTrackMetadata("sp-1", "Artist", "Title", "Album",
                        List.of("Techno"), List.of(), null, null, null, List.of(), 2020, 50, 300000L, null)
        ));

        TaggingPlan plan = useCase.execute("plan-2", List.of(path));

        assertThat(plan.filesWithMissingTags()).isEqualTo(0);
        TagOperation op = plan.operations().getFirst();
        assertThat(op.suggestedTags()).isEmpty();
        assertThat(op.currentTags()).containsEntry("artist", "Artist");
    }

    @Test
    void shouldHandleUnreadableFile() {
        var path = new Filepath("/music/corrupt.mp3");
        when(audioFileReader.readTags(path)).thenReturn(Optional.empty());

        TaggingPlan plan = useCase.execute("plan-3", List.of(path));

        assertThat(plan.operations()).isEmpty();
        assertThat(plan.status()).isEqualTo(PlanStatus.DRAFT);
    }

    @Test
    void shouldHandleSpotifyNotFound() {
        var path = new Filepath("/music/Unknown - Track.mp3");
        var fileInfo = new MusicFileInfo(path, "Unknown - Track.mp3",
                null, null, null, null, null, null, 1000, 0, false);

        when(audioFileReader.readTags(path)).thenReturn(Optional.of(fileInfo));
        when(metadataProvider.enrich("Unknown", "Track")).thenReturn(EnrichmentResult.notFound());

        TaggingPlan plan = useCase.execute("plan-4", List.of(path));

        TagOperation op = plan.operations().getFirst();
        assertThat(op.suggestedTags()).containsEntry("artist", "Unknown");
        assertThat(op.suggestedTags()).containsEntry("title", "Track");
        assertThat(op.message()).contains("Aucun résultat Spotify");
    }

    @Test
    void shouldHandleMultipleFiles() {
        var path1 = new Filepath("/music/a.mp3");
        var path2 = new Filepath("/music/b.mp3");
        var info1 = new MusicFileInfo(path1, "a.mp3", "A", "B", null, null, null, null, 100, 0, false);
        var info2 = new MusicFileInfo(path2, "b.mp3", "C", "D", "Album", "Genre", "120", "Am", 200, 0, false);

        when(audioFileReader.readTags(path1)).thenReturn(Optional.of(info1));
        when(audioFileReader.readTags(path2)).thenReturn(Optional.of(info2));
        when(metadataProvider.enrich(anyString(), anyString())).thenReturn(EnrichmentResult.notFound());

        TaggingPlan plan = useCase.execute("plan-5", List.of(path1, path2));

        assertThat(plan.totalFiles()).isEqualTo(2);
        assertThat(plan.operations()).hasSize(2);
    }
}
