package com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.in.AudioFeatureExtractor;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.domain.usecase.ScanMusicUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AyanMusicToolsTest {

    @Mock ScanMusicUseCase scanMusicUseCase;
    @Mock MusicMetadataProvider musicMetadataProvider;
    @Mock AudioFeatureExtractor audioFeatureExtractor;

    private AyanMusicTools tools;

    @BeforeEach
    void setUp() {
        tools = new AyanMusicTools(scanMusicUseCase, musicMetadataProvider, audioFeatureExtractor);
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
}
