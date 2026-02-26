package com.djtools.ayan.musictagger.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudioFeaturesTest {

    @Test
    void shouldRejectNegativeBpm() {
        assertThatThrownBy(() -> new AudioFeatures(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, -1.0, "C", "Major", 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BPM must not be negative");
    }

    @Test
    void shouldAcceptZeroBpm() {
        var features = new AudioFeatures(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.0, "C", "Major", 4);
        assertThat(features.bpm()).isZero();
    }

    @Test
    void shouldFormatFullKey() {
        var features = new AudioFeatures(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 128.0, "C", "Major", 4);
        assertThat(features.fullKey()).isEqualTo("C Major");
    }

    @Test
    void shouldReturnKeyOnlyWhenModeIsNull() {
        var features = new AudioFeatures(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 128.0, "Am", null, 4);
        assertThat(features.fullKey()).isEqualTo("Am");
    }

    @Test
    void shouldReturnEmptyWhenKeyIsNull() {
        var features = new AudioFeatures(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 128.0, null, "Major", 4);
        assertThat(features.fullKey()).isEmpty();
    }

    @Test
    void shouldAllowNullFields() {
        var features = new AudioFeatures(null, 0.7, null, null, null, null, 130.0, "A", "Minor", null);
        assertThat(features.danceability()).isNull();
        assertThat(features.energy()).isEqualTo(0.7);
        assertThat(features.bpm()).isEqualTo(130.0);
        assertThat(features.timeSignature()).isNull();
    }

    @Test
    void shouldAllowAllNullFields() {
        var features = new AudioFeatures(null, null, null, null, null, null, null, null, null, null);
        assertThat(features.bpm()).isNull();
        assertThat(features.fullKey()).isEmpty();
    }

    @Test
    void shouldAcceptNullBpm() {
        var features = new AudioFeatures(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, null, "C", "Major", 4);
        assertThat(features.bpm()).isNull();
    }

    @Test
    void mergeWithShouldPreferThisValues() {
        var local = new AudioFeatures(null, 0.8, null, null, null, null, 128.0, "A", "Minor", null);
        var spotify = new AudioFeatures(0.7, 0.5, 0.6, 0.3, 0.1, 0.05, 126.0, "B", "Major", 4);

        var merged = local.mergeWith(spotify);

        assertThat(merged.danceability()).isEqualTo(0.7);
        assertThat(merged.energy()).isEqualTo(0.8);
        assertThat(merged.valence()).isEqualTo(0.6);
        assertThat(merged.bpm()).isEqualTo(128.0);
        assertThat(merged.musicalKey()).isEqualTo("A");
        assertThat(merged.mode()).isEqualTo("Minor");
        assertThat(merged.timeSignature()).isEqualTo(4);
    }

    @Test
    void mergeWithNullShouldReturnThis() {
        var features = new AudioFeatures(null, 0.8, null, null, null, null, 128.0, "A", "Minor", null);
        assertThat(features.mergeWith(null)).isSameAs(features);
    }
}
