package com.djtools.ayan.musictagger.domain.model;

import java.util.Map;

/** Chronologie d'activité : plans/tags par période, usage des modes, durées moyennes. */
public record ActivityTimeline(
        Map<String, Long> plansPerPeriod,
        Map<String, Long> tagsAppliedPerPeriod,
        Map<String, Long> modeUsage,
        Map<String, Double> averageDurationByMode
) {

    public ActivityTimeline {
        plansPerPeriod = plansPerPeriod != null ? Map.copyOf(plansPerPeriod) : Map.of();
        tagsAppliedPerPeriod = tagsAppliedPerPeriod != null ? Map.copyOf(tagsAppliedPerPeriod) : Map.of();
        modeUsage = modeUsage != null ? Map.copyOf(modeUsage) : Map.of();
        averageDurationByMode = averageDurationByMode != null ? Map.copyOf(averageDurationByMode) : Map.of();
    }
}
