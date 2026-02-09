package com.djtools.ayan.musictagger.domain.port.out;

import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;

public interface MusicMetadataProvider {

    EnrichmentResult enrich(String artist, String title);
}
