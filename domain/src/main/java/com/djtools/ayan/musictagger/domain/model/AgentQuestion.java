package com.djtools.ayan.musictagger.domain.model;

import java.util.List;

/** Question posée par l'agent IA à l'utilisateur pour lever une ambiguïté de tagging. */
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
