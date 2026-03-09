package com.djtools.ayan.musictagger.domain.model;

public record SimilarTrackResult(EnrichedTrackMetadata track, double similarityScore) {

    public SimilarTrackResult {
        if (track == null) {
            throw new IllegalArgumentException("Track must not be null");
        }
        if (similarityScore < 0 || similarityScore > 1) {
            throw new IllegalArgumentException("Similarity score must be between 0 and 1: " + similarityScore);
        }
    }
}
