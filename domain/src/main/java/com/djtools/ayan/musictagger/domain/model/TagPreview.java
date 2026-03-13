package com.djtools.ayan.musictagger.domain.model;

import java.util.List;

/** Aperçu des modifications de tags prévues pour un fichier (diff avant écriture). */
public record TagPreview(String filepath, List<TagChange> changes) {

    public TagPreview {
        changes = changes != null ? List.copyOf(changes) : List.of();
    }
}
