package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;

import java.util.List;

/**
 * Résultat d'une recherche musicale externe (Soundcharts / web / Spotify).
 *
 * @param found      true si au moins une source a retourné un résultat
 * @param source     source ayant fourni le résultat ("soundcharts", "spotify", "web", "none")
 * @param query      terme de recherche original
 * @param tracks     morceaux trouvés (liste vide si source="web" ou "none")
 * @param webSummary résumé textuel DuckDuckGo, peut être null
 */
public record MusicLookupResult(
        boolean found,
        String source,
        String query,
        List<EnrichedTrackMetadata> tracks,
        String webSummary
) {
    public MusicLookupResult {
        tracks = tracks != null ? List.copyOf(tracks) : List.of();
    }
}
