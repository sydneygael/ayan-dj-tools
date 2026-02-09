package com.djtools.ayan.musictagger.domain.model;

import java.util.List;

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
        int releaseYear,
        int popularity,
        long durationMs,
        AudioFeatures audioFeatures
) {

    public EnrichedTrackMetadata {
        genres = genres != null ? List.copyOf(genres) : List.of();
        styles = styles != null ? List.copyOf(styles) : List.of();
        tags = tags != null ? List.copyOf(tags) : List.of();
    }
}
