package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.port.out.AudioFeaturesCacheRepository;
import com.djtools.ayan.musictagger.domain.port.out.PlanRepository;
import com.djtools.ayan.musictagger.domain.port.out.TaggingHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock TaggingHistoryRepository historyRepository;
    @Mock PlanRepository planRepository;
    @Mock AudioFeaturesCacheRepository audioFeaturesCache;

    private StatsService service;

    @BeforeEach
    void setUp() {
        service = new StatsService(historyRepository, planRepository, audioFeaturesCache);
    }

    @Test
    void computeStats_multipleEntries() {
        var e1 = new TaggingHistoryEntry("/a.mp3", "plan-1", Map.of(), Map.of("genre", "Techno"),
                OperationStatus.APPLIED, null, LocalDateTime.now());
        var e2 = new TaggingHistoryEntry("/b.mp3", "plan-2", Map.of(), Map.of("genre", "House", "bpm", "128"),
                OperationStatus.APPLIED, null, LocalDateTime.now());
        var e3 = new TaggingHistoryEntry("/c.mp3", "plan-2", Map.of(), Map.of("artist", "DJ X"),
                OperationStatus.ERROR, "Write failed", LocalDateTime.now());

        when(historyRepository.findAll()).thenReturn(List.of(e1, e2, e3));

        StatsReport stats = service.computeStats();

        assertThat(stats.totalPlansCreated()).isEqualTo(2);
        assertThat(stats.totalTagsApplied()).isEqualTo(3);
        assertThat(stats.totalFilesEnriched()).isEqualTo(2);
        assertThat(stats.tagsAppliedByType()).containsEntry("genre", 2L);
        assertThat(stats.tagsAppliedByType()).containsEntry("bpm", 1L);
    }

    @Test
    void computeStats_emptyHistory() {
        when(historyRepository.findAll()).thenReturn(List.of());

        StatsReport stats = service.computeStats();

        assertThat(stats.totalPlansCreated()).isZero();
        assertThat(stats.totalTagsApplied()).isZero();
        assertThat(stats.totalFilesEnriched()).isZero();
        assertThat(stats.tagsAppliedByType()).isEmpty();
        assertThat(stats.recentActivity()).isEmpty();
    }

    @Test
    void computeStats_distinctFilesAndPlans() {
        var e1 = new TaggingHistoryEntry("/a.mp3", "plan-1", Map.of(), Map.of("genre", "Techno"),
                OperationStatus.APPLIED, null, LocalDateTime.now());
        var e2 = new TaggingHistoryEntry("/a.mp3", "plan-2", Map.of(), Map.of("bpm", "128"),
                OperationStatus.APPLIED, null, LocalDateTime.now());

        when(historyRepository.findAll()).thenReturn(List.of(e1, e2));

        StatsReport stats = service.computeStats();

        assertThat(stats.totalFilesEnriched()).isEqualTo(1);
        assertThat(stats.totalPlansCreated()).isEqualTo(2);
    }

    // --- CollectionProfile tests ---

    @Test
    void computeCollectionProfile_genreBpmKeyDistribution() {
        var e1 = entry("/a.mp3", "plan-1", Map.of("genre", "Techno", "bpm", "128", "key", "Am"), OperationStatus.APPLIED);
        var e2 = entry("/b.mp3", "plan-1", Map.of("genre", "Techno", "bpm", "132", "key", "Cm"), OperationStatus.APPLIED);
        var e3 = entry("/c.mp3", "plan-1", Map.of("genre", "House", "bpm", "124", "key", "Am"), OperationStatus.APPLIED);

        when(historyRepository.findAll()).thenReturn(List.of(e1, e2, e3));
        when(audioFeaturesCache.findAll()).thenReturn(List.of());

        CollectionProfile profile = service.computeCollectionProfile();

        assertThat(profile.genreDistribution()).containsEntry("Techno", 2L).containsEntry("House", 1L);
        assertThat(profile.bpmHistogram()).containsEntry("125-130", 1L).containsEntry("130-135", 1L);
        assertThat(profile.keyDistribution()).containsEntry("Am", 2L).containsEntry("Cm", 1L);
        assertThat(profile.totalTracksScanned()).isEqualTo(3);
        assertThat(profile.totalTracksEnriched()).isEqualTo(3);
    }

    @Test
    void computeCollectionProfile_bpmBucketing() {
        assertThat(StatsService.toBpmBucket("120")).isEqualTo("120-125");
        assertThat(StatsService.toBpmBucket("124.5")).isEqualTo("120-125");
        assertThat(StatsService.toBpmBucket("125")).isEqualTo("125-130");
        assertThat(StatsService.toBpmBucket("invalid")).isNull();
    }

    @Test
    void computeCollectionProfile_emptyHistory() {
        when(historyRepository.findAll()).thenReturn(List.of());
        when(audioFeaturesCache.findAll()).thenReturn(List.of());

        CollectionProfile profile = service.computeCollectionProfile();

        assertThat(profile.genreDistribution()).isEmpty();
        assertThat(profile.bpmHistogram()).isEmpty();
        assertThat(profile.totalTracksScanned()).isZero();
    }

    @Test
    void computeCollectionProfile_averageAudioFeatures() {
        when(historyRepository.findAll()).thenReturn(List.of());
        var f1 = new AudioFeatures(0.8, 0.9, 0.7, 0.1, 0.0, null, 128.0, "Am", "minor", 4, null, null);
        var f2 = new AudioFeatures(0.6, 0.7, 0.5, 0.3, 0.2, null, 130.0, "Cm", "minor", 4, null, null);
        when(audioFeaturesCache.findAll()).thenReturn(List.of(f1, f2));

        CollectionProfile profile = service.computeCollectionProfile();

        assertThat(profile.averageAudioFeatures()).containsEntry("energy", 0.8);
        assertThat(profile.averageAudioFeatures()).containsEntry("danceability", 0.7);
        assertThat(profile.averageAudioFeatures()).containsEntry("valence", 0.6);
    }

    @Test
    void computeCollectionProfile_completeTagsCount() {
        var complete = entry("/a.mp3", "plan-1",
                Map.of("artist", "X", "title", "Y", "album", "Z", "genre", "Techno", "bpm", "128", "key", "Am"),
                OperationStatus.APPLIED);
        var incomplete = entry("/b.mp3", "plan-1", Map.of("genre", "House"), OperationStatus.APPLIED);

        when(historyRepository.findAll()).thenReturn(List.of(complete, incomplete));
        when(audioFeaturesCache.findAll()).thenReturn(List.of());

        CollectionProfile profile = service.computeCollectionProfile();

        assertThat(profile.totalWithCompleteTags()).isEqualTo(1);
    }

    // --- EnrichmentStats tests ---

    @Test
    void computeEnrichmentStats_matchRateAndErrorRate() {
        var applied1 = entry("/a.mp3", "plan-1", Map.of("genre", "Techno"), OperationStatus.APPLIED);
        var applied2 = entry("/b.mp3", "plan-1", Map.of("genre", "House", "bpm", "128"), OperationStatus.APPLIED);
        var error = entry("/c.mp3", "plan-1", Map.of(), OperationStatus.ERROR);

        when(historyRepository.findAll()).thenReturn(List.of(applied1, applied2, error));

        EnrichmentStats stats = service.computeEnrichmentStats();

        assertThat(stats.spotifyMatchRate()).isCloseTo(66.67, org.assertj.core.data.Offset.offset(0.1));
        assertThat(stats.errorRate()).isCloseTo(33.33, org.assertj.core.data.Offset.offset(0.1));
        assertThat(stats.mostEnrichedTagTypes()).containsEntry("genre", 2L);
        assertThat(stats.enrichmentBySource()).containsEntry("spotify", 2L);
    }

    @Test
    void computeEnrichmentStats_emptyHistory() {
        when(historyRepository.findAll()).thenReturn(List.of());

        EnrichmentStats stats = service.computeEnrichmentStats();

        assertThat(stats.spotifyMatchRate()).isZero();
        assertThat(stats.errorRate()).isZero();
    }

    @Test
    void computeEnrichmentStats_tagTypesSortedDescending() {
        var e1 = entry("/a.mp3", "plan-1", Map.of("genre", "Techno", "bpm", "128", "artist", "X"), OperationStatus.APPLIED);
        var e2 = entry("/b.mp3", "plan-1", Map.of("genre", "House", "bpm", "130"), OperationStatus.APPLIED);
        var e3 = entry("/c.mp3", "plan-1", Map.of("genre", "Trance"), OperationStatus.APPLIED);

        when(historyRepository.findAll()).thenReturn(List.of(e1, e2, e3));

        EnrichmentStats stats = service.computeEnrichmentStats();

        var keys = stats.mostEnrichedTagTypes().keySet().stream().toList();
        assertThat(keys.getFirst()).isEqualTo("genre");
    }

    // --- ActivityTimeline tests ---

    @Test
    void computeActivityTimeline_weekFilter() {
        var recent = entry("/a.mp3", "plan-1", Map.of("genre", "Techno"), OperationStatus.APPLIED,
                LocalDateTime.now().minusDays(2));
        var old = entry("/b.mp3", "plan-2", Map.of("genre", "House"), OperationStatus.APPLIED,
                LocalDateTime.now().minusDays(10));

        when(historyRepository.findAll()).thenReturn(List.of(recent, old));
        when(planRepository.findAll()).thenReturn(List.of(
                plan("plan-1", OperatingMode.PLAN, LocalDateTime.now().minusDays(2)),
                plan("plan-2", OperatingMode.MANUAL, LocalDateTime.now().minusDays(10))
        ));

        ActivityTimeline timeline = service.computeActivityTimeline("week");

        assertThat(timeline.tagsAppliedPerPeriod()).hasSize(1);
        assertThat(timeline.plansPerPeriod()).hasSize(1);
        assertThat(timeline.modeUsage()).containsEntry("PLAN", 1L).containsEntry("MANUAL", 1L);
    }

    @Test
    void computeActivityTimeline_allPeriod() {
        var e1 = entry("/a.mp3", "plan-1", Map.of("genre", "Techno"), OperationStatus.APPLIED,
                LocalDateTime.now().minusDays(100));

        when(historyRepository.findAll()).thenReturn(List.of(e1));
        when(planRepository.findAll()).thenReturn(List.of(
                plan("plan-1", OperatingMode.APPLY, LocalDateTime.now().minusDays(100))
        ));

        ActivityTimeline timeline = service.computeActivityTimeline("all");

        assertThat(timeline.tagsAppliedPerPeriod()).hasSize(1);
        assertThat(timeline.plansPerPeriod()).hasSize(1);
    }

    @Test
    void computeActivityTimeline_groupsByDate() {
        var today = LocalDateTime.now();
        var e1 = entry("/a.mp3", "plan-1", Map.of("genre", "Techno"), OperationStatus.APPLIED, today);
        var e2 = entry("/b.mp3", "plan-2", Map.of("genre", "House"), OperationStatus.APPLIED, today);

        when(historyRepository.findAll()).thenReturn(List.of(e1, e2));
        when(planRepository.findAll()).thenReturn(List.of());

        ActivityTimeline timeline = service.computeActivityTimeline("month");

        String todayStr = today.toLocalDate().toString();
        assertThat(timeline.tagsAppliedPerPeriod()).containsEntry(todayStr, 2L);
    }

    // --- helpers ---

    private static TaggingHistoryEntry entry(String filepath, String planId,
                                             Map<String, String> newTags, OperationStatus status) {
        return entry(filepath, planId, newTags, status, LocalDateTime.now());
    }

    private static TaggingHistoryEntry entry(String filepath, String planId,
                                             Map<String, String> newTags, OperationStatus status,
                                             LocalDateTime appliedAt) {
        return new TaggingHistoryEntry(filepath, planId, Map.of(), newTags, status, null, appliedAt);
    }

    private static TaggingPlan plan(String planId, OperatingMode mode, LocalDateTime createdAt) {
        return new TaggingPlan(planId, List.of(), createdAt, PlanStatus.COMPLETED, 1, 1, mode, 0);
    }
}
