package com.djtools.ayan.musictagger.domain.model;

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
