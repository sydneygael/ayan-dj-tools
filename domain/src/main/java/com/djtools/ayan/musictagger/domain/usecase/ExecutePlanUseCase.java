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

public class ExecutePlanUseCase {

    private final AudioFileWriter audioFileWriter;
    private final TaggingHistoryRepository historyRepository;

    public ExecutePlanUseCase(AudioFileWriter audioFileWriter, TaggingHistoryRepository historyRepository) {
        this.audioFileWriter = audioFileWriter;
        this.historyRepository = historyRepository;
    }

    public BatchApplyResult execute(TaggingPlan plan) {
        return execute(plan, _ -> {});
    }

    public BatchApplyResult execute(TaggingPlan plan, Consumer<TagProgressEvent> onProgress) {
        Instant start = Instant.now();
        List<TagWriteResult> results = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;
        int total = (int) plan.operations().stream().filter(op -> op.status() == OperationStatus.APPROVED).count();
        int index = 0;

        for (TagOperation op : plan.operations()) {
            if (op.status() != OperationStatus.APPROVED) {
                continue;
            }

            TagWriteResult result = audioFileWriter.writeTags(op.filepath(), op.suggestedTags());
            results.add(result);

            var entry = new TaggingHistoryEntry(
                    op.filepath(),
                    plan.planId(),
                    op.currentTags(),
                    op.suggestedTags(),
                    result.status(),
                    result.message(),
                    LocalDateTime.now()
            );
            historyRepository.save(entry);

            if (result.status() == OperationStatus.APPLIED) {
                successCount++;
            } else {
                errorCount++;
            }

            onProgress.accept(new TagProgressEvent(
                    plan.planId(), index, total, op.filepath(), result.status(), result.message()));
            index++;
        }

        return new BatchApplyResult(
                plan.planId(),
                results.size(),
                successCount,
                errorCount,
                results,
                Duration.between(start, Instant.now())
        );
    }
}
