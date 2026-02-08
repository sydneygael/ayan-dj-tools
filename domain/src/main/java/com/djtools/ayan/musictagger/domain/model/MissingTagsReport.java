package com.djtools.ayan.musictagger.domain.model;

import com.djtools.ayan.musictagger.domain.model.vo.Filepath;

import java.util.List;

public record MissingTagsReport(Filepath filepath, List<String> missingTags) {

    public MissingTagsReport {
        missingTags = List.copyOf(missingTags);
    }

    public boolean hasMissingTags() {
        return !missingTags.isEmpty();
    }

    public int missingCount() {
        return missingTags.size();
    }
}
