package com.djtools.ayan.musictagger.domain.model;

/**
 * Caractéristiques audio d'un morceau (données Spotify Audio Features).
 * Tous les champs sont nullable — un fichier local peut n'avoir aucune de ces infos.
 */
public record AudioFeatures(
        Double danceability,
        Double energy,
        Double valence,
        Double acousticness,
        Double instrumentalness,
        Double speechiness,
        Double bpm,
        String musicalKey,
        String mode,
        Integer timeSignature,
        Double liveness,
        Double loudness
) {

    public AudioFeatures {
        if (bpm != null && bpm < 0) {
            throw new IllegalArgumentException("BPM must not be negative: " + bpm);
        }
    }

    /** Tonalité complète (ex: "C# minor"). Vide si pas de clé musicale. */
    public String fullKey() {
        if (musicalKey == null || musicalKey.isBlank()) {
            return "";
        }
        if (mode == null || mode.isBlank()) {
            return musicalKey;
        }
        return musicalKey + " " + mode;
    }

    /** Fusionne avec un autre AudioFeatures : garde les valeurs de this, comble les nulls avec other. */
    public AudioFeatures mergeWith(AudioFeatures other) {
        if (other == null) {
            return this;
        }
        return new AudioFeatures(
                danceability != null ? danceability : other.danceability,
                energy != null ? energy : other.energy,
                valence != null ? valence : other.valence,
                acousticness != null ? acousticness : other.acousticness,
                instrumentalness != null ? instrumentalness : other.instrumentalness,
                speechiness != null ? speechiness : other.speechiness,
                bpm != null ? bpm : other.bpm,
                musicalKey != null ? musicalKey : other.musicalKey,
                mode != null ? mode : other.mode,
                timeSignature != null ? timeSignature : other.timeSignature,
                liveness != null ? liveness : other.liveness,
                loudness != null ? loudness : other.loudness
        );
    }
}
