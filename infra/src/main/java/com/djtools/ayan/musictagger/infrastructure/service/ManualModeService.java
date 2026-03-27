package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.port.out.AudioFileWriter;
import com.djtools.ayan.musictagger.domain.port.out.PlanRepository;
import com.djtools.ayan.musictagger.domain.port.out.TaggingHistoryRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ManualModeService {

    private final PlanRepository planRepository;
    private final AudioFileWriter audioFileWriter;
    private final TaggingHistoryRepository historyRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ManualModeService(PlanRepository planRepository,
                             AudioFileWriter audioFileWriter,
                             TaggingHistoryRepository historyRepository,
                             SimpMessagingTemplate messagingTemplate) {
        this.planRepository = planRepository;
        this.audioFileWriter = audioFileWriter;
        this.historyRepository = historyRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public TagOperation prepareNextFile(String planId) {
        final var plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan introuvable : " + planId));

        final var index = plan.currentIndex();
        if (index >= plan.operations().size()) {
            throw new IllegalStateException("Toutes les operations du plan ont ete traitees");
        }

        return plan.operations().get(index);
    }

    public TagOperation confirmFile(String planId, int index, boolean approved) {
        final var plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan introuvable : " + planId));

        if (index < 0 || index >= plan.operations().size()) {
            throw new IllegalArgumentException("Index d'operation invalide : " + index);
        }

        final var op = plan.operations().get(index);
        OperationStatus newStatus;
        String message = null;

        if (approved) {
            final var result = audioFileWriter.writeTags(op.filepath(), op.suggestedTags());
            newStatus = result.status();
            message = result.message();

            var entry = new TaggingHistoryEntry(
                    op.filepath(), planId, op.currentTags(), op.suggestedTags(),
                    result.status(), result.message(), LocalDateTime.now());
            historyRepository.save(entry);
        } else {
            newStatus = OperationStatus.REJECTED;
        }

        final var updated = op.withStatusAndMessage(newStatus, message);

        var updatedOps = new java.util.ArrayList<>(plan.operations());
        updatedOps.set(index, updated);

        final var nextIndex = index + 1;
        final var planStatus = nextIndex >= plan.operations().size() ? PlanStatus.COMPLETED : plan.status();

        final var updatedPlan = new TaggingPlan(
                plan.planId(), updatedOps, plan.createdAt(), planStatus,
                plan.totalFiles(), plan.filesWithMissingTags(), plan.mode(), nextIndex);
        planRepository.save(updatedPlan);

        var progressEvent = new TagProgressEvent(
                planId, index, plan.operations().size(), op.filepath(), newStatus, message);
        messagingTemplate.convertAndSend("/topic/plan/" + planId + "/progress", progressEvent);

        return updated;
    }

    public boolean isComplete(String planId) {
        final var plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan introuvable : " + planId));

        return plan.currentIndex() >= plan.operations().size();
    }
}
