package com.djtools.ayan.musictagger.domain.model;

import java.util.Map;

public record TagOperation(
        String filepath,
        Map<String, String> currentTags,
        Map<String, String> suggestedTags,
        OperationStatus status,
        String message
) {

    public TagOperation {
        currentTags = currentTags != null ? Map.copyOf(currentTags) : Map.of();
        suggestedTags = suggestedTags != null ? Map.copyOf(suggestedTags) : Map.of();
        if (status == null) {
            status = OperationStatus.PENDING;
        }
    }

    public TagOperation withStatus(OperationStatus newStatus) {
        return new TagOperation(filepath, currentTags, suggestedTags, newStatus, message);
    }

    public TagOperation withStatusAndMessage(OperationStatus newStatus, String newMessage) {
        return new TagOperation(filepath, currentTags, suggestedTags, newStatus, newMessage);
    }

    public boolean hasSuggestions() {
        return !suggestedTags.isEmpty();
    }
}
