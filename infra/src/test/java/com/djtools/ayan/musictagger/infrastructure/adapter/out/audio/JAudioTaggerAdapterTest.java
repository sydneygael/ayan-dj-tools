package com.djtools.ayan.musictagger.infrastructure.adapter.out.audio;

import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JAudioTaggerAdapterTest {

    private static Path tempDir;
    private static Path taggedFile;
    private static Path partialFile;

    private final JAudioTaggerAdapter adapter = new JAudioTaggerAdapter();

    @BeforeAll
    static void createTestFiles(@TempDir Path dir) throws Exception {
        tempDir = dir;
        taggedFile = TestAudioFileHelper.createMp3WithAllTags(dir, "test-with-tags.mp3");
        partialFile = TestAudioFileHelper.createMp3WithMissingTags(dir, "test-missing-tags.mp3");
    }

    @Test
    void shouldReadAllTagsFromCompleteFile() {
        var filepath = new Filepath(taggedFile.toString());
        Optional<MusicFileInfo> result = adapter.readTags(filepath);

        assertThat(result).isPresent();
        MusicFileInfo info = result.get();
        assertThat(info.artist()).isEqualTo("Test Artist");
        assertThat(info.title()).isEqualTo("Test Title");
        assertThat(info.album()).isEqualTo("Test Album");
        assertThat(info.genre()).isEqualTo("Electronic");
        assertThat(info.bpm()).isEqualTo("128");
        assertThat(info.key()).isEqualTo("Am");
        assertThat(info.hasArtistAndTitle()).isTrue();
        assertThat(info.fileSize()).isPositive();
    }

    @Test
    void shouldReadPartialTags() {
        var filepath = new Filepath(partialFile.toString());
        Optional<MusicFileInfo> result = adapter.readTags(filepath);

        assertThat(result).isPresent();
        MusicFileInfo info = result.get();
        assertThat(info.artist()).isEqualTo("Partial Artist");
        assertThat(info.title()).isEqualTo("Partial Title");
        assertThat(info.album()).isNull();
        assertThat(info.genre()).isNull();
        assertThat(info.bpm()).isNull();
        assertThat(info.key()).isNull();
        assertThat(info.isMissingTag("album")).isTrue();
        assertThat(info.isMissingTag("genre")).isTrue();
        assertThat(info.isMissingTag("bpm")).isTrue();
        assertThat(info.isMissingTag("key")).isTrue();
    }

    @Test
    void shouldReturnEmptyForNonExistentFile() {
        var filepath = new Filepath(tempDir.resolve("nonexistent.mp3").toString());
        Optional<MusicFileInfo> result = adapter.readTags(filepath);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldExtractFilename() {
        var filepath = new Filepath(taggedFile.toString());
        Optional<MusicFileInfo> result = adapter.readTags(filepath);

        assertThat(result).isPresent();
        assertThat(result.get().filename()).isEqualTo("test-with-tags.mp3");
    }
}
