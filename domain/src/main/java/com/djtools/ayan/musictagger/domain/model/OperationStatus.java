package com.djtools.ayan.musictagger.domain.model;

/** Cycle de vie d'une opération : PENDING → APPROVED/REJECTED → APPLIED/ERROR. */
public enum OperationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    APPLIED,
    ERROR
}
