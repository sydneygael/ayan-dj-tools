package com.djtools.ayan.musictagger.domain.port.in;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;

import java.util.Optional;

public interface AudioFeatureExtractor {
    Optional<AudioFeatures> extract(Filepath filepath);
}
