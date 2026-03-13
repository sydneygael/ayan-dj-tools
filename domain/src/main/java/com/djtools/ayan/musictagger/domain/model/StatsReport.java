package com.djtools.ayan.musictagger.domain.model;

import java.util.List;
import java.util.Map;

/** Rapport de statistiques globales : plans créés, tags appliqués, activité récente. */
public record StatsReport(
        long totalPlansCreated,
        long totalTagsApplied,
        long totalFilesEnriched,
        Map<String, Long> tagsAppliedByType,
        List<TaggingHistoryEntry> recentActivity
) {

    public StatsReport {
        tagsAppliedByType = tagsAppliedByType != null ? Map.copyOf(tagsAppliedByType) : Map.of();
        recentActivity = recentActivity != null ? List.copyOf(recentActivity) : List.of();
    }
}
