package com.djtools.ayan.musictagger.infrastructure.adapter.out.audio;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.id3.ID3v24Tag;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates minimal valid MP3 files for testing.
 * Generates a tiny MP3 frame with an ID3v2.4 tag.
 */
final class TestAudioFileHelper {

    private TestAudioFileHelper() {}

    static Path createMp3WithAllTags(Path directory, String filename) throws Exception {
        Path file = createMinimalMp3(directory, filename);
        AudioFile audioFile = AudioFileIO.read(file.toFile());
        var tag = new ID3v24Tag();
        tag.setField(FieldKey.ARTIST, "Test Artist");
        tag.setField(FieldKey.TITLE, "Test Title");
        tag.setField(FieldKey.ALBUM, "Test Album");
        tag.setField(FieldKey.GENRE, "Electronic");
        tag.setField(FieldKey.BPM, "128");
        tag.setField(FieldKey.KEY, "Am");
        audioFile.setTag(tag);
        audioFile.commit();
        return file;
    }

    static Path createMp3WithMissingTags(Path directory, String filename) throws Exception {
        Path file = createMinimalMp3(directory, filename);
        AudioFile audioFile = AudioFileIO.read(file.toFile());
        var tag = new ID3v24Tag();
        tag.setField(FieldKey.ARTIST, "Partial Artist");
        tag.setField(FieldKey.TITLE, "Partial Title");
        audioFile.setTag(tag);
        audioFile.commit();
        return file;
    }

    static Path createMinimalMp3(Path directory, String filename) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(filename);

        try (OutputStream os = Files.newOutputStream(file)) {
            // Minimal valid MP3: MPEG1 Layer 3, 128kbps, 44100Hz, stereo
            // Frame header: 0xFF 0xFB 0x90 0x00
            byte[] frameHeader = {(byte) 0xFF, (byte) 0xFB, (byte) 0x90, 0x00};
            // A single MP3 frame at 128kbps is 417 bytes (header + padding)
            byte[] frameData = new byte[413]; // 417 - 4 header bytes
            // Write a few frames so JAudiotagger considers it valid
            for (int i = 0; i < 5; i++) {
                os.write(frameHeader);
                os.write(frameData);
            }
        }
        return file;
    }

    static Path createNonAudioFile(Path directory, String filename) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(filename);
        Files.writeString(file, "not audio content");
        return file;
    }
}
