package com.djtools.ayan.musictagger.domain.model;

import java.util.List;
import java.util.Map;

public record SmartTagSuggestion(
        String filepath,
        Map<String, String> suggestedTags,
        List<SimilarTrackResult> similarTracks,
        double confidence,
        String source
) {

    public SmartTagSuggestion {
        if (filepath == null || filepath.isBlank()) {
            throw new IllegalArgumentException("Filepath must not be blank");
        }
        suggestedTags = suggestedTags != null ? Map.copyOf(suggestedTags) : Map.of();
        similarTracks = similarTracks != null ? List.copyOf(similarTracks) : List.of();
        if (confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("Confidence must be between 0 and 1: " + confidence);
        }
    }
}
