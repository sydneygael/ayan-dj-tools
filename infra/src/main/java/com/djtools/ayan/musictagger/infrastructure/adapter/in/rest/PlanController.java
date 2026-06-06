package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.infrastructure.service.ApplyModeService;
import com.djtools.ayan.musictagger.infrastructure.service.ManualModeService;
import com.djtools.ayan.musictagger.infrastructure.service.PlanManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/plan")
@Tag(name = "Plan", description = "Gestion des plans de tagging (PLAN / MANUEL / AUTO)")
public class PlanController {

    private final PlanManagementService planManagementService;
    private final ManualModeService manualModeService;
    private final ApplyModeService applyModeService;

    public PlanController(PlanManagementService planManagementService,
                          ManualModeService manualModeService,
                          ApplyModeService applyModeService) {
        this.planManagementService = planManagementService;
        this.manualModeService = manualModeService;
        this.applyModeService = applyModeService;
    }

    @Operation(
        summary = "Créer un plan de tagging",
        description = "Scanne les fichiers fournis, détecte les tags manquants, enrichit les métadonnées via Soundcharts, et construit un `TaggingPlan` avec une `TagOperation` par fichier. Le mode détermine le comportement d'exécution : PLAN (revue batch), MANUAL (confirmation fichier par fichier), APPLY (auto)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Plan créé avec la liste des opérations de tagging")
    })
    @PostMapping("/create")
    public TaggingPlan createPlan(@RequestBody CreatePlanRequest request) {
        final var mode = request.mode() != null ? request.mode() : OperatingMode.PLAN;
        return planManagementService.createPlan(request.filePaths(), mode);
    }

    @Operation(summary = "Récupérer un plan", description = "Retourne le plan stocké dans Redis. Les plans ont un TTL de 48h.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Plan trouvé"),
        @ApiResponse(responseCode = "404", description = "Plan introuvable (expiré ou id inconnu)")
    })
    @GetMapping("/{id}")
    public TaggingPlan getPlan(
            @Parameter(description = "Identifiant UUID du plan") @PathVariable String id) {
        return planManagementService.getPlan(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan introuvable : " + id));
    }

    @Operation(
        summary = "Progression du plan",
        description = "Retourne les compteurs d'opérations par statut : PENDING, APPROVED, APPLIED, REJECTED, ERROR. Utile pour mettre à jour une barre de progression côté frontend."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Compteurs de progression"),
        @ApiResponse(responseCode = "404", description = "Plan introuvable")
    })
    @GetMapping("/{id}/progress")
    public PlanProgressResponse getProgress(
            @Parameter(description = "Identifiant UUID du plan") @PathVariable String id) {
        final var plan = planManagementService.getPlan(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan introuvable : " + id));

        final var total = plan.operations().size();
        final var pending = plan.operations().stream().filter(op -> op.status() == OperationStatus.PENDING).count();
        final var approved = plan.operations().stream().filter(op -> op.status() == OperationStatus.APPROVED).count();
        final var applied = plan.operations().stream().filter(op -> op.status() == OperationStatus.APPLIED).count();
        final var rejected = plan.operations().stream().filter(op -> op.status() == OperationStatus.REJECTED).count();
        final var error = plan.operations().stream().filter(op -> op.status() == OperationStatus.ERROR).count();

        return new PlanProgressResponse(
                plan.planId(),
                plan.status(),
                plan.mode(),
                plan.currentIndex(),
                total,
                pending,
                approved,
                applied,
                rejected,
                error
        );
    }

    @Operation(
        summary = "Approuver toutes les opérations PENDING",
        description = "Passe le statut de toutes les opérations PENDING à APPROVED et le plan à READY_FOR_REVIEW. Pré-requis avant d'appeler /execute."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Plan mis à jour avec opérations approuvées"),
        @ApiResponse(responseCode = "404", description = "Plan introuvable")
    })
    @PutMapping("/{id}/approve")
    public TaggingPlan approvePlan(
            @Parameter(description = "Identifiant UUID du plan") @PathVariable String id) {
        return planManagementService.approvePlan(id);
    }

    @Operation(
        summary = "Exécuter le plan",
        description = "Applique toutes les opérations APPROVED aux fichiers audio. Chaque fichier reçoit un backup avant écriture ; en cas d'erreur le backup est restauré. Retourne un `BatchApplyResult` avec le nombre de succès/erreurs et la durée d'exécution."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rapport d'exécution — opérations appliquées, rejetées, erreurs, durée"),
        @ApiResponse(responseCode = "404", description = "Plan introuvable")
    })
    @PostMapping("/{id}/execute")
    public BatchApplyResult executePlan(
            @Parameter(description = "Identifiant UUID du plan") @PathVariable String id) {
        return planManagementService.executePlan(id);
    }

    @Operation(
        summary = "Prévisualiser les changements de tags",
        description = "Retourne un diff avant/après pour toutes les opérations du plan, sans écrire les fichiers. Chaque `TagPreview` liste les champs modifiés avec valeur courante et valeur proposée."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste de previews par fichier"),
        @ApiResponse(responseCode = "404", description = "Plan introuvable")
    })
    @GetMapping("/{id}/preview")
    public List<TagPreview> previewPlan(
            @Parameter(description = "Identifiant UUID du plan") @PathVariable String id) {
        return planManagementService.previewPlan(id);
    }

    @Operation(
        summary = "Historique d'exécution du plan",
        description = "Retourne les `TaggingHistoryEntry` enregistrés lors de l'exécution du plan — une entrée par fichier traité avec statut, tags écrits et horodatage."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entrées d'historique (vide si plan pas encore exécuté)"),
        @ApiResponse(responseCode = "404", description = "Plan introuvable")
    })
    @GetMapping("/{id}/history")
    public List<TaggingHistoryEntry> getPlanHistory(
            @Parameter(description = "Identifiant UUID du plan") @PathVariable String id) {
        return planManagementService.getPlanHistory(id);
    }

    @Operation(summary = "Supprimer un plan", description = "Supprime le plan de Redis. Opération irréversible — les fichiers déjà taggés ne sont pas affectés.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Plan supprimé")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlan(
            @Parameter(description = "Identifiant UUID du plan") @PathVariable String id) {
        planManagementService.deletePlan(id);
    }

    @Operation(
        summary = "Opération courante (mode MANUEL)",
        description = "Retourne la prochaine `TagOperation` en attente de confirmation dans un plan MANUAL. À appeler après chaque `confirm` pour avancer dans la séquence."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Opération courante avec tags proposés"),
        @ApiResponse(responseCode = "404", description = "Plan introuvable ou toutes les opérations ont été traitées")
    })
    @GetMapping("/{id}/current")
    public TagOperation getCurrentOperation(
            @Parameter(description = "Identifiant UUID du plan") @PathVariable String id) {
        return manualModeService.prepareNextFile(id);
    }

    @Operation(
        summary = "Confirmer ou rejeter une opération (mode MANUEL)",
        description = "Approuve ou rejette l'opération à l'index donné. Si approuvée, les tags sont écrits immédiatement sur le fichier. Retourne l'opération mise à jour avec son nouveau statut (APPLIED ou REJECTED)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Opération mise à jour"),
        @ApiResponse(responseCode = "404", description = "Plan ou opération introuvable")
    })
    @PostMapping("/{id}/operations/{index}/confirm")
    public TagOperation confirmOperation(
            @Parameter(description = "Identifiant UUID du plan") @PathVariable String id,
            @Parameter(description = "Index de l'opération dans la liste (0-based)") @PathVariable int index,
            @Parameter(description = "true = approuver et écrire les tags, false = rejeter") @RequestParam boolean approved) {
        return manualModeService.confirmFile(id, index, approved);
    }

    @Operation(
        summary = "Exécution automatique (mode APPLY)",
        description = "Lance l'exécution asynchrone de toutes les opérations sans confirmation. Retourne HTTP 202 immédiatement. Suivre la progression via GET /{id}/progress ou le topic STOMP `/topic/plan/{id}/progress`."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Exécution lancée en arrière-plan"),
        @ApiResponse(responseCode = "404", description = "Plan introuvable")
    })
    @PostMapping("/{id}/auto-execute")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void autoExecute(
            @Parameter(description = "Identifiant UUID du plan") @PathVariable String id) {
        applyModeService.executeAutomatic(id);
    }

    @Schema(description = "Requête de création d'un plan de tagging")
    public record CreatePlanRequest(
            @Schema(description = "Chemins absolus des fichiers audio à inclure dans le plan")
            List<String> filePaths,
            @Schema(description = "Mode d'opération : PLAN (batch review), MANUAL (fichier par fichier), APPLY (auto). Défaut : PLAN.")
            OperatingMode mode
    ) {}

    @Schema(description = "Compteurs de progression par statut d'opération")
    public record PlanProgressResponse(
            @Schema(description = "Identifiant UUID du plan") String planId,
            @Schema(description = "Statut global du plan") PlanStatus status,
            @Schema(description = "Mode d'opération du plan") OperatingMode mode,
            @Schema(description = "Index courant dans le mode MANUAL (0-based)") int currentIndex,
            @Schema(description = "Nombre total d'opérations") int totalOperations,
            @Schema(description = "Opérations en attente de confirmation") long pendingCount,
            @Schema(description = "Opérations approuvées, en attente d'exécution") long approvedCount,
            @Schema(description = "Opérations exécutées avec succès") long appliedCount,
            @Schema(description = "Opérations rejetées manuellement") long rejectedCount,
            @Schema(description = "Opérations ayant échoué lors de l'écriture") long errorCount
    ) {}
}
