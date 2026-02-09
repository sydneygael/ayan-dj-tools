package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.exception;

public class SpotifyRateLimitException extends SpotifyApiException {

    public SpotifyRateLimitException(String message) {
        super(message, 429);
    }
}
