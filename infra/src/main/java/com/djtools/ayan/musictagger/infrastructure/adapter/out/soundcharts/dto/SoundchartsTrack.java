package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SoundchartsTrack(
        String uuid,
        String name,
        @JsonProperty("creditName") String creditName,            // artiste « crédité » (présent dans la recherche)
        List<SoundchartsArtistRef> artists,
        @JsonProperty("releaseDate") String releaseDate,          // ISO "YYYY-MM-DDThh:mm:ss+00:00" ou "YYYY"
        List<SoundchartsGenreRef> genres,
        List<SoundchartsLabel> labels,
        SoundchartsIsrc isrc,
        Long duration,                                            // durée en SECONDES, nullable
        SoundchartsAudio audio,
        SoundchartsExternalIds externalIds,
        @JsonProperty("languageCode") String languageCode,
        boolean explicit
) {
    /** Extrait l'année depuis releaseDate. */
    public Integer releaseYear() {
        if (releaseDate == null || releaseDate.length() < 4) return null;
        try { return Integer.parseInt(releaseDate.substring(0, 4)); }
        catch (NumberFormatException e) { return null; }
    }

    /** Artiste principal : premier de {@code artists}, sinon {@code creditName}. */
    public String primaryArtist() {
        if (artists != null && !artists.isEmpty() && artists.getFirst().name() != null) {
            return artists.getFirst().name();
        }
        return creditName;
    }

    /** Valeur ISRC brute (sans le pays). */
    public String isrcValue() {
        return isrc != null ? isrc.value() : null;
    }

    /** Code pays ISO (ex: "GB") déduit de l'ISRC. */
    public String countryCode() {
        return isrc != null ? isrc.countryCode() : null;
    }

    /** Premier label, ou null. */
    public String primaryLabel() {
        return labels != null && !labels.isEmpty() ? labels.getFirst().name() : null;
    }

    /** Durée convertie en millisecondes (Soundcharts renvoie des secondes). */
    public Long durationMs() {
        return duration != null ? duration * 1000 : null;
    }
}
