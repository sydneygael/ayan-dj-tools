package com.djtools.ayan.musictagger.domain.port.out;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.SimilarTrackResult;

import java.util.List;

public interface VectorStorePort {

    void store(EnrichedTrackMetadata track);

    List<SimilarTrackResult> findSimilar(String query, int limit);
}
