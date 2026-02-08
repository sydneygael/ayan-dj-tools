package com.djtools.ayan.musictagger.domain.model.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilepathTest {

    @Test
    void shouldCreateValidFilepath() {
        var filepath = new Filepath("/music/song.mp3");
        assertThat(filepath.value()).isEqualTo("/music/song.mp3");
    }

    @Test
    void shouldExtractFilename() {
        var filepath = new Filepath("/music/artist/song.mp3");
        assertThat(filepath.filename()).isEqualTo("song.mp3");
    }

    @Test
    void shouldRejectNull() {
        assertThatThrownBy(() -> new Filepath(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null or blank");
    }

    @Test
    void shouldRejectBlank() {
        assertThatThrownBy(() -> new Filepath("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null or blank");
    }

    @Test
    void shouldRejectPathTraversal() {
        assertThatThrownBy(() -> new Filepath("/music/../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path traversal");
    }

    @Test
    void shouldRejectPathTraversalAtStart() {
        assertThatThrownBy(() -> new Filepath("../secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path traversal");
    }
}
