package com.djtools.ayan.musictagger.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BatchApplyResultTest {

    @Test
    void shouldHandleNullResults() {
        var result = new BatchApplyResult("plan-1", 0, 0, 0, null, Duration.ZERO);

        assertThat(result.results()).isEmpty();
    }

    @Test
    void shouldCreateDefensiveCopy() {
        var writeResult = new TagWriteResult("/a.mp3", OperationStatus.APPLIED, null);
        var result = new BatchApplyResult("plan-1", 1, 1, 0, List.of(writeResult), Duration.ofMillis(100));

        assertThat(result.results()).hasSize(1);
        assertThat(result.results().getFirst().filepath()).isEqualTo("/a.mp3");
    }
}
