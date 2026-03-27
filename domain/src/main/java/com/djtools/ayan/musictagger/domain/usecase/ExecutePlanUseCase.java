package com.djtools.ayan.musictagger.domain.usecase;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.port.out.AudioFileWriter;
import com.djtools.ayan.musictagger.domain.port.out.TaggingHistoryRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Exécute un plan de tagging : écrit les tags approuvés dans les fichiers audio.
 *
 * Étapes par opération : écriture des tags → enregistrement dans l'historique → notification de progression.
 * Seules les opérations APPROVED sont traitées.
 */
public class ExecutePlanUseCase {

    private static final System.Logger log = System.getLogger(ExecutePlanUseCase.class.getName());

    private final AudioFileWriter audioFileWriter;
    private final TaggingHistoryRepository historyRepository;

    public ExecutePlanUseCase(AudioFileWriter audioFileWriter, TaggingHistoryRepository historyRepository) {
        this.audioFileWriter = audioFileWriter;
        this.historyRepository = historyRepository;
    }

    /** Exécute le plan sans callback de progression. */
    public BatchApplyResult execute(TaggingPlan plan) {
        return execute(plan, _ -> {});
    }

    /** Exécute le plan avec notification de progression à chaque fichier traité. */
    public BatchApplyResult execute(TaggingPlan plan, Consumer<TagProgressEvent> onProgress) {
        final var start = Instant.now();
        final var approvedOps = filterApprovedOperations(plan);
        log.log(System.Logger.Level.INFO, "Exécution du plan {0} : {1} opération(s) approuvée(s)",
                plan.planId(), approvedOps.size());
        final var results = new ArrayList<TagWriteResult>();
        int successCount = 0;
        int errorCount = 0;

        for (int index = 0; index < approvedOps.size(); index++) {
            final var op = approvedOps.get(index);

            // Étape 1 : écrire les tags dans le fichier
            final var result = audioFileWriter.writeTags(op.filepath(), op.suggestedTags());
            results.add(result);

            // Étape 2 : sauvegarder dans l'historique (avant/après)
            recordHistory(plan.planId(), op, result);

            // Étape 3 : comptabiliser succès/erreur
            if (result.status() == OperationStatus.APPLIED) {
                successCount++;
                log.log(System.Logger.Level.DEBUG, "[{0}/{1}] Tags appliqués : {2}",
                        index + 1, approvedOps.size(), op.filepath());
            } else {
                errorCount++;
                log.log(System.Logger.Level.WARNING, "[{0}/{1}] Échec application tags : {2} — {3}",
                        index + 1, approvedOps.size(), op.filepath(), result.message());
            }

            // Étape 4 : notifier la progression
            onProgress.accept(new TagProgressEvent(
                    plan.planId(), index, approvedOps.size(), op.filepath(), result.status(), result.message()));
        }

        final var batchResult = new BatchApplyResult(
                plan.planId(), results.size(), successCount, errorCount,
                results, Duration.between(start, Instant.now()));
        log.log(System.Logger.Level.INFO, "Plan {0} terminé : {1} succès, {2} erreur(s) en {3}ms",
                plan.planId(), successCount, errorCount, batchResult.duration().toMillis());
        return batchResult;
    }

    /** Filtre les opérations approuvées du plan. */
    private List<TagOperation> filterApprovedOperations(TaggingPlan plan) {
        return plan.operations().stream()
                .filter(op -> op.status() == OperationStatus.APPROVED)
                .toList();
    }

    /** Enregistre l'opération dans l'historique de tagging. */
    private void recordHistory(String planId, TagOperation op, TagWriteResult result) {
        var entry = new TaggingHistoryEntry(
                op.filepath(), planId, op.currentTags(), op.suggestedTags(),
                result.status(), result.message(), LocalDateTime.now());
        historyRepository.save(entry);
    }
}
