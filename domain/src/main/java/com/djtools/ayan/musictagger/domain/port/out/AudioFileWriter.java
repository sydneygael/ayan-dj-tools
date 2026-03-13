package com.djtools.ayan.musictagger.domain.port.out;

import com.djtools.ayan.musictagger.domain.model.TagPreview;
import com.djtools.ayan.musictagger.domain.model.TagWriteResult;

import java.util.Map;

/** Port sortant : écriture de tags dans les fichiers audio (avec backup/rollback). */
public interface AudioFileWriter {

    /** Écrit les tags dans le fichier. Crée un backup avant écriture. */
    TagWriteResult writeTags(String filepath, Map<String, String> tags);

    /** Calcule le diff (avant/après) sans modifier le fichier. */
    TagPreview previewChanges(String filepath, Map<String, String> newTags);
}
