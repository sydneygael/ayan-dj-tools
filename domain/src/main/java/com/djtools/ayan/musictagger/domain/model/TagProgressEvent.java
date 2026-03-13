package com.djtools.ayan.musictagger.domain.model;

/** Événement de progression émis pendant l'exécution d'un plan (index/total par fichier). */
public record TagProgressEvent(
        String planId,
        int index,
        int total,
        String filepath,
        OperationStatus status,
        String message
) {}
