package com.djtools.ayan.musictagger.domain.port.out;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.SimilarTrackResult;

import java.util.List;

/** Port sortant : stockage vectoriel (Qdrant) pour la recherche par similarité sémantique. */
public interface VectorStorePort {

    /** Vectorise et stocke les métadonnées d'un morceau enrichi. */
    void store(EnrichedTrackMetadata track);

    /** Recherche les morceaux les plus similaires à la requête textuelle. */
    List<SimilarTrackResult> findSimilar(String query, int limit);
}
