package com.djtools.ayan.musictagger.domain.model;

import java.util.List;

/**
 * Un morceau retourné par une recherche par critères.
 *
 * @param track     métadonnées enrichies du morceau
 * @param relevance pertinence 0.0–1.0 vis-à-vis des critères demandés
 * @param reasons   raisons lisibles de la correspondance (ex: "Genre : house", "BPM : 124")
 */
public record SongMatch(EnrichedTrackMetadata track, double relevance, List<String> reasons) {

    public SongMatch {
        if (track == null) {
            throw new IllegalArgumentException("Track must not be null");
        }
        reasons = reasons != null ? List.copyOf(reasons) : List.of();
    }
}
