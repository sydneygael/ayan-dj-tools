package com.djtools.ayan.musictagger.domain.model;

import java.util.List;

/**
 * Résultat d'une recherche de morceaux par critères.
 *
 * @param matches         morceaux retenus, classés par pertinence décroissante (limités à {@code criteria.limit})
 * @param totalMatched    nombre total de morceaux correspondant aux critères avant la limite
 * @param criteriaSummary résumé lisible des critères appliqués (pour que l'agent confirme la recherche)
 */
public record SongSearchResult(List<SongMatch> matches, int totalMatched, String criteriaSummary) {

    public SongSearchResult {
        matches = matches != null ? List.copyOf(matches) : List.of();
        criteriaSummary = criteriaSummary != null ? criteriaSummary : "";
    }
}
