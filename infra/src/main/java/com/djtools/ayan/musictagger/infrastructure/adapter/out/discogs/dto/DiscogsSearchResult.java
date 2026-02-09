package com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs.dto;

import java.util.List;

public record DiscogsSearchResult(
        long id,
        String title,
        Integer year,
        List<String> genre,
        List<String> style,
        String country,
        String catno,
        String resource_url
) {}
