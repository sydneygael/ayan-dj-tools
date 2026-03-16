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
        List<TaggingHistoryEntry> all = historyRepository.findAll();

        Set<String> planIds = all.stream()
                .map(TaggingHistoryEntry::planId)
                .collect(Collectors.toSet());
        long totalPlansCreated = planIds.size();

        List<TaggingHistoryEntry> applied = all.stream()
                .filter(e -> e.status() == OperationStatus.APPLIED)
                .toList();

        long totalFilesEnriched = applied.stream()
                .map(TaggingHistoryEntry::filepath)
                .distinct()
                .count();

        long totalTagsApplied = applied.stream()
                .mapToLong(e -> e.newTags().size())
                .sum();

        Map<String, Long> tagsAppliedByType = applied.stream()
                .flatMap(e -> e.newTags().keySet().stream())
                .collect(Collectors.groupingBy(k -> k, Collectors.counting()));

        List<TaggingHistoryEntry> recentActivity = all.stream()
                .filter(e -> e.appliedAt() != null)
                .sorted(Comparator.comparing(TaggingHistoryEntry::appliedAt).reversed())
                .limit(10)
                .toList();

        return new StatsReport(totalPlansCreated, totalTagsApplied, totalFilesEnriched,
                tagsAppliedByType, recentActivity);
    }

    public CollectionProfile computeCollectionProfile() {
        List<TaggingHistoryEntry> all = historyRepository.findAll();
        List<TaggingHistoryEntry> applied = all.stream()
                .filter(e -> e.status() == OperationStatus.APPLIED)
                .toList();

        Map<String, Long> genreDistribution = applied.stream()
                .map(e -> e.newTags().get("genre"))
                .filter(g -> g != null && !g.isBlank())
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()));

        Map<String, Long> bpmHistogram = applied.stream()
                .map(e -> e.newTags().get("bpm"))
                .filter(b -> b != null && !b.isBlank())
                .map(StatsService::toBpmBucket)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(b -> b, TreeMap::new, Collectors.counting()));

        Map<String, Long> keyDistribution = applied.stream()
                .map(e -> e.newTags().get("key"))
                .filter(k -> k != null && !k.isBlank())
                .collect(Collectors.groupingBy(k -> k, Collectors.counting()));

        Map<String, Double> averageAudioFeatures = computeAverageAudioFeatures();

        long totalTracksScanned = all.stream()
                .map(TaggingHistoryEntry::filepath)
                .distinct()
                .count();

        long totalTracksEnriched = applied.stream()
                .map(TaggingHistoryEntry::filepath)
                .distinct()
                .count();

        Set<String> requiredTags = Set.of("artist", "title", "album", "genre", "bpm", "key");
        long totalWithCompleteTags = applied.stream()
                .filter(e -> e.newTags().keySet().containsAll(requiredTags))
                .map(TaggingHistoryEntry::filepath)
                .distinct()
                .count();

        return new CollectionProfile(genreDistribution, bpmHistogram, keyDistribution,
                averageAudioFeatures, totalTracksScanned, totalTracksEnriched, totalWithCompleteTags);
    }

    public EnrichmentStats computeEnrichmentStats() {
        List<TaggingHistoryEntry> all = historyRepository.findAll();
        if (all.isEmpty()) {
            return new EnrichmentStats(0, Map.of(), 0, Map.of());
        }

        List<TaggingHistoryEntry> applied = all.stream()
                .filter(e -> e.status() == OperationStatus.APPLIED)
                .toList();

        List<TaggingHistoryEntry> errors = all.stream()
                .filter(e -> e.status() == OperationStatus.ERROR)
                .toList();

        double spotifyMatchRate = (double) applied.size() / all.size() * 100;
        double errorRate = (double) errors.size() / all.size() * 100;

        Map<String, Long> mostEnrichedTagTypes = applied.stream()
                .flatMap(e -> e.newTags().keySet().stream())
                .collect(Collectors.groupingBy(k -> k, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        Map<String, Long> enrichmentBySource = new LinkedHashMap<>();
        enrichmentBySource.put("spotify", (long) applied.size());

        return new EnrichmentStats(spotifyMatchRate, mostEnrichedTagTypes, errorRate, enrichmentBySource);
    }

    public ActivityTimeline computeActivityTimeline(String period) {
        LocalDate cutoff = switch (period) {
            case "week" -> LocalDate.now().minusDays(7);
            case "month" -> LocalDate.now().minusDays(30);
            default -> LocalDate.MIN;
        };

        List<TaggingHistoryEntry> all = historyRepository.findAll();
        List<TaggingHistoryEntry> filtered = all.stream()
                .filter(e -> e.appliedAt() != null && !e.appliedAt().toLocalDate().isBefore(cutoff))
                .toList();

        Map<String, Long> tagsAppliedPerPeriod = filtered.stream()
                .filter(e -> e.status() == OperationStatus.APPLIED)
                .collect(Collectors.groupingBy(
                        e -> e.appliedAt().toLocalDate().toString(),
                        TreeMap::new,
                        Collectors.counting()));

        List<TaggingPlan> plans = planRepository.findAll();
        List<TaggingPlan> filteredPlans = plans.stream()
                .filter(p -> p.createdAt() != null && !p.createdAt().toLocalDate().isBefore(cutoff))
                .toList();

        Map<String, Long> plansPerPeriod = filteredPlans.stream()
                .collect(Collectors.groupingBy(
                        p -> p.createdAt().toLocalDate().toString(),
                        TreeMap::new,
                        Collectors.counting()));

        Map<String, Long> modeUsage = plans.stream()
                .collect(Collectors.groupingBy(
                        p -> p.mode().name(),
                        Collectors.counting()));

        return new ActivityTimeline(plansPerPeriod, tagsAppliedPerPeriod, modeUsage, Map.of());
    }

    private Map<String, Double> computeAverageAudioFeatures() {
        List<AudioFeatures> allFeatures = audioFeaturesCache.findAll();
        if (allFeatures.isEmpty()) {
            return Map.of();
        }

        Map<String, List<Double>> values = new LinkedHashMap<>();
        for (AudioFeatures f : allFeatures) {
            addIfNonNull(values, "energy", f.energy());
            addIfNonNull(values, "danceability", f.danceability());
            addIfNonNull(values, "valence", f.valence());
            addIfNonNull(values, "acousticness", f.acousticness());
            addIfNonNull(values, "instrumentalness", f.instrumentalness());
        }

        Map<String, Double> averages = new LinkedHashMap<>();
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
