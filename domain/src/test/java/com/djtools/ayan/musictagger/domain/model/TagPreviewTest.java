package com.djtools.ayan.musictagger.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TagPreviewTest {

    @Test
    void shouldHandleNullChanges() {
        var preview = new TagPreview("/a.mp3", null);

        assertThat(preview.changes()).isEmpty();
    }

    @Test
    void shouldCreateWithChanges() {
        var changes = List.of(
                new TagChange("genre", null, "Techno"),
                new TagChange("bpm", "120", "128")
        );
        var preview = new TagPreview("/a.mp3", changes);

        assertThat(preview.changes()).hasSize(2);
        assertThat(preview.changes().getFirst().field()).isEqualTo("genre");
    }

    @Test
    void shouldCreateDefensiveCopy() {
        var change = new TagChange("genre", null, "House");
        var preview = new TagPreview("/a.mp3", List.of(change));

        assertThat(preview.changes()).containsExactly(change);
    }
}
