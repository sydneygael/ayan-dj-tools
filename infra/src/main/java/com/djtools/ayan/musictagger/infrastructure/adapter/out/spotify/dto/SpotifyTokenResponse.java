package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto;

public record SpotifyTokenResponse(
        String access_token,
        String token_type,
        int expires_in
) {}
