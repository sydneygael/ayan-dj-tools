package com.djtools.ayan.musictagger.domain.usecase;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.SongSearchCriteria;
import com.djtools.ayan.musictagger.domain.model.SongSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchSongsUseCaseTest {

    private final SearchSongsUseCase useCase = new SearchSongsUseCase();

    private EnrichedTrackMetadata track(String id, Double bpm, Double energy, Integer year, List<String> genres) {
        return new EnrichedTrackMetadata(
                id, "Artist " + id, "Title " + id, "Album",
                genres, List.of(), null, null, null, List.of(), year, 50, 200000L,
                new AudioFeatures(0.8, energy, 0.5, null, null, null, bpm, "A", "minor", 4, null, null),
                null, null, null);
    }

    private EnrichedTrackMetadata trackNoFeatures(String id, List<String> genres) {
        return new EnrichedTrackMetadata(
                id, "Artist " + id, "Title " + id, "Album",
                genres, List.of(), null, null, null, List.of(), null, 50, 200000L, null, null, null, null);
    }

    private SongSearchCriteria criteria(String genre, Integer bpmMin, Integer bpmMax, int limit) {
        return new SongSearchCriteria(genre, null, bpmMin, bpmMax, null, null, null, null, limit);
    }

    @Test
    void search_excludesTracksOutOfBpmRange() {
        var candidates = List.of(
                track("inRange", 125.0, 0.7, 2020, List.of("house")),
                track("tooFast", 200.0, 0.7, 2020, List.of("house"))
        );

        SongSearchResult result = useCase.search(candidates, criteria("house", 120, 130, 10));

        var ids = result.matches().stream().map(m -> m.track().sourceId()).toList();
        assertThat(ids).contains("inRange").doesNotContain("tooFast");
    }

    @Test
    void search_excludesGenreMismatchButKeepsUntagged() {
        var candidates = List.of(
                track("techno", 125.0, 0.7, 2020, List.of("techno")),
                trackNoFeatures("untagged", List.of())
        );

        SongSearchResult result = useCase.search(candidates, criteria("house", null, null, 10));

        var ids = result.matches().stream().map(m -> m.track().sourceId()).toList();
        assertThat(ids).contains("untagged").doesNotContain("techno");
    }

    @Test
    void search_ranksConfirmedMatchesAboveBareCandidates() {
        var candidates = List.of(
                trackNoFeatures("bare", List.of()),
                track("strong", 125.0, 0.7, 2020, List.of("house"))
        );

        SongSearchResult result = useCase.search(candidates, criteria("house", 120, 130, 10));

        assertThat(result.matches().getFirst().track().sourceId()).isEqualTo("strong");
        assertThat(result.matches().getFirst().relevance())
                .isGreaterThan(result.matches().getLast().relevance());
    }

    @Test
    void search_lenientOnTracksWithoutAudioFeatures() {
        var candidates = List.of(trackNoFeatures("noFeatures", List.of()));

        SongSearchResult result = useCase.search(candidates, criteria(null, 120, 130, 10));

        assertThat(result.matches()).hasSize(1);
    }

    @Test
    void search_limitsToCriteriaLimitButReportsTotalMatched() {
        var candidates = List.of(
                track("a", 124.0, 0.7, 2020, List.of("house")),
                track("b", 125.0, 0.7, 2020, List.of("house")),
                track("c", 126.0, 0.7, 2020, List.of("house")),
                track("d", 127.0, 0.7, 2020, List.of("house")),
                track("e", 128.0, 0.7, 2020, List.of("house"))
        );

        SongSearchResult result = useCase.search(candidates, criteria("house", 120, 130, 2));

        assertThat(result.matches()).hasSize(2);
        assertThat(result.totalMatched()).isEqualTo(5);
    }

    @Test
    void search_emptyCandidates_returnsEmptyResult() {
        SongSearchResult result = useCase.search(List.of(), criteria("house", 120, 130, 10));

        assertThat(result.matches()).isEmpty();
        assertThat(result.totalMatched()).isZero();
        assertThat(result.criteriaSummary()).isNotBlank();
    }

    @Test
    void search_listsMatchReasons() {
        var candidates = List.of(track("t", 125.0, 0.72, 2019, List.of("Deep House")));

        var c = new SongSearchCriteria("house", null, 120, 130, 0.6, 0.9, 2018, 2020, 10);
        SongSearchResult result = useCase.search(candidates, c);

        var reasons = result.matches().getFirst().reasons();
        assertThat(reasons).anySatisfy(r -> assertThat(r).contains("Genre").contains("Deep House"));
        assertThat(reasons).anySatisfy(r -> assertThat(r).contains("BPM").contains("125"));
        assertThat(reasons).anySatisfy(r -> assertThat(r).contains("Énergie").contains("0.72"));
        assertThat(reasons).anySatisfy(r -> assertThat(r).contains("Année").contains("2019"));
    }

    @Test
    void search_buildsReadableCriteriaSummary() {
        var c = new SongSearchCriteria("house", "festif", 120, 130, 0.6, 0.9, 2018, 2020, 10);

        SongSearchResult result = useCase.search(List.of(), c);

        assertThat(result.criteriaSummary())
                .contains("Genre : house")
                .contains("Ambiance : festif")
                .contains("BPM : 120-130")
                .contains("Énergie : 0.60-0.90")
                .contains("Année : 2018-2020")
                .contains("Limite : 10");
    }

    @Test
    void criteria_normalizesLimit() {
        assertThat(new SongSearchCriteria(null, null, null, null, null, null, null, null, 0).limit()).isEqualTo(10);
        assertThat(new SongSearchCriteria(null, null, null, null, null, null, null, null, -5).limit()).isEqualTo(10);
        assertThat(new SongSearchCriteria(null, null, null, null, null, null, null, null, 999).limit()).isEqualTo(50);
        assertThat(new SongSearchCriteria(null, null, null, null, null, null, null, null, 7).limit()).isEqualTo(7);
    }
}
