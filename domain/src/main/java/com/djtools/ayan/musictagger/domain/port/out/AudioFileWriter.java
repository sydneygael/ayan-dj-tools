package com.djtools.ayan.musictagger.domain.port.out;

import com.djtools.ayan.musictagger.domain.model.TagPreview;
import com.djtools.ayan.musictagger.domain.model.TagWriteResult;

import java.util.Map;

public interface AudioFileWriter {

    TagWriteResult writeTags(String filepath, Map<String, String> tags);

    TagPreview previewChanges(String filepath, Map<String, String> newTags);
}
