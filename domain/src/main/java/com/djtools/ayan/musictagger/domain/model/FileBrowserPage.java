package com.djtools.ayan.musictagger.domain.model;

import java.util.List;

/**
 * Page de résultats d'un parcours de dossier.
 * Retournée par le tool browseFiles : liste des fichiers audio et sous-dossiers,
 * avec pagination pour les grands répertoires.
 */
public record FileBrowserPage(
        String directory,
        int page,
        int pageSize,
        int totalEntries,
        int totalPages,
        List<FileEntry> entries
) {}
