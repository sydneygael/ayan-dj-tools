package com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs;

import com.google.common.util.concurrent.RateLimiter;

@SuppressWarnings("UnstableApiUsage")
public class DiscogsRateLimiter {

    private final RateLimiter rateLimiter;

    public DiscogsRateLimiter(double requestsPerSecond) {
        this.rateLimiter = RateLimiter.create(requestsPerSecond);
    }

    public void acquire() {
        rateLimiter.acquire();
    }
}
