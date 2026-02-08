package com.djtools.ayan.musictagger.infrastructure.adapter.out.audio;

import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudioScannerServiceTest {

    private static Path tempDir;
    private static AudioScannerService service;

    @BeforeAll
    static void setup(@TempDir Path dir) throws Exception {
        tempDir = dir;

        // Root level files
        TestAudioFileHelper.createMp3WithAllTags(dir, "song1.mp3");
        TestAudioFileHelper.createMp3WithMissingTags(dir, "song2.mp3");

        // Nested directory
        Path subDir = dir.resolve("subfolder");
        TestAudioFileHelper.createMp3WithAllTags(subDir, "nested-song.mp3");

        // Non-audio files that should be filtered out
        TestAudioFileHelper.createNonAudioFile(dir, "readme.txt");
        TestAudioFileHelper.createNonAudioFile(dir, "cover.jpg");

        var adapter = new JAudioTaggerAdapter();
        service = new AudioScannerService(adapter, List.of("mp3", "flac", "wav", "aiff", "m4a", "ogg"));
    }

    @Test
    void shouldScanDirectoryRecursively() throws IOException {
        List<MusicFileInfo> results = service.scanDirectory(tempDir);

        assertThat(results).hasSize(3);
    }

    @Test
    void shouldFilterByExtension() throws IOException {
        List<MusicFileInfo> results = service.scanDirectory(tempDir);

        assertThat(results)
                .allMatch(info -> info.filename().endsWith(".mp3"));
    }

    @Test
    void shouldIncludeNestedFiles() throws IOException {
        List<MusicFileInfo> results = service.scanDirectory(tempDir);

        assertThat(results)
                .anyMatch(info -> info.filename().equals("nested-song.mp3"));
    }

    @Test
    void shouldRejectNonDirectory() {
        Path file = tempDir.resolve("song1.mp3");
        assertThatThrownBy(() -> service.scanDirectory(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not a directory");
    }
}
