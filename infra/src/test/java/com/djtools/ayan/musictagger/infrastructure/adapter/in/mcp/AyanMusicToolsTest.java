package com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.in.AudioFeatureExtractor;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.domain.usecase.ScanMusicUseCase;
import com.djtools.ayan.musictagger.infrastructure.service.PlanManagementService;
import com.djtools.ayan.musictagger.infrastructure.service.TrackVectorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AyanMusicToolsTest {

    @Mock ScanMusicUseCase scanMusicUseCase;
    @Mock MusicMetadataProvider musicMetadataProvider;
    @Mock AudioFeatureExtractor audioFeatureExtractor;
    @Mock PlanManagementService planManagementService;
    @Mock com.djtools.ayan.musictagger.infrastructure.service.ManualModeService manualModeService;
    @Mock TrackVectorizationService vectorizationService;

    private AyanMusicTools tools;

    @BeforeEach
    void setUp() {
        tools = new AyanMusicTools(scanMusicUseCase, musicMetadataProvider, audioFeatureExtractor, planManagementService, manualModeService, vectorizationService);
    }

    @Test
    void scanMusicFile_returnsFileInfo() {
        var filepath = new Filepath("C:/music/test.mp3");
        var info = new MusicFileInfo(filepath, "test.mp3", "Artist", "Title", "Album", "Genre", "120", "Am", 1000, 123456);
        when(scanMusicUseCase.execute(any())).thenReturn(List.of(info));

        MusicFileInfo result = tools.scanMusicFile("C:/music/test.mp3");

        assertThat(result.artist()).isEqualTo("Artist");
        assertThat(result.title()).isEqualTo("Title");
    }

    @Test
    void scanMusicFile_throwsWhenFileNotFound() {
        when(scanMusicUseCase.execute(any())).thenReturn(List.of());

        assertThatThrownBy(() -> tools.scanMusicFile("C:/music/missing.mp3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Impossible de lire");
    }

    @Test
    void detectMissingTags_delegatesToUseCase() {
        var report = new MissingTagsReport(new Filepath("C:/music/test.mp3"), List.of("bpm", "key"));
        when(scanMusicUseCase.detectMissingTags(any())).thenReturn(report);

        MissingTagsReport result = tools.detectMissingTags("C:/music/test.mp3");

        assertThat(result.missingTags()).containsExactly("bpm", "key");
    }

    @Test
    void suggestTags_artistDashTitle() {
        TagSuggestion result = tools.suggestTagsFromFilename("Daft Punk - Around The World.mp3");

        assertThat(result.artist()).isEqualTo("Daft Punk");
        assertThat(result.title()).isEqualTo("Around The World");
    }

    @Test
    void suggestTags_emDash() {
        TagSuggestion result = tools.suggestTagsFromFilename("Bicep — Glue.flac");

        assertThat(result.artist()).isEqualTo("Bicep");
        assertThat(result.title()).isEqualTo("Glue");
    }

    @Test
    void suggestTags_enDash() {
        TagSuggestion result = tools.suggestTagsFromFilename("Bonobo – Kerala.wav");

        assertThat(result.artist()).isEqualTo("Bonobo");
        assertThat(result.title()).isEqualTo("Kerala");
    }

    @Test
    void suggestTags_noSeparator_returnsTitleOnly() {
        TagSuggestion result = tools.suggestTagsFromFilename("unknown_track.mp3");

        assertThat(result.artist()).isNull();
        assertThat(result.title()).isEqualTo("unknown_track");
    }

    @Test
    void suggestTags_noExtension() {
        TagSuggestion result = tools.suggestTagsFromFilename("just_a_name");

        assertThat(result.artist()).isNull();
        assertThat(result.title()).isEqualTo("just_a_name");
    }

    @Test
    void enrichWithSpotify_delegatesToProvider() {
        var metadata = new EnrichedTrackMetadata(
                "sp123", "Artist", "Title", "Album",
                List.of("Electronic"), List.of(), "Label", "FR",
                "ISRC123", List.of(), 2024, 80, 210000, null
        );
        when(musicMetadataProvider.enrich("Artist", "Title"))
                .thenReturn(EnrichmentResult.success(metadata));

        EnrichmentResult result = tools.enrichWithSpotify("Artist", "Title");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data().artist()).isEqualTo("Artist");
    }

    @Test
    void createPlanForFiles_delegatesToService() {
        var plan = new TaggingPlan("plan-1",
                List.of(new TagOperation("/a.mp3", Map.of(), Map.of("genre", "Techno"), OperationStatus.PENDING, null)),
                LocalDateTime.now(), PlanStatus.READY_FOR_REVIEW, 1, 1);
        when(planManagementService.createPlan(any())).thenReturn(plan);

        TaggingPlan result = tools.createPlanForFiles(List.of("/a.mp3"));

        assertThat(result.planId()).isEqualTo("plan-1");
        assertThat(result.operations()).hasSize(1);
    }

    @Test
    void applyTagsPlan_delegatesToService() {
        var batchResult = new BatchApplyResult("plan-1", 1, 1, 0,
                List.of(new TagWriteResult("/a.mp3", OperationStatus.APPLIED, null)), Duration.ofMillis(50));
        when(planManagementService.executePlan("plan-1")).thenReturn(batchResult);

        BatchApplyResult result = tools.applyTagsPlan("plan-1");

        assertThat(result.successCount()).isEqualTo(1);
    }

    @Test
    void previewTagUpdate_delegatesToService() {
        var preview = new TagPreview("/a.mp3", List.of(new TagChange("genre", null, "Techno")));
        when(planManagementService.previewFile("/a.mp3", Map.of("genre", "Techno"))).thenReturn(preview);

        TagPreview result = tools.previewTagUpdate("/a.mp3", Map.of("genre", "Techno"));

        assertThat(result.changes()).hasSize(1);
    }

    @Test
    void getTaggingHistory_delegatesToService() {
        var entry = new TaggingHistoryEntry("/a.mp3", "plan-1", Map.of(), Map.of("genre", "Techno"),
                OperationStatus.APPLIED, null, LocalDateTime.now());
        when(planManagementService.getPlanHistory("plan-1")).thenReturn(List.of(entry));

        List<TaggingHistoryEntry> result = tools.getTaggingHistory("plan-1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().filepath()).isEqualTo("/a.mp3");
    }

    @Test
    void enrichWithSpotify_triggersVectorization() {
        var metadata = new EnrichedTrackMetadata(
                "sp123", "Artist", "Title", "Album",
                List.of("Electronic"), List.of(), "Label", "FR",
                "ISRC123", List.of(), 2024, 80, 210000, null
        );
        when(musicMetadataProvider.enrich("Artist", "Title"))
                .thenReturn(EnrichmentResult.success(metadata));

        tools.enrichWithSpotify("Artist", "Title");

        verify(vectorizationService).store(metadata);
    }

    @Test
    void findSimilarTracks_delegatesToVectorizationService() {
        var track = new EnrichedTrackMetadata(
                "sp123", "Artist", "Title", "Album",
                List.of("Electronic"), List.of(), null, null,
                null, List.of(), 2024, 80, 210000, null
        );
        when(vectorizationService.findSimilarTracks("electronic", 3))
                .thenReturn(List.of(new SimilarTrackResult(track, 0.9)));

        List<SimilarTrackResult> results = tools.findSimilarTracks("electronic", 3);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().track().artist()).isEqualTo("Artist");
    }

    @Test
    void smartSuggestTags_delegatesToVectorizationService() {
        var suggestion = new SmartTagSuggestion("/test.mp3", Map.of("genre", "Techno"), List.of(), 0.8, "spotify+rag");
        when(vectorizationService.smartSuggestTags("/test.mp3")).thenReturn(suggestion);

        SmartTagSuggestion result = tools.smartSuggestTags("/test.mp3");

        assertThat(result.filepath()).isEqualTo("/test.mp3");
        assertThat(result.source()).isEqualTo("spotify+rag");
    }
}
