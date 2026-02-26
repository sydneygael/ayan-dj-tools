package com.djtools.ayan.musictagger.infrastructure.adapter.out.audio;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TarsosDspAudioFeatureAdapterTest {

    private TarsosDspAudioFeatureAdapter adapter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        adapter = new TarsosDspAudioFeatureAdapter();
    }

    @Test
    void shouldReturnEmptyForNonExistentFile() {
        var filepath = new Filepath("/nonexistent/file.wav");
        Optional<AudioFeatures> result = adapter.extract(filepath);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldExtractFeaturesFromSineWave() throws Exception {
        File wavFile = generateSineWav(440.0, 3.0);
        var filepath = new Filepath(wavFile.getAbsolutePath());

        Optional<AudioFeatures> result = adapter.extract(filepath);

        assertThat(result).isPresent();
        AudioFeatures features = result.get();
        assertThat(features.musicalKey()).isEqualTo("A");
        assertThat(features.energy()).isNotNull().isGreaterThan(0.0);
        assertThat(features.danceability()).isNull();
        assertThat(features.valence()).isNull();
        assertThat(features.acousticness()).isNull();
        assertThat(features.instrumentalness()).isNull();
        assertThat(features.speechiness()).isNull();
        assertThat(features.timeSignature()).isNull();
    }

    @Test
    void shouldHandleCorruptedFileGracefully() throws Exception {
        File corruptFile = tempDir.resolve("corrupt.wav").toFile();
        java.nio.file.Files.write(corruptFile.toPath(), new byte[]{0, 1, 2, 3, 4, 5});
        var filepath = new Filepath(corruptFile.getAbsolutePath());

        Optional<AudioFeatures> result = adapter.extract(filepath);

        // ffmpeg may decode corrupt files silently — result has all-null fields
        if (result.isPresent()) {
            AudioFeatures features = result.get();
            assertThat(features.bpm()).isNull();
            assertThat(features.musicalKey()).isNull();
            assertThat(features.energy()).isNull();
        }
    }

    private File generateSineWav(double frequency, double durationSeconds) throws Exception {
        float sampleRate = 44100;
        int numSamples = (int) (sampleRate * durationSeconds);
        byte[] data = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            double angle = 2.0 * Math.PI * frequency * i / sampleRate;
            short sample = (short) (Short.MAX_VALUE * 0.8 * Math.sin(angle));
            data[i * 2] = (byte) (sample & 0xFF);
            data[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }

        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        var bais = new java.io.ByteArrayInputStream(data);
        var audioStream = new AudioInputStream(bais, format, numSamples);

        File wavFile = tempDir.resolve("test_sine_440.wav").toFile();
        AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, wavFile);
        return wavFile;
    }
}
