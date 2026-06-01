package com.djtools.ayan.musictagger.domain.model;

/**
 * Critères de recherche de morceaux exprimés en dialogue (genre, plage de BPM, énergie, années, ambiance).
 *
 * <p>Tous les champs sont optionnels (null = critère ignoré), sauf {@code limit} qui est normalisé :
 * une valeur ≤ 0 retombe sur {@link #DEFAULT_LIMIT} et le résultat est plafonné à {@link #MAX_LIMIT}.
 */
public record SongSearchCriteria(
        String genre,
        String mood,
        Integer bpmMin,
        Integer bpmMax,
        Double energyMin,
        Double energyMax,
        Integer yearMin,
        Integer yearMax,
        int limit
) {

    public static final int DEFAULT_LIMIT = 10;
    public static final int MAX_LIMIT = 50;

    public SongSearchCriteria {
        if (limit <= 0) {
            limit = DEFAULT_LIMIT;
        } else if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
    }

    public boolean hasGenre() {
        return genre != null && !genre.isBlank();
    }

    public boolean hasMood() {
        return mood != null && !mood.isBlank();
    }

    public boolean hasBpmBound() {
        return bpmMin != null || bpmMax != null;
    }

    public boolean hasEnergyBound() {
        return energyMin != null || energyMax != null;
    }

    public boolean hasYearBound() {
        return yearMin != null || yearMax != null;
    }
}
