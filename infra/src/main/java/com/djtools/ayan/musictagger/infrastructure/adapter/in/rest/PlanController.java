package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.infrastructure.service.ApplyModeService;
import com.djtools.ayan.musictagger.infrastructure.service.ManualModeService;
import com.djtools.ayan.musictagger.infrastructure.service.PlanManagementService;
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

    @PostMapping("/create")
    public TaggingPlan createPlan(@RequestBody CreatePlanRequest request) {
        OperatingMode mode = request.mode() != null ? request.mode() : OperatingMode.PLAN;
        return planManagementService.createPlan(request.filePaths(), mode);
    }

    @GetMapping("/{id}")
    public TaggingPlan getPlan(@PathVariable String id) {
        return planManagementService.getPlan(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan introuvable : " + id));
    }

    @PutMapping("/{id}/approve")
    public TaggingPlan approvePlan(@PathVariable String id) {
        return planManagementService.approvePlan(id);
    }

    @PostMapping("/{id}/execute")
    public BatchApplyResult executePlan(@PathVariable String id) {
        return planManagementService.executePlan(id);
    }

    @GetMapping("/{id}/preview")
    public List<TagPreview> previewPlan(@PathVariable String id) {
        return planManagementService.previewPlan(id);
    }

    @GetMapping("/{id}/history")
    public List<TaggingHistoryEntry> getPlanHistory(@PathVariable String id) {
        return planManagementService.getPlanHistory(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlan(@PathVariable String id) {
        planManagementService.deletePlan(id);
    }

    @GetMapping("/{id}/current")
    public TagOperation getCurrentOperation(@PathVariable String id) {
        return manualModeService.prepareNextFile(id);
    }

    @PostMapping("/{id}/operations/{index}/confirm")
    public TagOperation confirmOperation(@PathVariable String id,
                                         @PathVariable int index,
                                         @RequestParam boolean approved) {
        return manualModeService.confirmFile(id, index, approved);
    }

    @PostMapping("/{id}/auto-execute")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void autoExecute(@PathVariable String id) {
        applyModeService.executeAutomatic(id);
    }

    public record CreatePlanRequest(List<String> filePaths, OperatingMode mode) {}
}
