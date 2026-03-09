package com.djtools.ayan.musictagger.domain.usecase;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.port.out.AudioFileWriter;
import com.djtools.ayan.musictagger.domain.port.out.TaggingHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutePlanUseCaseTest {

    @Mock AudioFileWriter audioFileWriter;
    @Mock TaggingHistoryRepository historyRepository;

    private ExecutePlanUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExecutePlanUseCase(audioFileWriter, historyRepository);
    }

    @Test
    void shouldApplyApprovedOperations() {
        var op = new TagOperation("/a.mp3", Map.of(), Map.of("genre", "Techno"), OperationStatus.APPROVED, null);
        var plan = new TaggingPlan("plan-1", List.of(op), LocalDateTime.now(), PlanStatus.APPROVED, 1, 1);

        when(audioFileWriter.writeTags(eq("/a.mp3"), any())).thenReturn(
                new TagWriteResult("/a.mp3", OperationStatus.APPLIED, null));

        BatchApplyResult result = useCase.execute(plan);

        assertThat(result.totalOperations()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.errorCount()).isZero();
    }

    @Test
    void shouldSkipNonApprovedOperations() {
        var pending = new TagOperation("/a.mp3", Map.of(), Map.of("genre", "Techno"), OperationStatus.PENDING, null);
        var rejected = new TagOperation("/b.mp3", Map.of(), Map.of("bpm", "128"), OperationStatus.REJECTED, null);
        var plan = new TaggingPlan("plan-1", List.of(pending, rejected), LocalDateTime.now(), PlanStatus.APPROVED, 2, 2);

        BatchApplyResult result = useCase.execute(plan);

        assertThat(result.totalOperations()).isZero();
        verifyNoInteractions(audioFileWriter);
    }

    @Test
    void shouldHandleWriteErrors() {
        var op = new TagOperation("/a.mp3", Map.of(), Map.of("genre", "Techno"), OperationStatus.APPROVED, null);
        var plan = new TaggingPlan("plan-1", List.of(op), LocalDateTime.now(), PlanStatus.APPROVED, 1, 1);

        when(audioFileWriter.writeTags(eq("/a.mp3"), any())).thenReturn(
                new TagWriteResult("/a.mp3", OperationStatus.ERROR, "Permission denied"));

        BatchApplyResult result = useCase.execute(plan);

        assertThat(result.successCount()).isZero();
        assertThat(result.errorCount()).isEqualTo(1);
        assertThat(result.results().getFirst().message()).isEqualTo("Permission denied");
    }

    @Test
    void shouldRecordHistoryForEachOperation() {
        var op1 = new TagOperation("/a.mp3", Map.of("artist", "Old"), Map.of("genre", "Techno"), OperationStatus.APPROVED, null);
        var op2 = new TagOperation("/b.mp3", Map.of(), Map.of("bpm", "128"), OperationStatus.APPROVED, null);
        var plan = new TaggingPlan("plan-1", List.of(op1, op2), LocalDateTime.now(), PlanStatus.APPROVED, 2, 2);

        when(audioFileWriter.writeTags(eq("/a.mp3"), any())).thenReturn(
                new TagWriteResult("/a.mp3", OperationStatus.APPLIED, null));
        when(audioFileWriter.writeTags(eq("/b.mp3"), any())).thenReturn(
                new TagWriteResult("/b.mp3", OperationStatus.APPLIED, null));

        useCase.execute(plan);

        verify(historyRepository, times(2)).save(any(TaggingHistoryEntry.class));
    }

    @Test
    void shouldReturnCorrectCounts() {
        var ok = new TagOperation("/a.mp3", Map.of(), Map.of("genre", "Techno"), OperationStatus.APPROVED, null);
        var fail = new TagOperation("/b.mp3", Map.of(), Map.of("bpm", "128"), OperationStatus.APPROVED, null);
        var skip = new TagOperation("/c.mp3", Map.of(), Map.of(), OperationStatus.REJECTED, null);
        var plan = new TaggingPlan("plan-1", List.of(ok, fail, skip), LocalDateTime.now(), PlanStatus.APPROVED, 3, 2);

        when(audioFileWriter.writeTags(eq("/a.mp3"), any())).thenReturn(
                new TagWriteResult("/a.mp3", OperationStatus.APPLIED, null));
        when(audioFileWriter.writeTags(eq("/b.mp3"), any())).thenReturn(
                new TagWriteResult("/b.mp3", OperationStatus.ERROR, "fail"));

        BatchApplyResult result = useCase.execute(plan);

        assertThat(result.planId()).isEqualTo("plan-1");
        assertThat(result.totalOperations()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.errorCount()).isEqualTo(1);
        assertThat(result.duration()).isNotNull();
    }

    @Test
    void shouldInvokeProgressCallbackForEachFile() {
        var op1 = new TagOperation("/a.mp3", Map.of(), Map.of("genre", "Techno"), OperationStatus.APPROVED, null);
        var op2 = new TagOperation("/b.mp3", Map.of(), Map.of("bpm", "128"), OperationStatus.APPROVED, null);
        var plan = new TaggingPlan("plan-1", List.of(op1, op2), LocalDateTime.now(), PlanStatus.APPROVED, 2, 2);

        when(audioFileWriter.writeTags(eq("/a.mp3"), any())).thenReturn(
                new TagWriteResult("/a.mp3", OperationStatus.APPLIED, null));
        when(audioFileWriter.writeTags(eq("/b.mp3"), any())).thenReturn(
                new TagWriteResult("/b.mp3", OperationStatus.APPLIED, null));

        List<TagProgressEvent> events = new ArrayList<>();
        useCase.execute(plan, events::add);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).index()).isZero();
        assertThat(events.get(0).filepath()).isEqualTo("/a.mp3");
        assertThat(events.get(1).index()).isEqualTo(1);
        assertThat(events.get(1).total()).isEqualTo(2);
    }
}
