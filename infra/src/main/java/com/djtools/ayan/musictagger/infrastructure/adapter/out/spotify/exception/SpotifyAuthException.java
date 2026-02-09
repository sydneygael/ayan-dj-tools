package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.exception;

public class SpotifyAuthException extends RuntimeException {

    public SpotifyAuthException(String message) {
        super(message);
    }

    public SpotifyAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
