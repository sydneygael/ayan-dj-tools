package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.port.out.PlanRepository;
import com.djtools.ayan.musictagger.domain.usecase.ExecutePlanUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplyModeServiceTest {

    @Mock PlanRepository planRepository;
    @Mock ExecutePlanUseCase executePlanUseCase;
    @Mock SimpMessagingTemplate messagingTemplate;

    private ApplyModeService service;

    @BeforeEach
    void setUp() {
        service = new ApplyModeService(planRepository, executePlanUseCase, messagingTemplate);
    }

    @Test
    void executeAutomatic_happyPath() throws Exception {
        var op = new TagOperation("/a.mp3", Map.of(), Map.of("genre", "Techno"), OperationStatus.APPROVED, null);
        var plan = new TaggingPlan("plan-1", List.of(op), LocalDateTime.now(), PlanStatus.APPROVED, 1, 1);
        when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan));

        var batchResult = new BatchApplyResult("plan-1", 1, 1, 0,
                List.of(new TagWriteResult("/a.mp3", OperationStatus.APPLIED, null)), Duration.ofMillis(50));
        when(executePlanUseCase.execute(any(TaggingPlan.class), any())).thenReturn(batchResult);

        CompletableFuture<BatchApplyResult> future = service.executeAutomatic("plan-1");
        BatchApplyResult result = future.join();

        assertThat(result.successCount()).isEqualTo(1);

        var captor = ArgumentCaptor.forClass(TaggingPlan.class);
        verify(planRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).status()).isEqualTo(PlanStatus.APPLYING);
        assertThat(captor.getAllValues().get(1).status()).isEqualTo(PlanStatus.COMPLETED);
    }

    @Test
    void executeAutomatic_throwsWhenNotApproved() {
        var plan = new TaggingPlan("plan-1", List.of(), LocalDateTime.now(), PlanStatus.DRAFT, 0, 0);
        when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.executeAutomatic("plan-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED");
        verify(executePlanUseCase, never()).execute(any(TaggingPlan.class), any());
    }

    @Test
    void executeAutomatic_throwsWhenNotFound() {
        when(planRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executeAutomatic("nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }
}
