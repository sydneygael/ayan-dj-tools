package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.port.out.AudioFileWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagControllerTest {

    @Mock AudioFileWriter audioFileWriter;
    @InjectMocks TagController controller;

    @Test
    void shouldApplyTags() {
        when(audioFileWriter.writeTags("/a.mp3", Map.of("genre", "Techno")))
                .thenReturn(new TagWriteResult("/a.mp3", OperationStatus.APPLIED, null));

        var result = controller.applyTags(new TagController.ApplyTagsRequest("/a.mp3", Map.of("genre", "Techno")));

        assertThat(result.status()).isEqualTo(OperationStatus.APPLIED);
    }

    @Test
    void shouldPreviewTags() {
        var preview = new TagPreview("/a.mp3", List.of(new TagChange("genre", null, "Techno")));
        when(audioFileWriter.previewChanges("/a.mp3", Map.of("genre", "Techno"))).thenReturn(preview);

        var result = controller.previewTags(new TagController.ApplyTagsRequest("/a.mp3", Map.of("genre", "Techno")));

        assertThat(result.changes()).hasSize(1);
    }

    @Test
    void shouldReturnErrorResult() {
        when(audioFileWriter.writeTags("/a.mp3", Map.of("genre", "Techno")))
                .thenReturn(new TagWriteResult("/a.mp3", OperationStatus.ERROR, "File not found"));

        var result = controller.applyTags(new TagController.ApplyTagsRequest("/a.mp3", Map.of("genre", "Techno")));

        assertThat(result.status()).isEqualTo(OperationStatus.ERROR);
        assertThat(result.message()).isEqualTo("File not found");
    }

    @Test
    void shouldPreviewEmptyChanges() {
        var preview = new TagPreview("/a.mp3", List.of());
        when(audioFileWriter.previewChanges("/a.mp3", Map.of("artist", "Same"))).thenReturn(preview);

        var result = controller.previewTags(new TagController.ApplyTagsRequest("/a.mp3", Map.of("artist", "Same")));

        assertThat(result.changes()).isEmpty();
    }

    @Test
    void shouldPreviewMultipleChanges() {
        var changes = List.of(new TagChange("genre", null, "Techno"), new TagChange("bpm", "120", "128"));
        var preview = new TagPreview("/a.mp3", changes);
        when(audioFileWriter.previewChanges("/a.mp3", Map.of("genre", "Techno", "bpm", "128"))).thenReturn(preview);

        var result = controller.previewTags(new TagController.ApplyTagsRequest("/a.mp3", Map.of("genre", "Techno", "bpm", "128")));

        assertThat(result.changes()).hasSize(2);
    }
}
