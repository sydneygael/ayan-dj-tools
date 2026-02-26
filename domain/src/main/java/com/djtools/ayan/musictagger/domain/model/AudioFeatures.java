package com.djtools.ayan.musictagger.domain.model;

public record AudioFeatures(
        Double danceability,
        Double energy,
        Double valence,
        Double acousticness,
        Double instrumentalness,
        Double speechiness,
        Double bpm,
        String musicalKey,
        String mode,
        Integer timeSignature
) {

    public AudioFeatures {
        if (bpm != null && bpm < 0) {
            throw new IllegalArgumentException("BPM must not be negative: " + bpm);
        }
    }

    public String fullKey() {
        if (musicalKey == null || musicalKey.isBlank()) {
            return "";
        }
        if (mode == null || mode.isBlank()) {
            return musicalKey;
        }
        return musicalKey + " " + mode;
    }

    public AudioFeatures mergeWith(AudioFeatures other) {
        if (other == null) {
            return this;
        }
        return new AudioFeatures(
                danceability != null ? danceability : other.danceability,
                energy != null ? energy : other.energy,
                valence != null ? valence : other.valence,
                acousticness != null ? acousticness : other.acousticness,
                instrumentalness != null ? instrumentalness : other.instrumentalness,
                speechiness != null ? speechiness : other.speechiness,
                bpm != null ? bpm : other.bpm,
                musicalKey != null ? musicalKey : other.musicalKey,
                mode != null ? mode : other.mode,
                timeSignature != null ? timeSignature : other.timeSignature
        );
    }
}
