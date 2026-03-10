package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.OperationStatus;
import com.djtools.ayan.musictagger.domain.model.StatsReport;
import com.djtools.ayan.musictagger.domain.model.TaggingHistoryEntry;
import com.djtools.ayan.musictagger.domain.port.out.TaggingHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final TaggingHistoryRepository historyRepository;

    public StatsService(TaggingHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
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
}
