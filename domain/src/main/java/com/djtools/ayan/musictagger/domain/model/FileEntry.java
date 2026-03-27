package com.djtools.ayan.musictagger.domain.model;

/**
 * Entrée dans un dossier : fichier audio (avec tags) ou sous-dossier.
 * Utilisé par le tool browseFiles de l'agent pour naviguer dans les fichiers.
 */
public record FileEntry(
        String name,
        String absolutePath,
        boolean isDirectory,
        long fileSizeBytes,
        String artist,
        String title,
        String album,
        String genre,
        boolean hasCompleteTags
) {}
