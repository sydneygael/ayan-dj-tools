package com.djtools.ayan.musictagger.domain.model;

/**
 * Résultat d'un enrichissement de métadonnées (sealed interface).
 * Trois cas : Success (données trouvées), NotFound (aucun résultat), Error (erreur technique).
 * Permet le pattern matching exhaustif dans les switch/if.
 */
public sealed interface EnrichmentResult {

    static EnrichmentResult success(EnrichedTrackMetadata metadata) {
        return new Success(metadata);
    }

    static EnrichmentResult notFound() {
        return new NotFound();
    }

    static EnrichmentResult error(String message) {
        return new Error(message);
    }

    default boolean isSuccess() {
        return this instanceof Success;
    }

    /** Extrait les données — uniquement valide sur Success, sinon IllegalStateException. */
    default EnrichedTrackMetadata data() {
        if (this instanceof Success(EnrichedTrackMetadata metadata)) {
            return metadata;
        }
        throw new IllegalStateException("No data available for " + getClass().getSimpleName());
    }

    record Success(EnrichedTrackMetadata metadata) implements EnrichmentResult {}
    record NotFound() implements EnrichmentResult {}
    record Error(String message) implements EnrichmentResult {}
}
