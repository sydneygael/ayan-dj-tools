package com.djtools.ayan.musictagger.domain.port.out;

import com.djtools.ayan.musictagger.domain.model.TaggingPlan;

import java.util.Optional;

public interface PlanRepository {

    void save(TaggingPlan plan);

    Optional<TaggingPlan> findById(String planId);

    void delete(String planId);
}
