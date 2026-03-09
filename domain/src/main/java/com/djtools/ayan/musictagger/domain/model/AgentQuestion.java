package com.djtools.ayan.musictagger.domain.model;

import java.util.List;

public record AgentQuestion(
        String questionId,
        String filepath,
        QuestionType type,
        String question,
        List<String> options,
        String context,
        double currentConfidence
) {

    public AgentQuestion {
        options = options != null ? List.copyOf(options) : List.of();
    }

    public enum QuestionType {
        MULTIPLE_CHOICE,
        PREFERENCE,
        CONFIRMATION
    }
}
