package com.djtools.ayan.musictagger.domain.model;

public record AudioFeatures(
        double danceability,
        double energy,
        double valence,
        double acousticness,
        double instrumentalness,
        double speechiness,
        double bpm,
        String musicalKey,
        String mode,
        int timeSignature
) {

    public AudioFeatures {
        if (bpm < 0) {
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
}
