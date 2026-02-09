package com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs.dto;

import java.util.List;

public record DiscogsRelease(
        long id,
        String title,
        List<String> genres,
        List<String> styles,
        String country,
        String notes,
        Integer year,
        List<DiscogsLabel> labels,
        List<DiscogsTrack> tracklist
) {}
