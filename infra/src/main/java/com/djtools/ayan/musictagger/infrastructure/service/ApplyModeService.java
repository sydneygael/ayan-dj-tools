package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.port.out.PlanRepository;
import com.djtools.ayan.musictagger.domain.usecase.ExecutePlanUseCase;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ApplyModeService {

    private final PlanRepository planRepository;
    private final ExecutePlanUseCase executePlanUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    public ApplyModeService(PlanRepository planRepository,
                            ExecutePlanUseCase executePlanUseCase,
                            SimpMessagingTemplate messagingTemplate) {
        this.planRepository = planRepository;
        this.executePlanUseCase = executePlanUseCase;
        this.messagingTemplate = messagingTemplate;
    }

    @Async
    public CompletableFuture<BatchApplyResult> executeAutomatic(String planId) {
        TaggingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan introuvable : " + planId));

        if (plan.status() != PlanStatus.APPROVED) {
            throw new IllegalStateException(
                    "Le plan ne peut être exécuté que depuis le statut APPROVED (actuel : %s)".formatted(plan.status()));
        }

        planRepository.save(plan.withStatus(PlanStatus.APPLYING));

        BatchApplyResult result = executePlanUseCase.execute(plan, event ->
                messagingTemplate.convertAndSend("/topic/plan/" + planId + "/progress", event));

        TaggingPlan completed = plan.withStatus(PlanStatus.COMPLETED)
                .withOperations(plan.operations().stream()
                        .map(op -> {
                            if (op.status() != OperationStatus.APPROVED) return op;
                            return result.results().stream()
                                    .filter(r -> r.filepath().equals(op.filepath()))
                                    .findFirst()
                                    .map(r -> op.withStatusAndMessage(r.status(), r.message()))
                                    .orElse(op);
                        })
                        .toList());

        planRepository.save(completed);
        return CompletableFuture.completedFuture(result);
    }
}
