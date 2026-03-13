package com.djtools.ayan.musictagger.domain.model;

/** Changement unitaire d'un tag : ancien → nouveau pour un champ donné. */
public record TagChange(String field, String oldValue, String newValue) {}
