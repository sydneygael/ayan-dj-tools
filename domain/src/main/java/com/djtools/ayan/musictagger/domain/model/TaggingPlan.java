package com.djtools.ayan.musictagger.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record TaggingPlan(
        String planId,
        List<TagOperation> operations,
        LocalDateTime createdAt,
        PlanStatus status,
        int totalFiles,
        int filesWithMissingTags,
        OperatingMode mode,
        int currentIndex
) {

    public TaggingPlan {
        operations = operations != null ? List.copyOf(operations) : List.of();
        if (status == null) {
            status = PlanStatus.DRAFT;
        }
        if (mode == null) {
            mode = OperatingMode.PLAN;
        }
    }

    /** Backward-compatible constructor (mode=PLAN, currentIndex=0). */
    public TaggingPlan(String planId, List<TagOperation> operations, LocalDateTime createdAt,
                       PlanStatus status, int totalFiles, int filesWithMissingTags) {
        this(planId, operations, createdAt, status, totalFiles, filesWithMissingTags, OperatingMode.PLAN, 0);
    }

    public TaggingPlan withStatus(PlanStatus newStatus) {
        return new TaggingPlan(planId, operations, createdAt, newStatus, totalFiles, filesWithMissingTags, mode, currentIndex);
    }

    public TaggingPlan withCurrentIndex(int newIndex) {
        return new TaggingPlan(planId, operations, createdAt, status, totalFiles, filesWithMissingTags, mode, newIndex);
    }

    public TaggingPlan withMode(OperatingMode newMode) {
        return new TaggingPlan(planId, operations, createdAt, status, totalFiles, filesWithMissingTags, newMode, currentIndex);
    }

    public TaggingPlan withOperations(List<TagOperation> newOperations) {
        return new TaggingPlan(planId, newOperations, createdAt, status, totalFiles, filesWithMissingTags, mode, currentIndex);
    }

    public int pendingCount() {
        return (int) operations.stream()
                .filter(op -> op.status() == OperationStatus.PENDING)
                .count();
    }

    public int approvedCount() {
        return (int) operations.stream()
                .filter(op -> op.status() == OperationStatus.APPROVED)
                .count();
    }
}
