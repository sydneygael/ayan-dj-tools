package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.infrastructure.service.PlanManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanControllerTest {

    @Mock PlanManagementService planManagementService;
    @Mock com.djtools.ayan.musictagger.infrastructure.service.ManualModeService manualModeService;
    @Mock com.djtools.ayan.musictagger.infrastructure.service.ApplyModeService applyModeService;
    @InjectMocks PlanController controller;

    @Test
    void shouldCreatePlan() {
        var plan = new TaggingPlan("plan-1",
                List.of(new TagOperation("/a.mp3", Map.of(), Map.of("genre", "Techno"), OperationStatus.PENDING, null)),
                LocalDateTime.of(2026, 3, 5, 10, 0), PlanStatus.READY_FOR_REVIEW, 1, 1);
        when(planManagementService.createPlan(any(), any())).thenReturn(plan);

        TaggingPlan result = controller.createPlan(new PlanController.CreatePlanRequest(List.of("/a.mp3"), null));

        assertThat(result.planId()).isEqualTo("plan-1");
        assertThat(result.status()).isEqualTo(PlanStatus.READY_FOR_REVIEW);
        assertThat(result.operations()).hasSize(1);
    }

    @Test
    void shouldGetPlan() {
        var plan = new TaggingPlan("plan-1", List.of(), LocalDateTime.now(), PlanStatus.DRAFT, 0, 0);
        when(planManagementService.getPlan("plan-1")).thenReturn(Optional.of(plan));

        TaggingPlan result = controller.getPlan("plan-1");

        assertThat(result.planId()).isEqualTo("plan-1");
    }

    @Test
    void shouldThrow404ForMissingPlan() {
        when(planManagementService.getPlan("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getPlan("nope"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldApprovePlan() {
        var approved = new TaggingPlan("plan-1", List.of(), LocalDateTime.now(), PlanStatus.APPROVED, 1, 0);
        when(planManagementService.approvePlan("plan-1")).thenReturn(approved);

        TaggingPlan result = controller.approvePlan("plan-1");

        assertThat(result.status()).isEqualTo(PlanStatus.APPROVED);
    }

    @Test
    void shouldDeletePlan() {
        controller.deletePlan("plan-1");

        verify(planManagementService).deletePlan("plan-1");
    }

    @Test
    void shouldExecutePlan() {
        var batchResult = new BatchApplyResult("plan-1", 1, 1, 0,
                List.of(new TagWriteResult("/a.mp3", OperationStatus.APPLIED, null)), Duration.ofMillis(50));
        when(planManagementService.executePlan("plan-1")).thenReturn(batchResult);

        BatchApplyResult result = controller.executePlan("plan-1");

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.planId()).isEqualTo("plan-1");
    }

    @Test
    void shouldPreviewPlan() {
        var previews = List.of(new TagPreview("/a.mp3", List.of(new TagChange("genre", null, "Techno"))));
        when(planManagementService.previewPlan("plan-1")).thenReturn(previews);

        List<TagPreview> result = controller.previewPlan("plan-1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().changes()).hasSize(1);
    }

    @Test
    void shouldGetPlanHistory() {
        var entries = List.of(new TaggingHistoryEntry("/a.mp3", "plan-1", Map.of(), Map.of("genre", "Techno"),
                OperationStatus.APPLIED, null, LocalDateTime.now()));
        when(planManagementService.getPlanHistory("plan-1")).thenReturn(entries);

        List<TaggingHistoryEntry> result = controller.getPlanHistory("plan-1");

        assertThat(result).hasSize(1);
    }
}
