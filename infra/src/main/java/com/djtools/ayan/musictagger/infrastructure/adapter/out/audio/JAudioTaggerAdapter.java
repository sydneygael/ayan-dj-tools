package com.djtools.ayan.musictagger.infrastructure.adapter.out.audio;

import com.djtools.ayan.musictagger.domain.exception.AudioProcessingException;
import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.in.AudioFileReader;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Optional;

@Component
public class JAudioTaggerAdapter implements AudioFileReader {

    private static final Logger log = LoggerFactory.getLogger(JAudioTaggerAdapter.class);

    @Override
    public Optional<MusicFileInfo> readTags(Filepath path) {
        File file = new File(path.value());
        if (!file.exists() || !file.isFile()) {
            log.warn("File not found or not a regular file: {}", path.value());
            return Optional.empty();
        }

        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();

            String artist = tag != null ? getField(tag, FieldKey.ARTIST) : null;
            String title = tag != null ? getField(tag, FieldKey.TITLE) : null;
            String album = tag != null ? getField(tag, FieldKey.ALBUM) : null;
            String genre = tag != null ? getField(tag, FieldKey.GENRE) : null;
            String bpm = tag != null ? getField(tag, FieldKey.BPM) : null;
            String key = tag != null ? getField(tag, FieldKey.KEY) : null;

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
                    file.lastModified()
            ));
        } catch (Exception e) {
            throw new AudioProcessingException("Failed to read audio file: " + path.value(), e);
        }
    }

    private String getField(Tag tag, FieldKey fieldKey) {
        try {
            String value = tag.getFirst(fieldKey);
            return (value != null && !value.isBlank()) ? value : null;
        } catch (Exception e) {
            return null;
        }
    }
}
