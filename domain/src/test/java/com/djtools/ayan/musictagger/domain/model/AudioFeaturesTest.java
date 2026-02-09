package com.djtools.ayan.musictagger.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudioFeaturesTest {

    @Test
    void shouldRejectNegativeBpm() {
        assertThatThrownBy(() -> new AudioFeatures(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, -1, "C", "Major", 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BPM must not be negative");
    }

    @Test
    void shouldAcceptZeroBpm() {
        var features = new AudioFeatures(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0, "C", "Major", 4);
        assertThat(features.bpm()).isZero();
    }

    @Test
    void shouldFormatFullKey() {
        var features = new AudioFeatures(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 128, "C", "Major", 4);
        assertThat(features.fullKey()).isEqualTo("C Major");
    }

    @Test
    void shouldReturnKeyOnlyWhenModeIsNull() {
        var features = new AudioFeatures(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 128, "Am", null, 4);
        assertThat(features.fullKey()).isEqualTo("Am");
    }

    @Test
    void shouldReturnEmptyWhenKeyIsNull() {
        var features = new AudioFeatures(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 128, null, "Major", 4);
        assertThat(features.fullKey()).isEmpty();
    }
}
