package com.djtools.ayan.musictagger.infrastructure.adapter.out.audio;

import com.djtools.ayan.musictagger.domain.exception.AudioProcessingException;
import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.in.AudioFileReader;
import com.djtools.ayan.musictagger.domain.port.out.AudioFileWriter;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.framebody.FrameBodyGEOB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Component
public class JAudioTaggerAdapter implements AudioFileReader, AudioFileWriter {

    private static final Logger log = LoggerFactory.getLogger(JAudioTaggerAdapter.class);

    @Override
    public Optional<MusicFileInfo> readTags(Filepath path) {
        final var file = new File(path.value());
        if (!file.exists() || !file.isFile()) {
            log.warn("File not found or not a regular file: {}", path.value());
            return Optional.empty();
        }

        try {
            final var audioFile = AudioFileIO.read(file);
            final var tag = audioFile.getTag();

            final var artist = tag != null ? getField(tag, FieldKey.ARTIST) : null;
            final var title = tag != null ? getField(tag, FieldKey.TITLE) : null;
            final var album = tag != null ? getField(tag, FieldKey.ALBUM) : null;
            final var genre = tag != null ? getField(tag, FieldKey.GENRE) : null;
            final var bpm = tag != null ? getField(tag, FieldKey.BPM) : null;
            final var key = tag != null ? getField(tag, FieldKey.KEY) : null;

            return Optional.of(new MusicFileInfo(
                    path,
                    path.filename(),
                    artist,
                    title,
                    album,
                    genre,
                    bpm,
                    key,
                    file.length(),
                    file.lastModified(),
                    detectSeratoAnalysis(tag)
            ));
        } catch (Exception e) {
            throw new AudioProcessingException("Failed to read audio file: " + path.value(), e);
        }
    }

    @Override
    public TagWriteResult writeTags(String filepath, Map<String, String> tags) {
        final var filePath = Path.of(filepath);
        Path backupPath = null;

        try {
            backupPath = createBackup(filePath);

            final var audioFile = AudioFileIO.read(filePath.toFile());
            final var tag = audioFile.getTagOrCreateAndSetDefault();

            for (Map.Entry<String, String> entry : tags.entrySet()) {
                final var fieldKey = FieldKey.valueOf(entry.getKey().toUpperCase());
                tag.setField(fieldKey, entry.getValue());
            }

            audioFile.commit();
            Files.deleteIfExists(backupPath);

            log.info("Tags written successfully to: {}", filepath);
            return new TagWriteResult(filepath, OperationStatus.APPLIED, null);
        } catch (Exception e) {
            log.error("Failed to write tags to: {}", filepath, e);
            if (backupPath != null) {
                restoreBackup(backupPath, filePath);
            }
            return new TagWriteResult(filepath, OperationStatus.ERROR, e.getMessage());
        }
    }

    @Override
    public TagPreview previewChanges(String filepath, Map<String, String> newTags) {
        final var currentTags = readCurrentTags(filepath);
        final var changes = new ArrayList<TagChange>();

        for (Map.Entry<String, String> entry : newTags.entrySet()) {
            final var field = entry.getKey();
            final var newValue = entry.getValue();
            final var oldValue = currentTags.get(field);

            if (!Objects.equals(oldValue, newValue)) {
                changes.add(new TagChange(field, oldValue, newValue));
            }
        }

        return new TagPreview(filepath, changes);
    }

    private Map<String, String> readCurrentTags(String filepath) {
        final var info = readTags(new Filepath(filepath));
        if (info.isEmpty()) {
            return Map.of();
        }
        final var i = info.get();
        final var tags = new LinkedHashMap<String, String>();
        if (i.artist() != null) tags.put("artist", i.artist());
        if (i.title() != null) tags.put("title", i.title());
        if (i.album() != null) tags.put("album", i.album());
        if (i.genre() != null) tags.put("genre", i.genre());
        if (i.bpm() != null) tags.put("bpm", i.bpm());
        if (i.key() != null) tags.put("key", i.key());
        return tags;
    }

    private Path createBackup(Path filePath) throws IOException {
        final var backup = filePath.resolveSibling(filePath.getFileName() + ".bak");
        Files.copy(filePath, backup, StandardCopyOption.REPLACE_EXISTING);
        return backup;
    }

    private void restoreBackup(Path backupPath, Path originalPath) {
        try {
            Files.copy(backupPath, originalPath, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(backupPath);
            log.info("Restored backup for: {}", originalPath);
        } catch (IOException e) {
            log.error("Failed to restore backup for: {}", originalPath, e);
        }
    }

    private boolean detectSeratoAnalysis(Tag tag) {
        if (tag == null) return false;
        var it = tag.getFields();
        while (it.hasNext()) {
            if (it.next().getId().toLowerCase(Locale.ROOT).contains("serato")) return true;
        }
        if (tag instanceof AbstractID3v2Tag id3v2) {
            return containsSeratoFrame(id3v2.getFrame("GEOB"));
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean containsSeratoFrame(Object raw) {
        if (raw == null) return false;
        if (raw instanceof List<?> list) return list.stream().anyMatch(this::isSeratoGEOB);
        return isSeratoGEOB(raw);
    }

    private boolean isSeratoGEOB(Object obj) {
        if (obj instanceof AbstractID3v2Frame frame
                && frame.getBody() instanceof FrameBodyGEOB geob) {
            var desc = geob.getDescription();
            return desc != null && desc.toLowerCase(Locale.ROOT).contains("serato");
        }
        return false;
    }

    private String getField(Tag tag, FieldKey fieldKey) {
        try {
            final var value = tag.getFirst(fieldKey);
            return (value != null && !value.isBlank()) ? value : null;
        } catch (Exception e) {
            return null;
        }
    }
}
