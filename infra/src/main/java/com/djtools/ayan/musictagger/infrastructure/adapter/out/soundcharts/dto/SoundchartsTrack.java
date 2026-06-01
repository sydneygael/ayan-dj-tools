package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SoundchartsTrack(
        String uuid,
        String name,
        List<SoundchartsArtistRef> artists,
        @JsonProperty("releaseDate") String releaseDate,   // "YYYY-MM-DD" ou "YYYY"
        List<SoundchartsGenreRef> genres,
        String label,
        String isrc,
        String country,
        Long duration,                                     // durée en ms, nullable
        SoundchartsExternalIds externalIds
) {
    /** Extrait l'année depuis releaseDate. */
    public Integer releaseYear() {
        if (releaseDate == null || releaseDate.length() < 4) return null;
        try { return Integer.parseInt(releaseDate.substring(0, 4)); }
        catch (NumberFormatException e) { return null; }
    }

    public String primaryArtist() {
        if (artists == null || artists.isEmpty()) return null;
        return artists.getFirst().name();
    }
}
