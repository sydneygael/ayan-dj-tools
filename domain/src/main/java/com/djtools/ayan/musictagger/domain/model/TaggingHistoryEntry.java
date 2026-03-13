package com.djtools.ayan.musictagger.domain.model;

import java.time.LocalDateTime;
import java.util.Map;

/** Entrée d'historique : snapshot avant/après d'une écriture de tags sur un fichier. */
public record TaggingHistoryEntry(
        String filepath,
        String planId,
        Map<String, String> oldTags,
        Map<String, String> newTags,
        OperationStatus status,
        String errorMessage,
        LocalDateTime appliedAt
) {

    public TaggingHistoryEntry {
        oldTags = oldTags != null ? Map.copyOf(oldTags) : Map.of();
        newTags = newTags != null ? Map.copyOf(newTags) : Map.of();
    }
}
