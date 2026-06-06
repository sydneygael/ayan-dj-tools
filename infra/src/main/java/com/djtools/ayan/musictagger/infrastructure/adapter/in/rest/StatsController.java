package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.ActivityTimeline;
import com.djtools.ayan.musictagger.domain.model.CollectionProfile;
import com.djtools.ayan.musictagger.domain.model.EnrichmentStats;
import com.djtools.ayan.musictagger.domain.model.StatsReport;
import com.djtools.ayan.musictagger.infrastructure.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(
        summary = "Rapport complet de la collection",
        description = "Agrège toutes les statistiques disponibles : nombre de fichiers, taux d'enrichissement, profil de la collection (genres, BPM, tonalités) et timeline d'activité. Équivalent à appeler /collection + /enrichment + /activity en une seule requête."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rapport complet")
    })
    @GetMapping
    public StatsReport getStats() {
        return statsService.computeStats();
    }

    @Operation(
        summary = "Profil de la collection",
        description = "Distribution des genres, BPM moyen, tonalités dominantes, label principal. Calculé depuis la table `enriched_track_metadata` (PostgreSQL)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profil statistique de la collection")
    })
    @GetMapping("/collection")
    public CollectionProfile getCollectionProfile() {
        return statsService.computeCollectionProfile();
    }

    @Operation(
        summary = "Taux d'enrichissement des métadonnées",
        description = "Pourcentages de tags renseignés (artist, title, album, genre, bpm, key, label, isrc…) sur l'ensemble des fichiers scannés. Utile pour identifier les champs à compléter en priorité."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistiques d'enrichissement par champ")
    })
    @GetMapping("/enrichment")
    public EnrichmentStats getEnrichmentStats() {
        return statsService.computeEnrichmentStats();
    }

    @Operation(
        summary = "Timeline d'activité de tagging",
        description = "Nombre de tags appliqués par jour/semaine/mois selon la période demandée. Données issues de l'historique Redis (`tagging-history:` — TTL 7 jours)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Timeline avec points de données agrégés par période")
    })
    @GetMapping("/activity")
    public ActivityTimeline getActivityTimeline(
            @Parameter(description = "Granularité temporelle : 'day', 'week', 'month' (défaut)")
            @RequestParam(defaultValue = "month") String period) {
        return statsService.computeActivityTimeline(period);
    }
}
