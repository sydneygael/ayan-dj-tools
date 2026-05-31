package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto;

import java.util.Map;

public record SpotifyAudioFeatures(
        String id,
        Float danceability,
        Float energy,
        Float valence,
        Float acousticness,
        Float instrumentalness,
        Float speechiness,
        Float tempo,
        Integer key,
        Integer mode,
        Integer time_signature
) {

    private static final Map<Integer, String> KEY_MAP = Map.ofEntries(
            Map.entry(0, "C"), Map.entry(1, "C#"), Map.entry(2, "D"),
            Map.entry(3, "D#"), Map.entry(4, "E"), Map.entry(5, "F"),
            Map.entry(6, "F#"), Map.entry(7, "G"), Map.entry(8, "G#"),
            Map.entry(9, "A"), Map.entry(10, "A#"), Map.entry(11, "B")
    );

    public String musicalKey() {
        return KEY_MAP.getOrDefault(key != null ? key : -1, "");
    }

    public String musicalMode() {
        return mode != null && mode == 1 ? "Major" : "Minor";
    }

    public Double bpm() {
        return tempo != null ? (double) tempo : null;
    }
}
