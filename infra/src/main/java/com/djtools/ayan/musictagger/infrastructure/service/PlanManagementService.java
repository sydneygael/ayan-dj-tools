package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.out.AudioFileWriter;
import com.djtools.ayan.musictagger.domain.port.out.PlanRepository;
import com.djtools.ayan.musictagger.domain.port.out.TaggingHistoryRepository;
import com.djtools.ayan.musictagger.domain.usecase.CreatePlanUseCase;
import com.djtools.ayan.musictagger.domain.usecase.ExecutePlanUseCase;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlanManagementService {

    private final CreatePlanUseCase createPlanUseCase;
    private final ExecutePlanUseCase executePlanUseCase;
    private final PlanRepository planRepository;
    private final AudioFileWriter audioFileWriter;
    private final TaggingHistoryRepository historyRepository;

    public PlanManagementService(CreatePlanUseCase createPlanUseCase,
                                 ExecutePlanUseCase executePlanUseCase,
                                 PlanRepository planRepository,
                                 AudioFileWriter audioFileWriter,
                                 TaggingHistoryRepository historyRepository) {
        this.createPlanUseCase = createPlanUseCase;
        this.executePlanUseCase = executePlanUseCase;
        this.planRepository = planRepository;
        this.audioFileWriter = audioFileWriter;
        this.historyRepository = historyRepository;
    }

    public TaggingPlan createPlan(List<String> filePaths) {
        return createPlan(filePaths, OperatingMode.PLAN);
    }

    public TaggingPlan createPlan(List<String> filePaths, OperatingMode mode) {
        String planId = UUID.randomUUID().toString();
        List<Filepath> paths = filePaths.stream().map(Filepath::new).toList();

        TaggingPlan plan = createPlanUseCase.execute(planId, paths).withMode(mode);

        if (mode == OperatingMode.APPLY) {
            plan = plan.withOperations(plan.operations().stream()
                    .map(op -> op.withStatus(OperationStatus.APPROVED))
                    .toList())
                    .withStatus(PlanStatus.APPROVED);
        }

        planRepository.save(plan);
        return plan;
    }

    public Optional<TaggingPlan> getPlan(String planId) {
        return planRepository.findById(planId);
    }

    public TaggingPlan approvePlan(String planId) {
        TaggingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan introuvable : " + planId));

        if (plan.status() != PlanStatus.READY_FOR_REVIEW) {
            throw new IllegalStateException(
                    "Le plan ne peut être approuvé que depuis le statut READY_FOR_REVIEW (actuel : %s)".formatted(plan.status()));
        }

        TaggingPlan approved = new TaggingPlan(
                plan.planId(),
                plan.operations().stream()
                        .map(op -> op.status() == OperationStatus.PENDING ? op.withStatus(OperationStatus.APPROVED) : op)
                        .toList(),
                plan.createdAt(),
                PlanStatus.APPROVED,
                plan.totalFiles(),
                plan.filesWithMissingTags(),
                plan.mode(),
                plan.currentIndex()
        );

        planRepository.save(approved);
        return approved;
    }

    public BatchApplyResult executePlan(String planId) {
        TaggingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan introuvable : " + planId));

        if (plan.status() != PlanStatus.APPROVED) {
            throw new IllegalStateException(
                    "Le plan ne peut être exécuté que depuis le statut APPROVED (actuel : %s)".formatted(plan.status()));
        }

        planRepository.save(plan.withStatus(PlanStatus.APPLYING));

        BatchApplyResult result = executePlanUseCase.execute(plan);

        TaggingPlan completed = new TaggingPlan(
                plan.planId(),
                plan.operations().stream()
                        .map(op -> {
                            if (op.status() != OperationStatus.APPROVED) {
                                return op;
                            }
                            return result.results().stream()
                                    .filter(r -> r.filepath().equals(op.filepath()))
                                    .findFirst()
                                    .map(r -> op.withStatusAndMessage(r.status(), r.message()))
                                    .orElse(op);
                        })
                        .toList(),
                plan.createdAt(),
                PlanStatus.COMPLETED,
                plan.totalFiles(),
                plan.filesWithMissingTags(),
                plan.mode(),
                plan.currentIndex()
        );

        planRepository.save(completed);
        return result;
    }

    public List<TagPreview> previewPlan(String planId) {
        TaggingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan introuvable : " + planId));

        return plan.operations().stream()
                .filter(TagOperation::hasSuggestions)
                .map(op -> audioFileWriter.previewChanges(op.filepath(), op.suggestedTags()))
                .toList();
    }

    public List<TaggingHistoryEntry> getPlanHistory(String planId) {
        return historyRepository.findByPlanId(planId);
    }

    public TagPreview previewFile(String filepath, java.util.Map<String, String> tags) {
        return audioFileWriter.previewChanges(filepath, tags);
    }

    public void deletePlan(String planId) {
        planRepository.delete(planId);
    }
}
