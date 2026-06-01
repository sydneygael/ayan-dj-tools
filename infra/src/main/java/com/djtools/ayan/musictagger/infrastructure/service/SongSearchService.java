package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.SimilarTrackResult;
import com.djtools.ayan.musictagger.domain.model.SongSearchCriteria;
import com.djtools.ayan.musictagger.domain.model.SongSearchResult;
import com.djtools.ayan.musictagger.domain.usecase.SearchSongsUseCase;
import org.springframework.stereotype.Service;

/**
 * Recherche de morceaux par critères donnés en dialogue.
 *
 * <p>Construit une requête sémantique à partir des critères, récupère un large pool de candidats via le
 * vector store (RAG), puis délègue le filtrage et le classement par critères au use case domaine pur.
 */
@Service
public class SongSearchService {

    private static final int CANDIDATE_POOL = 200;

    private final TrackVectorizationService trackVectorizationService;
    private final SearchSongsUseCase searchSongsUseCase;

    public SongSearchService(TrackVectorizationService trackVectorizationService,
                             SearchSongsUseCase searchSongsUseCase) {
        this.trackVectorizationService = trackVectorizationService;
        this.searchSongsUseCase = searchSongsUseCase;
    }

    public SongSearchResult search(SongSearchCriteria criteria) {
        final var query = buildQuery(criteria);
        final var candidates = trackVectorizationService.findSimilarTracks(query, CANDIDATE_POOL).stream()
                .map(SimilarTrackResult::track)
                .toList();
        return searchSongsUseCase.search(candidates, criteria);
    }

    /** Construit la requête RAG à partir des critères textuels et numériques disponibles. */
    private String buildQuery(SongSearchCriteria criteria) {
        final var sb = new StringBuilder();
        if (criteria.hasGenre()) sb.append(criteria.genre().trim()).append(' ');
        if (criteria.hasMood()) sb.append(criteria.mood().trim()).append(' ');

        final Integer bpmMid = bpmMid(criteria);
        if (bpmMid != null) sb.append(bpmMid).append(" BPM ");

        final var energyWord = energyWord(criteria);
        if (!energyWord.isEmpty()) sb.append(energyWord).append(' ');

        final var query = sb.toString().trim();
        return query.isEmpty() ? "dj music track" : query;
    }

    private Integer bpmMid(SongSearchCriteria criteria) {
        if (criteria.bpmMin() != null && criteria.bpmMax() != null) {
            return (criteria.bpmMin() + criteria.bpmMax()) / 2;
        }
        if (criteria.bpmMin() != null) return criteria.bpmMin();
        return criteria.bpmMax();
    }

    private String energyWord(SongSearchCriteria criteria) {
        if (!criteria.hasEnergyBound()) return "";
        final double mid;
        if (criteria.energyMin() != null && criteria.energyMax() != null) {
            mid = (criteria.energyMin() + criteria.energyMax()) / 2.0;
        } else {
            mid = criteria.energyMin() != null ? criteria.energyMin() : criteria.energyMax();
        }
        if (mid >= 0.6) return "energetic high energy";
        if (mid <= 0.4) return "chill calm mellow";
        return "";
    }
}
