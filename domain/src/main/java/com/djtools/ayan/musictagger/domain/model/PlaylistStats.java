package com.djtools.ayan.musictagger.domain.model;

import java.util.Map;

/**
 * Statistiques agrégées d'une playlist harmonique.
 *
 * @param totalTracks           nombre de morceaux
 * @param avgBpm                BPM moyen
 * @param avgEnergy             énergie moyenne (0–1)
 * @param avgTransitionQuality  qualité moyenne des transitions (0–1)
 * @param keyDistribution       répartition des clés Camelot (code → nombre de morceaux)
 * @param perfectTransitions    nombre de transitions PERFECT_MATCH
 */
public record PlaylistStats(
        int totalTracks,
        double avgBpm,
        double avgEnergy,
        double avgTransitionQuality,
        Map<String, Long> keyDistribution,
        long perfectTransitions
) {
    public PlaylistStats {
        keyDistribution = keyDistribution != null ? Map.copyOf(keyDistribution) : Map.of();
    }

    /** Compatibilité harmonique en pourcentage (qualité moyenne × 100). */
    public double harmonicCompatibility() {
        return avgTransitionQuality * 100;
    }
}
