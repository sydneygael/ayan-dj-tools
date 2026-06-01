package com.djtools.ayan.musictagger.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Playlist mixée harmoniquement via la roue de Camelot (« Mixed In Key »).
 * Chaque transition reste dans une tonalité compatible et privilégie un écart de ±6 BPM.
 */
public record HarmonicPlaylist(
        String playlistId,
        String name,
        List<PlaylistTrack> tracks,
        PlaylistStats stats,
        Instant createdAt
) {
    public HarmonicPlaylist {
        Objects.requireNonNull(playlistId, "playlistId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        tracks = tracks != null ? List.copyOf(tracks) : List.of();
        Objects.requireNonNull(stats, "stats must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
