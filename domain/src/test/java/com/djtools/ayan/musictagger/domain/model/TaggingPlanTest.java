package com.djtools.ayan.musictagger.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TaggingPlanTest {

    @Test
    void shouldCreatePlanWithDefaults() {
        var plan = new TaggingPlan("plan-1", null, LocalDateTime.now(), null, 3, 1);

        assertThat(plan.operations()).isEmpty();
        assertThat(plan.status()).isEqualTo(PlanStatus.DRAFT);
    }

    @Test
    void shouldMakeOperationsImmutable() {
        var op = new TagOperation("/file.mp3", Map.of(), Map.of("genre", "Techno"), OperationStatus.PENDING, null);
        var plan = new TaggingPlan("plan-1", List.of(op), LocalDateTime.now(), PlanStatus.READY_FOR_REVIEW, 1, 1);

        assertThat(plan.operations()).hasSize(1);
    }

    @Test
    void shouldCountPendingOperations() {
        var pending = new TagOperation("/a.mp3", Map.of(), Map.of("genre", "Techno"), OperationStatus.PENDING, null);
        var approved = new TagOperation("/b.mp3", Map.of(), Map.of("bpm", "128"), OperationStatus.APPROVED, null);
        var plan = new TaggingPlan("plan-1", List.of(pending, approved), LocalDateTime.now(), PlanStatus.READY_FOR_REVIEW, 2, 2);

        assertThat(plan.pendingCount()).isEqualTo(1);
        assertThat(plan.approvedCount()).isEqualTo(1);
    }

    @Test
    void shouldChangeStatus() {
        var plan = new TaggingPlan("plan-1", List.of(), LocalDateTime.now(), PlanStatus.READY_FOR_REVIEW, 0, 0);

        var approved = plan.withStatus(PlanStatus.APPROVED);

        assertThat(approved.status()).isEqualTo(PlanStatus.APPROVED);
        assertThat(approved.planId()).isEqualTo("plan-1");
    }

    @Test
    void shouldDefaultModeToPlan() {
        var plan = new TaggingPlan("plan-1", null, LocalDateTime.now(), null, 0, 0);

        assertThat(plan.mode()).isEqualTo(OperatingMode.PLAN);
        assertThat(plan.currentIndex()).isZero();
    }

    @Test
    void shouldDefaultModeToPlanWithSixArgConstructor() {
        var plan = new TaggingPlan("plan-1", List.of(), LocalDateTime.now(), PlanStatus.DRAFT, 0, 0);

        assertThat(plan.mode()).isEqualTo(OperatingMode.PLAN);
        assertThat(plan.currentIndex()).isZero();
    }

    @Test
    void shouldWithCurrentIndex() {
        var plan = new TaggingPlan("plan-1", List.of(), LocalDateTime.now(), PlanStatus.DRAFT, 0, 0);

        var updated = plan.withCurrentIndex(3);

        assertThat(updated.currentIndex()).isEqualTo(3);
        assertThat(updated.planId()).isEqualTo("plan-1");
    }

    @Test
    void shouldWithMode() {
        var plan = new TaggingPlan("plan-1", List.of(), LocalDateTime.now(), PlanStatus.DRAFT, 0, 0);

        var manual = plan.withMode(OperatingMode.MANUAL);

        assertThat(manual.mode()).isEqualTo(OperatingMode.MANUAL);
        assertThat(manual.planId()).isEqualTo("plan-1");
    }

    @Test
    void shouldWithStatusPreserveMode() {
        var plan = new TaggingPlan("plan-1", List.of(), LocalDateTime.now(), PlanStatus.DRAFT, 0, 0,
                OperatingMode.MANUAL, 2);

        var approved = plan.withStatus(PlanStatus.APPROVED);

        assertThat(approved.mode()).isEqualTo(OperatingMode.MANUAL);
        assertThat(approved.currentIndex()).isEqualTo(2);
    }
}
