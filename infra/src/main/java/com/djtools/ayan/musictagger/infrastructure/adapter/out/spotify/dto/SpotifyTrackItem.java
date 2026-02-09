package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto;

import java.util.List;

public record SpotifyTrackItem(
        String id,
        String name,
        List<SpotifyArtistItem> artists,
        SpotifyAlbum album,
        long duration_ms,
        int popularity
) {

    public String primaryArtist() {
        if (artists == null || artists.isEmpty()) {
            return "";
        }
        return artists.getFirst().name();
    }

    public String primaryArtistId() {
        if (artists == null || artists.isEmpty()) {
            return null;
        }
        return artists.getFirst().id();
    }
}
