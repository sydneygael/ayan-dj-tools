package com.djtools.ayan.musictagger.domain.model;

/** Réponse de l'utilisateur à une AgentQuestion. applyToSimilar = appliquer aux fichiers similaires. */
public record AgentQuestionResponse(
        String questionId,
        String selectedOption,
        boolean applyToSimilar
) {}
