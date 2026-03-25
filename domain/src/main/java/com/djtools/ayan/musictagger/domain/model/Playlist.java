package com.djtools.ayan.musictagger.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Playlist(
        String playlistId,
        String name,
        String technique,
        List<EnrichedTrackMetadata> tracks,
        Instant createdAt
) {
    public Playlist {
        Objects.requireNonNull(playlistId, "playlistId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(technique, "technique must not be null");
        tracks = tracks != null ? List.copyOf(tracks) : List.of();
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
