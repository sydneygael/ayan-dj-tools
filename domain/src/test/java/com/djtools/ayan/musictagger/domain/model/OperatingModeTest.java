package com.djtools.ayan.musictagger.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperatingModeTest {

    @Test
    void shouldHaveThreeModes() {
        assertThat(OperatingMode.values()).containsExactly(
                OperatingMode.PLAN, OperatingMode.MANUAL, OperatingMode.APPLY);
    }
}
