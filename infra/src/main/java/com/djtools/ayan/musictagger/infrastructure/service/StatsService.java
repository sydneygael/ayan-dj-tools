package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.port.out.AudioFeaturesCacheRepository;
import com.djtools.ayan.musictagger.domain.port.out.PlanRepository;
import com.djtools.ayan.musictagger.domain.port.out.TaggingHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final TaggingHistoryRepository historyRepository;
    private final PlanRepository planRepository;
    private final AudioFeaturesCacheRepository audioFeaturesCache;

    public StatsService(TaggingHistoryRepository historyRepository,
                        PlanRepository planRepository,
                        AudioFeaturesCacheRepository audioFeaturesCache) {
        this.historyRepository = historyRepository;
        this.planRepository = planRepository;
        this.audioFeaturesCache = audioFeaturesCache;
    }

    public StatsReport computeStats() {
        final var all = historyRepository.findAll();

        final var planIds = all.stream()
                .map(TaggingHistoryEntry::planId)
                .collect(Collectors.toSet());
        final var totalPlansCreated = planIds.size();

        final var applied = all.stream()
                .filter(e -> e.status() == OperationStatus.APPLIED)
                .toList();

        final var totalFilesEnriched = applied.stream()
                .map(TaggingHistoryEntry::filepath)
                .distinct()
                .count();

        final var totalTagsApplied = applied.stream()
                .mapToLong(e -> e.newTags().size())
                .sum();

        final var tagsAppliedByType = applied.stream()
                .flatMap(e -> e.newTags().keySet().stream())
                .collect(Collectors.groupingBy(k -> k, Collectors.counting()));

        final var recentActivity = all.stream()
                .filter(e -> e.appliedAt() != null)
                .sorted(Comparator.comparing(TaggingHistoryEntry::appliedAt).reversed())
                .limit(10)
                .toList();

        return new StatsReport(totalPlansCreated, totalTagsApplied, totalFilesEnriched,
                tagsAppliedByType, recentActivity);
    }

    public CollectionProfile computeCollectionProfile() {
        final var all = historyRepository.findAll();
        final var applied = all.stream()
                .filter(e -> e.status() == OperationStatus.APPLIED)
                .toList();

        final var genreDistribution = applied.stream()
                .map(e -> e.newTags().get("genre"))
                .filter(g -> g != null && !g.isBlank())
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()));

        final var bpmHistogram = applied.stream()
                .map(e -> e.newTags().get("bpm"))
                .filter(b -> b != null && !b.isBlank())
                .map(StatsService::toBpmBucket)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(b -> b, TreeMap::new, Collectors.counting()));

        final var keyDistribution = applied.stream()
                .map(e -> e.newTags().get("key"))
                .filter(k -> k != null && !k.isBlank())
                .collect(Collectors.groupingBy(k -> k, Collectors.counting()));

        final var averageAudioFeatures = computeAverageAudioFeatures();

        final var totalTracksScanned = all.stream()
                .map(TaggingHistoryEntry::filepath)
                .distinct()
                .count();

        final var totalTracksEnriched = applied.stream()
                .map(TaggingHistoryEntry::filepath)
                .distinct()
                .count();

        final var requiredTags = Set.of("artist", "title", "album", "genre", "bpm", "key");
        final var totalWithCompleteTags = applied.stream()
                .filter(e -> e.newTags().keySet().containsAll(requiredTags))
                .map(TaggingHistoryEntry::filepath)
                .distinct()
                .count();

        return new CollectionProfile(genreDistribution, bpmHistogram, keyDistribution,
                averageAudioFeatures, totalTracksScanned, totalTracksEnriched, totalWithCompleteTags);
    }

    public EnrichmentStats computeEnrichmentStats() {
        final var all = historyRepository.findAll();
        if (all.isEmpty()) {
            return new EnrichmentStats(0, Map.of(), 0, Map.of());
        }

        final var applied = all.stream()
                .filter(e -> e.status() == OperationStatus.APPLIED)
                .toList();

        final var errors = all.stream()
                .filter(e -> e.status() == OperationStatus.ERROR)
                .toList();

        final var spotifyMatchRate = (double) applied.size() / all.size() * 100;
        final var errorRate = (double) errors.size() / all.size() * 100;

        final var mostEnrichedTagTypes = applied.stream()
                .flatMap(e -> e.newTags().keySet().stream())
                .collect(Collectors.groupingBy(k -> k, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        final var enrichmentBySource = new LinkedHashMap<String, Long>();
        enrichmentBySource.put("spotify", (long) applied.size());

        return new EnrichmentStats(spotifyMatchRate, mostEnrichedTagTypes, errorRate, enrichmentBySource);
    }

    public ActivityTimeline computeActivityTimeline(String period) {
        LocalDate cutoff = switch (period) {
            case "week" -> LocalDate.now().minusDays(7);
            case "month" -> LocalDate.now().minusDays(30);
            default -> LocalDate.MIN;
        };

        final var all = historyRepository.findAll();
        final var filtered = all.stream()
                .filter(e -> e.appliedAt() != null && !e.appliedAt().toLocalDate().isBefore(cutoff))
                .toList();

        final var tagsAppliedPerPeriod = filtered.stream()
                .filter(e -> e.status() == OperationStatus.APPLIED)
                .collect(Collectors.groupingBy(
                        e -> e.appliedAt().toLocalDate().toString(),
                        TreeMap::new,
                        Collectors.counting()));

        final var plans = planRepository.findAll();
        final var filteredPlans = plans.stream()
                .filter(p -> p.createdAt() != null && !p.createdAt().toLocalDate().isBefore(cutoff))
                .toList();

        final var plansPerPeriod = filteredPlans.stream()
                .collect(Collectors.groupingBy(
                        p -> p.createdAt().toLocalDate().toString(),
                        TreeMap::new,
                        Collectors.counting()));

        final var modeUsage = plans.stream()
                .collect(Collectors.groupingBy(
                        p -> p.mode().name(),
                        Collectors.counting()));

        return new ActivityTimeline(plansPerPeriod, tagsAppliedPerPeriod, modeUsage, Map.of());
    }

    private Map<String, Double> computeAverageAudioFeatures() {
        final var allFeatures = audioFeaturesCache.findAll();
        if (allFeatures.isEmpty()) {
            return Map.of();
        }

        final var values = new LinkedHashMap<String, List<Double>>();
        for (AudioFeatures f : allFeatures) {
            addIfNonNull(values, "energy", f.energy());
            addIfNonNull(values, "danceability", f.danceability());
            addIfNonNull(values, "valence", f.valence());
            addIfNonNull(values, "acousticness", f.acousticness());
            addIfNonNull(values, "instrumentalness", f.instrumentalness());
        }

        final var averages = new LinkedHashMap<String, Double>();
        values.forEach((key, vals) -> {
            if (!vals.isEmpty()) {
                double avg = vals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                averages.put(key, Math.round(avg * 100.0) / 100.0);
            }
        });
        return averages;
    }

    private static void addIfNonNull(Map<String, List<Double>> map, String key, Double value) {
        if (value != null) {
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
    }

    static String toBpmBucket(String bpmStr) {
        try {
            double bpm = Double.parseDouble(bpmStr);
            int base = ((int) bpm / 5) * 5;
            return base + "-" + (base + 5);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
