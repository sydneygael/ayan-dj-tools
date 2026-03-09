package com.djtools.ayan.musictagger.domain.model;

import java.util.List;

public record TagPreview(String filepath, List<TagChange> changes) {

    public TagPreview {
        changes = changes != null ? List.copyOf(changes) : List.of();
    }
}
