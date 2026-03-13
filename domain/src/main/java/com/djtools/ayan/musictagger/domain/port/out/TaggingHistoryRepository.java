package com.djtools.ayan.musictagger.domain.port.out;

import com.djtools.ayan.musictagger.domain.model.TaggingHistoryEntry;

import java.util.List;

/** Port sortant : historique des opérations de tagging (audit trail). */
public interface TaggingHistoryRepository {

    void save(TaggingHistoryEntry entry);

    List<TaggingHistoryEntry> findByPlanId(String planId);

    List<TaggingHistoryEntry> findByFilepath(String filepath);

    List<TaggingHistoryEntry> findAll();
}
