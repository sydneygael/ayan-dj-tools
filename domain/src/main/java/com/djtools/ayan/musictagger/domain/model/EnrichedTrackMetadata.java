package com.djtools.ayan.musictagger.domain.model;

import java.util.List;

public record EnrichedTrackMetadata(
        String sourceId,
        String artist,
        String title,
        String album,
        List<String> genres,
        int releaseYear,
        int popularity,
        long durationMs,
        AudioFeatures audioFeatures
) {

    public EnrichedTrackMetadata {
        genres = genres != null ? List.copyOf(genres) : List.of();
    }
}
