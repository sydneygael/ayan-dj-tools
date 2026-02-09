package com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz;

import com.google.common.util.concurrent.RateLimiter;

@SuppressWarnings("UnstableApiUsage")
public class MusicBrainzRateLimiter {

    private final RateLimiter rateLimiter;

    public MusicBrainzRateLimiter(double requestsPerSecond) {
        this.rateLimiter = RateLimiter.create(requestsPerSecond);
    }

    public void acquire() {
        rateLimiter.acquire();
    }
}
