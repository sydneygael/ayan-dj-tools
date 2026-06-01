package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts;

import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.domain.port.out.EnrichedMetadataCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoundchartsFirstMetadataProvider implements MusicMetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(SoundchartsFirstMetadataProvider.class);

    private final EnrichedMetadataCacheRepository cache;
    private final SoundchartsMusicMetadataAdapter soundcharts;
    private final MusicMetadataProvider            fallbackProvider;

    public SoundchartsFirstMetadataProvider(
            EnrichedMetadataCacheRepository cache,
            SoundchartsMusicMetadataAdapter soundcharts,
            MusicMetadataProvider fallbackProvider
    ) {
        this.cache            = cache;
        this.soundcharts      = soundcharts;
        this.fallbackProvider = fallbackProvider;
    }

    @Override
    public EnrichmentResult enrich(String artist, String title) {

        // ── 1. Persistance Postgres ───────────────────────────────────────────
        final var cached = cache.get(artist, title);
        if (cached.isPresent()) {
            log.info("Cache hit for '{} – {}' — aucun appel API", artist, title);
            return EnrichmentResult.success(cached.get());
        }

        // ── 2. Soundcharts ────────────────────────────────────────────────────
        final var scResult = soundcharts.enrich(artist, title);
        if (scResult.isSuccess()) {
            cache.put(artist, title, scResult.data());
            return scResult;
        }

        log.info("Soundcharts no success for '{} – {}', fallback Spotify", artist, title);

        // ── 3. Spotify (fallback) ─────────────────────────────────────────────
        final var spotifyResult = fallbackProvider.enrich(artist, title);
        if (spotifyResult.isSuccess()) {
            cache.put(artist, title, spotifyResult.data());
        }
        return spotifyResult;
    }
}
