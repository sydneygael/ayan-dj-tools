package com.djtools.ayan.musictagger.domain.model;

import java.util.Map;

/** Profil de la collection : distributions de genres, BPM, tonalités et features audio moyennes. */
public record CollectionProfile(
        Map<String, Long> genreDistribution,
        Map<String, Long> bpmHistogram,
        Map<String, Long> keyDistribution,
        Map<String, Double> averageAudioFeatures,
        long totalTracksScanned,
        long totalTracksEnriched,
        long totalWithCompleteTags
) {

    public CollectionProfile {
        genreDistribution = genreDistribution != null ? Map.copyOf(genreDistribution) : Map.of();
        bpmHistogram = bpmHistogram != null ? Map.copyOf(bpmHistogram) : Map.of();
        keyDistribution = keyDistribution != null ? Map.copyOf(keyDistribution) : Map.of();
        averageAudioFeatures = averageAudioFeatures != null ? Map.copyOf(averageAudioFeatures) : Map.of();
    }
}
