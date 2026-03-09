package com.djtools.ayan.musictagger.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmartTagSuggestionTest {

    @Test
    void shouldCreateValidSuggestion() {
        var suggestion = new SmartTagSuggestion(
                "/music/test.mp3",
                Map.of("genre", "Techno"),
                List.of(),
                0.8,
                "spotify+rag"
        );

        assertThat(suggestion.filepath()).isEqualTo("/music/test.mp3");
        assertThat(suggestion.suggestedTags()).containsEntry("genre", "Techno");
        assertThat(suggestion.confidence()).isEqualTo(0.8);
    }

    @Test
    void shouldCreateDefensiveCopies() {
        var tags = new java.util.HashMap<>(Map.of("genre", "Techno"));
        var suggestion = new SmartTagSuggestion("/test.mp3", tags, List.of(), 0.5, "rag");

        assertThatThrownBy(() -> suggestion.suggestedTags().put("bpm", "128"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> suggestion.similarTracks().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectBlankFilepath() {
        assertThatThrownBy(() -> new SmartTagSuggestion("", Map.of(), List.of(), 0.5, "rag"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Filepath must not be blank");
    }

    @Test
    void shouldRejectInvalidConfidence() {
        assertThatThrownBy(() -> new SmartTagSuggestion("/test.mp3", Map.of(), List.of(), -0.1, "rag"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 1");
    }
}
