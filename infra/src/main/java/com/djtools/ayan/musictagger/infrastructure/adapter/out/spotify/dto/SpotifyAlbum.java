package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto;

import java.util.List;

public record SpotifyAlbum(
        String id,
        String name,
        String release_date,
        List<SpotifyImage> images
) {

    public int releaseYear() {
        if (release_date == null || release_date.length() < 4) {
            return 0;
        }
        try {
            return Integer.parseInt(release_date.substring(0, 4));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
