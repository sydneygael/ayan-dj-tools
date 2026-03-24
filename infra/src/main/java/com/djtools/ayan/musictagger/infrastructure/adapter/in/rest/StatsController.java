package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.ActivityTimeline;
import com.djtools.ayan.musictagger.domain.model.CollectionProfile;
import com.djtools.ayan.musictagger.domain.model.EnrichmentStats;
import com.djtools.ayan.musictagger.domain.model.StatsReport;
import com.djtools.ayan.musictagger.infrastructure.service.StatsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Stats", description = "Statistiques de la collection et de l'activité")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public StatsReport getStats() {
        return statsService.computeStats();
    }

    @GetMapping("/collection")
    public CollectionProfile getCollectionProfile() {
        return statsService.computeCollectionProfile();
    }

    @GetMapping("/enrichment")
    public EnrichmentStats getEnrichmentStats() {
        return statsService.computeEnrichmentStats();
    }

    @GetMapping("/activity")
    public ActivityTimeline getActivityTimeline(@RequestParam(defaultValue = "month") String period) {
        return statsService.computeActivityTimeline(period);
    }
}
