package com.djtools.ayan.musictagger.domain.model.vo;

import java.nio.file.Path;

/**
 * Value Object représentant un chemin de fichier validé.
 * Rejette les chemins vides et les tentatives de path traversal ("..").
 */
public record Filepath(String value) {

    public Filepath {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Filepath must not be null or blank");
        }
        if (value.contains("..")) {
            throw new IllegalArgumentException("Path traversal is not allowed: " + value);
        }
    }

    /** Extrait le nom de fichier (dernier composant du chemin). */
    public String filename() {
        return Path.of(value).getFileName().toString();
    }
}
