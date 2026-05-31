package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto;

import java.util.List;

public record SpotifyArtist(
        String id,
        String name,
        List<String> genres,
        Integer popularity,
        SpotifyFollowers followers
) {}
