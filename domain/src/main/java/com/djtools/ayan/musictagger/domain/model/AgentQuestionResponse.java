package com.djtools.ayan.musictagger.domain.model;

public record AgentQuestionResponse(
        String questionId,
        String selectedOption,
        boolean applyToSimilar
) {}
