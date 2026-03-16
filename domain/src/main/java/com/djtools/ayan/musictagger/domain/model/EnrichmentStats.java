package com.djtools.ayan.musictagger.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Statistiques d'enrichissement : taux de match Spotify, erreurs, tags les plus enrichis. */
public record EnrichmentStats(
        double spotifyMatchRate,
        Map<String, Long> mostEnrichedTagTypes,
        double errorRate,
        Map<String, Long> enrichmentBySource
) {

    public EnrichmentStats {
        mostEnrichedTagTypes = mostEnrichedTagTypes != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(mostEnrichedTagTypes))
                : Map.of();
        enrichmentBySource = enrichmentBySource != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(enrichmentBySource))
                : Map.of();
    }
}
