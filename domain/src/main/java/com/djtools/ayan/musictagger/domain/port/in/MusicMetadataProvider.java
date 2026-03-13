package com.djtools.ayan.musictagger.domain.port.in;

import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;

/** Port entrant : enrichissement de métadonnées via une source externe (Spotify). */
public interface MusicMetadataProvider {

    /** Recherche et enrichit un morceau par artiste + titre. */
    EnrichmentResult enrich(String artist, String title);
}
