package com.djtools.ayan.musictagger.domain.model;

import java.time.Duration;
import java.util.List;

public record BatchApplyResult(
        String planId,
        int totalOperations,
        int successCount,
        int errorCount,
        List<TagWriteResult> results,
        Duration duration
) {

    public BatchApplyResult {
        results = results != null ? List.copyOf(results) : List.of();
    }
}
