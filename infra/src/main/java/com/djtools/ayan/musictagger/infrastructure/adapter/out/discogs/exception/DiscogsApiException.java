package com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs.exception;

public class DiscogsApiException extends RuntimeException {

    private final int statusCode;

    public DiscogsApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public DiscogsApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
