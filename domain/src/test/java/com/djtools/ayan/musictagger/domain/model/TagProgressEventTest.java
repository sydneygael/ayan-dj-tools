package com.djtools.ayan.musictagger.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TagProgressEventTest {

    @Test
    void shouldCreateTagProgressEvent() {
        var event = new TagProgressEvent("plan-1", 2, 5, "/a.mp3", OperationStatus.APPLIED, "OK");

        assertThat(event.planId()).isEqualTo("plan-1");
        assertThat(event.index()).isEqualTo(2);
        assertThat(event.total()).isEqualTo(5);
        assertThat(event.filepath()).isEqualTo("/a.mp3");
        assertThat(event.status()).isEqualTo(OperationStatus.APPLIED);
        assertThat(event.message()).isEqualTo("OK");
    }
}
