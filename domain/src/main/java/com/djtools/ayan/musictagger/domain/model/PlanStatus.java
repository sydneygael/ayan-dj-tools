package com.djtools.ayan.musictagger.domain.model;

/** Cycle de vie d'un plan : DRAFT → READY_FOR_REVIEW → APPROVED → APPLYING → COMPLETED. */
public enum PlanStatus {
    DRAFT,
    READY_FOR_REVIEW,
    APPROVED,
    APPLYING,
    COMPLETED
}
