package com.djtools.ayan.musictagger.domain.port.out;

import com.djtools.ayan.musictagger.domain.model.TaggingPlan;

import java.util.List;
import java.util.Optional;

/** Port sortant : persistance des plans de tagging (Redis). */
public interface PlanRepository {

    void save(TaggingPlan plan);

    Optional<TaggingPlan> findById(String planId);

    void delete(String planId);

    List<TaggingPlan> findAll();
}
