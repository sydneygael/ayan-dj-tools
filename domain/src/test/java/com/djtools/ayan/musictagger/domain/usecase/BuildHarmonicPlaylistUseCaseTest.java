package com.djtools.ayan.musictagger.domain.usecase;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.HarmonicPlaylist;
import com.djtools.ayan.musictagger.domain.model.PlaylistTrack;
import com.djtools.ayan.musictagger.domain.model.vo.CamelotKey;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BuildHarmonicPlaylistUseCaseTest {

    private final BuildHarmonicPlaylistUseCase useCase = new BuildHarmonicPlaylistUseCase();

    private EnrichedTrackMetadata track(String id, double bpm, double energy, String key, String mode) {
        return new EnrichedTrackMetadata(
                id, "Artist " + id, "Title " + id, "Album",
                List.of(), List.of(), null, null, null, List.of(), 2020, 50, 200000L,
                new AudioFeatures(0.8, energy, 0.5, null, null, null, bpm, key, mode, 4, null, null),
                null, null);
    }

    private CamelotKey parse(String code) {
        int number = Integer.parseInt(code.substring(0, code.length() - 1));
        return new CamelotKey(number, code.charAt(code.length() - 1));
    }

    @Test
    void build_producesOnlyCompatibleTransitions() {
        var candidates = List.of(
                track("t1", 128, 0.7, "A", "minor"),  // 8A
                track("t2", 127, 0.6, "A", "minor"),  // 8A
                track("t3", 126, 0.6, "E", "minor"),  // 9A
                track("t4", 125, 0.6, "C", "major"),  // 8B (relative of 8A)
                track("t5", 124, 0.6, "D", "minor")   // 7A
        );

        HarmonicPlaylist playlist = useCase.build("p1", "Mix", candidates, 5, 0.6);

        assertThat(playlist.tracks()).isNotEmpty();
        // Chaque transition reste dans une tonalité compatible.
        for (int i = 1; i < playlist.tracks().size(); i++) {
            var from = parse(playlist.tracks().get(i - 1).camelotKey());
            var to = parse(playlist.tracks().get(i).camelotKey());
            assertThat(from.isCompatibleWith(to))
                    .as("transition %s -> %s", from.code(), to.code())
                    .isTrue();
        }
    }

    @Test
    void build_dedupsTracks() {
        var candidates = List.of(
                track("t1", 128, 0.7, "A", "minor"),
                track("t2", 127, 0.6, "A", "minor"),
                track("t3", 126, 0.6, "E", "minor")
        );

        HarmonicPlaylist playlist = useCase.build("p1", "Mix", candidates, 10, 0.6);

        var ids = playlist.tracks().stream().map(pt -> pt.track().sourceId()).toList();
        assertThat(ids).doesNotHaveDuplicates();
        assertThat(playlist.tracks()).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void build_prefersTracksWithinSixBpm() {
        // Démarre sur 8A (inséré en premier, tous comptes égaux à 1).
        var candidates = List.of(
                track("start", 128, 0.6, "A", "minor"),     // 8A
                track("close", 132, 0.6, "E", "minor"),      // 9A, +4 BPM (≤6)
                track("far", 160, 0.6, "D", "minor")         // 7A, +32 BPM
        );

        HarmonicPlaylist playlist = useCase.build("p1", "Mix", candidates, 3, 0.6);

        assertThat(playlist.tracks()).hasSize(2);
        assertThat(playlist.tracks().get(0).track().sourceId()).isEqualTo("start");
        assertThat(playlist.tracks().get(1).track().sourceId()).isEqualTo("close");
        assertThat(playlist.tracks().get(1).transitionType()).isEqualTo("ADJACENT_KEY");
    }

    @Test
    void build_bestEffortOnTinyCollection_noException() {
        var candidates = List.of(track("only", 128, 0.6, "A", "minor"));

        HarmonicPlaylist playlist = useCase.build("p1", "Mix", candidates, 50, 0.6);

        assertThat(playlist.tracks()).hasSize(1);
        assertThat(playlist.tracks().getFirst().transitionType()).isNull();
    }

    @Test
    void build_emptyWhenNoCandidates() {
        HarmonicPlaylist playlist = useCase.build("p1", "Mix", List.of(), 10, 0.6);

        assertThat(playlist.tracks()).isEmpty();
        assertThat(playlist.stats().totalTracks()).isZero();
    }

    @Test
    void build_filtersOutTracksWithoutCamelotKey() {
        var candidates = List.of(
                track("nokey1", 128, 0.6, null, "minor"),
                track("nokey2", 128, 0.6, "A", null)
        );

        HarmonicPlaylist playlist = useCase.build("p1", "Mix", candidates, 10, 0.6);

        assertThat(playlist.tracks()).isEmpty();
    }

    @Test
    void build_computesStats() {
        var candidates = List.of(
                track("t1", 128, 0.8, "A", "minor"),  // 8A
                track("t2", 132, 0.6, "A", "minor")   // 8A → PERFECT_MATCH
        );

        HarmonicPlaylist playlist = useCase.build("p1", "Mix", candidates, 2, 0.7);

        assertThat(playlist.tracks()).hasSize(2);
        assertThat(playlist.stats().totalTracks()).isEqualTo(2);
        assertThat(playlist.stats().avgBpm()).isEqualTo(130.0);
        assertThat(playlist.stats().perfectTransitions()).isEqualTo(1);
        assertThat(playlist.stats().keyDistribution()).containsEntry("8A", 2L);
        assertThat(playlist.stats().harmonicCompatibility()).isGreaterThan(0.0);
    }
}
