package com.djtools.ayan.musictagger.domain.model;

import java.util.List;

/** Métadonnées enrichies d'un morceau (source externe (Soundcharts, Spotify)). Copie défensive des listes. */
public record EnrichedTrackMetadata(
        String sourceId,
        String artist,
        String title,
        String album,
        List<String> genres,
        List<String> styles,
        String label,
        String country,
        String isrc,
        List<String> tags,
        Integer releaseYear,
        Integer popularity,
        Long durationMs,
        AudioFeatures audioFeatures,
        String languageCode,
        Boolean explicit
) {

    public EnrichedTrackMetadata {
        genres = genres != null ? List.copyOf(genres) : List.of();
        styles = styles != null ? List.copyOf(styles) : List.of();
        tags = tags != null ? List.copyOf(tags) : List.of();
    }
}
