package com.djtools.ayan.musictagger.domain.model;

import java.util.List;
import java.util.stream.Stream;

/** Thèmes et tonalité émotionnelle extraits de l'analyse lyricale. Source-agnostique. */
public record TrackThemes(
        List<String> topics,
        List<String> themes,
        String mood,
        String sentiment
) {
    public TrackThemes {
        topics = topics != null ? List.copyOf(topics) : List.of();
        themes = themes != null ? List.copyOf(themes) : List.of();
    }

    public List<String> allTerms() {
        return Stream.concat(topics.stream(), themes.stream())
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();
    }

    public boolean isEmpty() {
        return topics.isEmpty() && themes.isEmpty()
                && (mood == null || mood.isBlank())
                && (sentiment == null || sentiment.isBlank());
    }
}
