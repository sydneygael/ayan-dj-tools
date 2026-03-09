package com.djtools.ayan.musictagger.domain.model;

public record TagProgressEvent(
        String planId,
        int index,
        int total,
        String filepath,
        OperationStatus status,
        String message
) {}
