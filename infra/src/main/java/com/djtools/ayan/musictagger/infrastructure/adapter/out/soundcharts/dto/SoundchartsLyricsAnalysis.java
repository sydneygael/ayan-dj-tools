package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.stream.Stream;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SoundchartsLyricsAnalysis(
        List<String> topics,
        List<String> themes,
        String mood,
        String sentiment
) {
    public SoundchartsLyricsAnalysis {
        topics = topics != null ? List.copyOf(topics) : List.of();
        themes = themes != null ? List.copyOf(themes) : List.of();
    }

    public List<String> allThemes() {
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
