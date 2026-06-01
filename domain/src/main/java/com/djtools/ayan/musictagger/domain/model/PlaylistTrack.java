package com.djtools.ayan.musictagger.domain.model;

import java.util.Objects;

/**
 * Un morceau positionné dans une playlist harmonique.
 *
 * @param track            métadonnées du morceau
 * @param position         rang dans la playlist (1-based)
 * @param camelotKey       code Camelot du morceau (ex "8A")
 * @param transitionType   type de transition depuis le morceau précédent
 *                         (PERFECT_MATCH / MODE_CHANGE / ADJACENT_KEY / JUMP) ; null pour le premier
 * @param transitionQuality qualité de la transition 0.0–1.0 ; 0.0 pour le premier
 */
public record PlaylistTrack(
        EnrichedTrackMetadata track,
        int position,
        String camelotKey,
        String transitionType,
        double transitionQuality
) {
    public PlaylistTrack {
        Objects.requireNonNull(track, "track must not be null");
        Objects.requireNonNull(camelotKey, "camelotKey must not be null");
    }
}
