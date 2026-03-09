package com.djtools.ayan.musictagger.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TagOperationTest {

    @Test
    void shouldCreateWithDefaultPendingStatus() {
        var op = new TagOperation("/file.mp3", Map.of("artist", "Daft Punk"), Map.of("genre", "Electronic"), null, null);

        assertThat(op.status()).isEqualTo(OperationStatus.PENDING);
    }

    @Test
    void shouldHandleNullMaps() {
        var op = new TagOperation("/file.mp3", null, null, OperationStatus.PENDING, null);

        assertThat(op.currentTags()).isEmpty();
        assertThat(op.suggestedTags()).isEmpty();
    }

    @Test
    void shouldChangeStatus() {
        var op = new TagOperation("/file.mp3", Map.of(), Map.of("bpm", "128"), OperationStatus.PENDING, null);

        var approved = op.withStatus(OperationStatus.APPROVED);

        assertThat(approved.status()).isEqualTo(OperationStatus.APPROVED);
        assertThat(approved.filepath()).isEqualTo("/file.mp3");
        assertThat(approved.suggestedTags()).containsEntry("bpm", "128");
    }

    @Test
    void shouldChangeStatusAndMessage() {
        var op = new TagOperation("/file.mp3", Map.of(), Map.of("bpm", "128"), OperationStatus.APPROVED, null);

        var applied = op.withStatusAndMessage(OperationStatus.APPLIED, "Tags écrits avec succès");

        assertThat(applied.status()).isEqualTo(OperationStatus.APPLIED);
        assertThat(applied.message()).isEqualTo("Tags écrits avec succès");
        assertThat(applied.filepath()).isEqualTo("/file.mp3");
    }

    @Test
    void shouldDetectHasSuggestions() {
        var withSuggestions = new TagOperation("/a.mp3", Map.of(), Map.of("genre", "Techno"), OperationStatus.PENDING, null);
        var withoutSuggestions = new TagOperation("/b.mp3", Map.of(), Map.of(), OperationStatus.PENDING, null);

        assertThat(withSuggestions.hasSuggestions()).isTrue();
        assertThat(withoutSuggestions.hasSuggestions()).isFalse();
    }
}
