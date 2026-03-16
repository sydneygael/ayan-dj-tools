package com.djtools.ayan.musictagger.domain.port.out;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;

import java.util.List;
import java.util.Optional;

/** Port sortant : cache des AudioFeatures par filepath (Redis). */
public interface AudioFeaturesCacheRepository {

    void save(String filepath, AudioFeatures features);

    Optional<AudioFeatures> findByFilepath(String filepath);

    List<AudioFeatures> findAll();
}
