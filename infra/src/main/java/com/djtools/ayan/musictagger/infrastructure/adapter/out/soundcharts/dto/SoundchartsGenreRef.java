package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Genre Soundcharts : un genre racine ({@code root}) et ses sous-genres ({@code sub}). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SoundchartsGenreRef(String root, List<String> sub) {

    public List<String> subSafe() {
        return sub != null ? sub : List.of();
    }
}
