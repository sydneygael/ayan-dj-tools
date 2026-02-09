package com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs.dto;

import java.util.List;

public record DiscogsSearchResponse(DiscogsPagination pagination, List<DiscogsSearchResult> results) {}
