package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.port.out.AudioFileWriter;
import com.djtools.ayan.musictagger.domain.port.out.PlanRepository;
import com.djtools.ayan.musictagger.domain.port.out.TaggingHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManualModeServiceTest {

    @Mock PlanRepository planRepository;
    @Mock AudioFileWriter audioFileWriter;
    @Mock TaggingHistoryRepository historyRepository;

    private ManualModeService service;

    private TaggingPlan approvedPlanWith2Ops(int currentIndex) {
        var op1 = new TagOperation("/a.mp3", Map.of("artist", "Old"), Map.of("artist", "New", "genre", "Techno"), OperationStatus.PENDING, null);
        var op2 = new TagOperation("/b.mp3", Map.of(), Map.of("bpm", "128"), OperationStatus.PENDING, null);
        return new TaggingPlan("plan-1", List.of(op1, op2), LocalDateTime.now(),
                PlanStatus.APPROVED, 2, 2, OperatingMode.MANUAL, currentIndex);
    }

    @BeforeEach
    void setUp() {
        service = new ManualModeService(planRepository, audioFileWriter, historyRepository);
    }

    @Test
    void prepareNextFile_returnsCurrentOperation() {
        var plan = approvedPlanWith2Ops(0);
        when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan));

        TagOperation result = service.prepareNextFile("plan-1");

        assertThat(result.filepath()).isEqualTo("/a.mp3");
        assertThat(result.suggestedTags()).containsEntry("genre", "Techno");
    }

    @Test
    void prepareNextFile_throwsWhenAllProcessed() {
        var plan = approvedPlanWith2Ops(2);
        when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.prepareNextFile("plan-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("traitees");
    }

    @Test
    void confirmFile_approved_writesAndSavesHistory() {
        var plan = approvedPlanWith2Ops(0);
        when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan));
        when(audioFileWriter.writeTags(eq("/a.mp3"), any())).thenReturn(
                new TagWriteResult("/a.mp3", OperationStatus.APPLIED, null));

        TagOperation result = service.confirmFile("plan-1", 0, true);

        assertThat(result.status()).isEqualTo(OperationStatus.APPLIED);
        verify(audioFileWriter).writeTags(eq("/a.mp3"), any());
        verify(historyRepository).save(any(TaggingHistoryEntry.class));

        var planCaptor = ArgumentCaptor.forClass(TaggingPlan.class);
        verify(planRepository).save(planCaptor.capture());
        assertThat(planCaptor.getValue().currentIndex()).isEqualTo(1);
    }

    @Test
    void confirmFile_rejected_noTagWrite() {
        var plan = approvedPlanWith2Ops(0);
        when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan));

        TagOperation result = service.confirmFile("plan-1", 0, false);

        assertThat(result.status()).isEqualTo(OperationStatus.REJECTED);
        verify(audioFileWriter, never()).writeTags(anyString(), any());
        verify(historyRepository, never()).save(any());

        var planCaptor = ArgumentCaptor.forClass(TaggingPlan.class);
        verify(planRepository).save(planCaptor.capture());
        assertThat(planCaptor.getValue().currentIndex()).isEqualTo(1);
    }

    @Test
    void isComplete_returnsTrueWhenDone() {
        when(planRepository.findById("plan-1")).thenReturn(Optional.of(approvedPlanWith2Ops(2)));
        assertThat(service.isComplete("plan-1")).isTrue();

        when(planRepository.findById("plan-1")).thenReturn(Optional.of(approvedPlanWith2Ops(0)));
        assertThat(service.isComplete("plan-1")).isFalse();
    }
}
