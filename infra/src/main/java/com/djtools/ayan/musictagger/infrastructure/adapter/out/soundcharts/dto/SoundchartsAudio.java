package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Caractéristiques audio Soundcharts (mêmes conventions que Spotify Audio Features).
 * {@code key} : pitch class 0–11 (0=C). {@code mode} : 1=major, 0=minor. {@code tempo} : BPM.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SoundchartsAudio(
        Double acousticness,
        Double danceability,
        Double energy,
        Double instrumentalness,
        Integer key,
        Double liveness,
        Double loudness,
        Integer mode,
        Double speechiness,
        Double tempo,
        Integer timeSignature,
        Double valence
) {}
