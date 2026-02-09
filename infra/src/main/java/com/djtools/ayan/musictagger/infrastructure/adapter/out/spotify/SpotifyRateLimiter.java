package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify;

import com.google.common.util.concurrent.RateLimiter;

@SuppressWarnings("UnstableApiUsage")
public class SpotifyRateLimiter {

    private final RateLimiter rateLimiter;

    public SpotifyRateLimiter(double requestsPerSecond) {
        this.rateLimiter = RateLimiter.create(requestsPerSecond);
    }

    public void acquire() {
        rateLimiter.acquire();
    }
}
