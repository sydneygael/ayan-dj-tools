package com.djtools.ayan.musictagger.infrastructure.adapter.out.audio;

import com.djtools.ayan.musictagger.domain.model.OperationStatus;
import com.djtools.ayan.musictagger.domain.model.TagChange;
import com.djtools.ayan.musictagger.domain.model.TagPreview;
import com.djtools.ayan.musictagger.domain.model.TagWriteResult;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JAudioTaggerAdapterWriteTest {

    private static Path tempDir;
    private final JAudioTaggerAdapter adapter = new JAudioTaggerAdapter();

    @BeforeAll
    static void initDir(@TempDir Path dir) {
        tempDir = dir;
    }

    @Test
    void shouldWriteAndReadBackTags() throws Exception {
        Path file = TestAudioFileHelper.createMp3WithMissingTags(tempDir, "write-test.mp3");

        TagWriteResult result = adapter.writeTags(file.toString(), Map.of("album", "New Album", "genre", "Techno"));

        assertThat(result.status()).isEqualTo(OperationStatus.APPLIED);
        assertThat(result.message()).isNull();

        var info = adapter.readTags(new Filepath(file.toString())).orElseThrow();
        assertThat(info.album()).isEqualTo("New Album");
        assertThat(info.genre()).isEqualTo("Techno");
        assertThat(info.artist()).isEqualTo("Partial Artist");
    }

    @Test
    void shouldCleanupBackupAfterSuccess() throws Exception {
        Path file = TestAudioFileHelper.createMp3WithMissingTags(tempDir, "backup-cleanup.mp3");

        adapter.writeTags(file.toString(), Map.of("genre", "House"));

        assertThat(Files.exists(file.resolveSibling("backup-cleanup.mp3.bak"))).isFalse();
    }

    @Test
    void shouldReturnErrorForNonExistentFile() {
        TagWriteResult result = adapter.writeTags(tempDir.resolve("nope.mp3").toString(), Map.of("genre", "Techno"));

        assertThat(result.status()).isEqualTo(OperationStatus.ERROR);
        assertThat(result.message()).isNotNull();
    }

    @Test
    void shouldPreviewChanges() throws Exception {
        Path file = TestAudioFileHelper.createMp3WithAllTags(tempDir, "preview-test.mp3");

        TagPreview preview = adapter.previewChanges(file.toString(), Map.of("genre", "Techno", "bpm", "140"));

        assertThat(preview.filepath()).isEqualTo(file.toString());
        assertThat(preview.changes()).extracting(TagChange::field).contains("genre", "bpm");
        assertThat(preview.changes()).anyMatch(c -> c.field().equals("genre") && "Electronic".equals(c.oldValue()) && "Techno".equals(c.newValue()));
        assertThat(preview.changes()).anyMatch(c -> c.field().equals("bpm") && "128".equals(c.oldValue()) && "140".equals(c.newValue()));
    }

    @Test
    void shouldPreviewNewTagsAsNullOldValue() throws Exception {
        Path file = TestAudioFileHelper.createMp3WithMissingTags(tempDir, "preview-new.mp3");

        TagPreview preview = adapter.previewChanges(file.toString(), Map.of("genre", "Techno"));

        assertThat(preview.changes()).hasSize(1);
        assertThat(preview.changes().getFirst().oldValue()).isNull();
        assertThat(preview.changes().getFirst().newValue()).isEqualTo("Techno");
    }
}
