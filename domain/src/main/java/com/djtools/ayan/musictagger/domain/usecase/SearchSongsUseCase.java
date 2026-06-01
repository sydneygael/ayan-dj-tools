package com.djtools.ayan.musictagger.domain.usecase;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.SongMatch;
import com.djtools.ayan.musictagger.domain.model.SongSearchCriteria;
import com.djtools.ayan.musictagger.domain.model.SongSearchResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Filtre et classe une liste de morceaux candidats selon des critères donnés (genre, BPM, énergie, années).
 *
 * <p>Logique pure (zéro dépendance Spring/RAG). Le filtrage est volontairement indulgent : un morceau
 * dont une caractéristique est inconnue (BPM/énergie/année absents, ou aucun genre tagué) n'est pas exclu,
 * mais les morceaux dont les valeurs confirment le critère obtiennent une meilleure pertinence et
 * remontent en tête. L'ordre d'entrée (pertinence sémantique RAG) départage les ex æquo (tri stable).
 */
public class SearchSongsUseCase {

    public SongSearchResult search(List<EnrichedTrackMetadata> candidates, SongSearchCriteria criteria) {
        final List<EnrichedTrackMetadata> pool = candidates == null ? List.of() : candidates;

        final List<SongMatch> ranked = pool.stream()
                .filter(track -> passesFilters(track, criteria))
                .map(track -> new SongMatch(track, relevance(track, criteria), reasons(track, criteria)))
                .sorted(Comparator.comparingDouble(SongMatch::relevance).reversed())
                .toList();

        final List<SongMatch> limited = ranked.stream().limit(criteria.limit()).toList();
        return new SongSearchResult(limited, ranked.size(), summarize(criteria));
    }

    private boolean passesFilters(EnrichedTrackMetadata track, SongSearchCriteria criteria) {
        final AudioFeatures af = track.audioFeatures();

        if (criteria.hasBpmBound() && af != null && af.bpm() != null) {
            if (criteria.bpmMin() != null && af.bpm() < criteria.bpmMin()) return false;
            if (criteria.bpmMax() != null && af.bpm() > criteria.bpmMax()) return false;
        }
        if (criteria.hasEnergyBound() && af != null && af.energy() != null) {
            if (criteria.energyMin() != null && af.energy() < criteria.energyMin()) return false;
            if (criteria.energyMax() != null && af.energy() > criteria.energyMax()) return false;
        }
        if (criteria.hasYearBound() && track.releaseYear() != null && track.releaseYear() > 0) {
            if (criteria.yearMin() != null && track.releaseYear() < criteria.yearMin()) return false;
            if (criteria.yearMax() != null && track.releaseYear() > criteria.yearMax()) return false;
        }
        // Genre : on n'exclut que les morceaux qui ONT des genres tagués mais aucun ne correspond.
        if (criteria.hasGenre()) {
            final List<String> genres = combinedGenres(track);
            if (!genres.isEmpty() && genres.stream().noneMatch(g -> containsIgnoreCase(g, criteria.genre()))) {
                return false;
            }
        }
        return true;
    }

    private double relevance(EnrichedTrackMetadata track, SongSearchCriteria criteria) {
        double score = 0.5;
        if (criteria.hasGenre() && genreMatches(track, criteria)) score += 0.2;
        if (bpmConfirmed(track, criteria)) score += 0.15;
        if (energyConfirmed(track, criteria)) score += 0.1;
        if (yearConfirmed(track, criteria)) score += 0.1;
        return Math.min(1.0, score);
    }

    private List<String> reasons(EnrichedTrackMetadata track, SongSearchCriteria criteria) {
        final List<String> reasons = new ArrayList<>();
        final AudioFeatures af = track.audioFeatures();

        if (criteria.hasGenre() && genreMatches(track, criteria)) {
            combinedGenres(track).stream()
                    .filter(g -> containsIgnoreCase(g, criteria.genre()))
                    .findFirst()
                    .ifPresent(g -> reasons.add("Genre : " + g));
        }
        if (bpmConfirmed(track, criteria)) {
            reasons.add("BPM : " + af.bpm().intValue());
        }
        if (energyConfirmed(track, criteria)) {
            reasons.add("Énergie : " + String.format(Locale.US, "%.2f", af.energy()));
        }
        if (yearConfirmed(track, criteria)) {
            reasons.add("Année : " + track.releaseYear());
        }
        return reasons;
    }

    private boolean genreMatches(EnrichedTrackMetadata track, SongSearchCriteria criteria) {
        return combinedGenres(track).stream().anyMatch(g -> containsIgnoreCase(g, criteria.genre()));
    }

    private boolean bpmConfirmed(EnrichedTrackMetadata track, SongSearchCriteria criteria) {
        final AudioFeatures af = track.audioFeatures();
        if (!criteria.hasBpmBound() || af == null || af.bpm() == null) return false;
        if (criteria.bpmMin() != null && af.bpm() < criteria.bpmMin()) return false;
        return criteria.bpmMax() == null || af.bpm() <= criteria.bpmMax();
    }

    private boolean energyConfirmed(EnrichedTrackMetadata track, SongSearchCriteria criteria) {
        final AudioFeatures af = track.audioFeatures();
        if (!criteria.hasEnergyBound() || af == null || af.energy() == null) return false;
        if (criteria.energyMin() != null && af.energy() < criteria.energyMin()) return false;
        return criteria.energyMax() == null || af.energy() <= criteria.energyMax();
    }

    private boolean yearConfirmed(EnrichedTrackMetadata track, SongSearchCriteria criteria) {
        if (!criteria.hasYearBound() || track.releaseYear() == null || track.releaseYear() <= 0) return false;
        if (criteria.yearMin() != null && track.releaseYear() < criteria.yearMin()) return false;
        return criteria.yearMax() == null || track.releaseYear() <= criteria.yearMax();
    }

    private List<String> combinedGenres(EnrichedTrackMetadata track) {
        return Stream.concat(track.genres().stream(), track.styles().stream()).toList();
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT).trim());
    }

    private String summarize(SongSearchCriteria criteria) {
        final List<String> parts = new ArrayList<>();
        if (criteria.hasGenre()) parts.add("Genre : " + criteria.genre().trim());
        if (criteria.hasMood()) parts.add("Ambiance : " + criteria.mood().trim());
        rangePart(criteria.bpmMin(), criteria.bpmMax()).ifPresent(r -> parts.add("BPM : " + r));
        energyRangePart(criteria.energyMin(), criteria.energyMax()).ifPresent(r -> parts.add("Énergie : " + r));
        rangePart(criteria.yearMin(), criteria.yearMax()).ifPresent(r -> parts.add("Année : " + r));
        parts.add("Limite : " + criteria.limit());
        return String.join("  |  ", parts);
    }

    private java.util.Optional<String> rangePart(Integer min, Integer max) {
        if (min == null && max == null) return java.util.Optional.empty();
        if (min != null && max != null) return java.util.Optional.of(min + "-" + max);
        return java.util.Optional.of(min != null ? "≥ " + min : "≤ " + max);
    }

    private java.util.Optional<String> energyRangePart(Double min, Double max) {
        if (min == null && max == null) return java.util.Optional.empty();
        if (min != null && max != null) return java.util.Optional.of(fmt(min) + "-" + fmt(max));
        return java.util.Optional.of(min != null ? "≥ " + fmt(min) : "≤ " + fmt(max));
    }

    private String fmt(double v) {
        return String.format(Locale.US, "%.2f", v);
    }
}
