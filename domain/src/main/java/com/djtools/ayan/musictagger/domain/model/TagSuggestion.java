package com.djtools.ayan.musictagger.domain.model;

/** Suggestion artiste/titre extraite du nom de fichier. Champs nullable si non devinable. */
public record TagSuggestion(String artist, String title) {}
