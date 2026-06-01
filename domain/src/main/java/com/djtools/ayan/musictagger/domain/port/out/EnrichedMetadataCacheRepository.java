package com.djtools.ayan.musictagger.domain.port.out;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;

import java.util.Optional;

/** Cache persistant des métadonnées enrichies — évite les appels répétés à Soundcharts/Spotify. */
public interface EnrichedMetadataCacheRepository {
    Optional<EnrichedTrackMetadata> get(String artist, String title);
    void put(String artist, String title, EnrichedTrackMetadata metadata);
}
