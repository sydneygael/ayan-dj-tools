package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.out.AudioFileWriter;
import com.djtools.ayan.musictagger.domain.port.out.PlanRepository;
import com.djtools.ayan.musictagger.domain.port.out.TaggingHistoryRepository;
import com.djtools.ayan.musictagger.domain.usecase.CreatePlanUseCase;
import com.djtools.ayan.musictagger.domain.usecase.ExecutePlanUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanManagementServiceTest {

    @Mock CreatePlanUseCase createPlanUseCase;
    @Mock ExecutePlanUseCase executePlanUseCase;
    @Mock PlanRepository planRepository;
    @Mock AudioFileWriter audioFileWriter;
    @Mock TaggingHistoryRepository historyRepository;

    private PlanManagementService service;

    @BeforeEach
    void setUp() {
        service = new PlanManagementService(createPlanUseCase, executePlanUseCase, planRepository, audioFileWriter, historyRepository);
    }

    @Test
    void shouldCreateAndSavePlan() {
        var plan = new TaggingPlan("plan-1", List.of(), LocalDateTime.now(), PlanStatus.DRAFT, 0, 0);
        when(createPlanUseCase.execute(anyString(), any())).thenReturn(plan);

        TaggingPlan result = service.createPlan(List.of("/a.mp3"));

        assertThat(result).isEqualTo(plan);
        verify(planRepository).save(plan);
    }

    @Test
    void shouldGetPlanById() {
        var plan = new TaggingPlan("plan-1", List.of(), LocalDateTime.now(), PlanStatus.READY_FOR_REVIEW, 1, 0);
        when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan));

        assertThat(service.getPlan("plan-1")).contains(plan);
    }

    @Test
    void shouldApprovePlan() {
        var pending = new TagOperation("/a.mp3", Map.of(), Map.of("genre", "Techno"), OperationStatus.PENDING, null);
        var plan = new TaggingPlan("plan-1", List.of(pending), LocalDateTime.now(), PlanStatus.READY_FOR_REVIEW, 1, 1);
        when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan));

        TaggingPlan approved = service.approvePlan("plan-1");

        assertThat(approved.status()).isEqualTo(PlanStatus.APPROVED);
        assertThat(approved.operations().getFirst().status()).isEqualTo(OperationStatus.APPROVED);

        var captor = ArgumentCaptor.forClass(TaggingPlan.class);
        verify(planRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PlanStatus.APPROVED);
    }

    @Test
    void shouldRejectApproveIfNotReadyForReview() {
        var plan = new TaggingPlan("plan-1", List.of(), LocalDateTime.now(), PlanStatus.DRAFT, 0, 0);
        when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.approvePlan("plan-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("READY_FOR_REVIEW");
    }

    @Test
    void shouldThrowWhenApprovingNonExistentPlan() {
        when(planRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approvePlan("nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void shouldDeletePlan() {
        service.deletePlan("plan-1");

        verify(planRepository).delete("plan-1");
    }

    @Test
    void shouldExecuteApprovedPlan() {
        var op = new TagOperation("/a.mp3", Map.of(), Map.of("genre", "Techno"), OperationStatus.APPROVED, null);
        var plan = new TaggingPlan("plan-1", List.of(op), LocalDateTime.now(), PlanStatus.APPROVED, 1, 1);
        when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan));

        var batchResult = new BatchApplyResult("plan-1", 1, 1, 0,
                List.of(new TagWriteResult("/a.mp3", OperationStatus.APPLIED, null)), Duration.ofMillis(50));
        when(executePlanUseCase.execute(any())).thenReturn(batchResult);

        BatchApplyResult result = service.executePlan("plan-1");

        assertThat(result.successCount()).isEqualTo(1);
        verify(planRepository, times(2)).save(any(TaggingPlan.class));
    }

    @Test
    void shouldRejectExecuteIfNotApproved() {
        var plan = new TaggingPlan("plan-1", List.of(), LocalDateTime.now(), PlanStatus.DRAFT, 0, 0);
        when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.executePlan("plan-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    void shouldTransitionStatusDuringExecution() {
        var op = new TagOperation("/a.mp3", Map.of(), Map.of("genre", "Techno"), OperationStatus.APPROVED, null);
        var plan = new TaggingPlan("plan-1", List.of(op), LocalDateTime.now(), PlanStatus.APPROVED, 1, 1);
        when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan));

        var batchResult = new BatchApplyResult("plan-1", 1, 1, 0,
                List.of(new TagWriteResult("/a.mp3", OperationStatus.APPLIED, null)), Duration.ofMillis(50));
        when(executePlanUseCase.execute(any())).thenReturn(batchResult);

        service.executePlan("plan-1");

        var captor = ArgumentCaptor.forClass(TaggingPlan.class);
        verify(planRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).status()).isEqualTo(PlanStatus.APPLYING);
        assertThat(captor.getAllValues().get(1).status()).isEqualTo(PlanStatus.COMPLETED);
    }
}
