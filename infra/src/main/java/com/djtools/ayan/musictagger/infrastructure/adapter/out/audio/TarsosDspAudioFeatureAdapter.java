package com.djtools.ayan.musictagger.infrastructure.adapter.out.audio;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.in.AudioFeatureExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

@Component
public class TarsosDspAudioFeatureAdapter implements AudioFeatureExtractor {

    private static final Logger log = LoggerFactory.getLogger(TarsosDspAudioFeatureAdapter.class);

    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 2048;
    private static final int OVERLAP = 1024;

    private static final String[] PITCH_CLASSES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    @Override
    public Optional<AudioFeatures> extract(Filepath filepath) {
        File file = new File(filepath.value());
        if (!file.exists() || !file.isFile()) {
            log.warn("File does not exist or is not a file: {}", filepath.value());
            return Optional.empty();
        }

        try {
            final var bpm = detectBpm(file);
            final var keyResult = detectKey(file);
            final var energy = detectEnergy(file);

            return Optional.of(new AudioFeatures(
                    null,
                    energy,
                    null,
                    null,
                    null,
                    null,
                    bpm,
                    keyResult != null ? keyResult.key : null,
                    keyResult != null ? keyResult.mode : null,
                    null,
                    null,
                    null
            ));
        } catch (Exception e) {
            log.error("Failed to extract audio features from: {}", filepath.value(), e);
            return Optional.empty();
        }
    }

    private Double detectBpm(File file) throws Exception {
        final var dispatcher = createDispatcher(file);
        final var onsetTimestamps = new ArrayList<Double>();

        final var onsetDetector = new ComplexOnsetDetector(BUFFER_SIZE);
        onsetDetector.setHandler((time, salience) -> onsetTimestamps.add(time));
        dispatcher.addAudioProcessor(onsetDetector);
        dispatcher.run();

        if (onsetTimestamps.size() < 2) {
            return null;
        }

        final var intervals = new ArrayList<Double>();
        for (int i = 1; i < onsetTimestamps.size(); i++) {
            final var interval = onsetTimestamps.get(i) - onsetTimestamps.get(i - 1);
            if (interval > 0.2 && interval < 2.0) {
                intervals.add(interval);
            }
        }

        if (intervals.isEmpty()) {
            return null;
        }

        Collections.sort(intervals);
        final var medianInterval = intervals.get(intervals.size() / 2);
        double bpm = 60.0 / medianInterval;

        if (bpm < 60) bpm *= 2;
        if (bpm > 200) bpm /= 2;

        return Math.round(bpm * 10.0) / 10.0;
    }

    private KeyResult detectKey(File file) throws Exception {
        final var dispatcher = createDispatcher(file);
        final var pitchClassHistogram = new int[12];

        PitchDetectionHandler handler = (PitchDetectionResult result, AudioEvent event) -> {
            float pitch = result.getPitch();
            if (pitch > 0) {
                int midiNote = (int) Math.round(69 + 12 * Math.log(pitch / 440.0) / Math.log(2));
                int pitchClass = ((midiNote % 12) + 12) % 12;
                pitchClassHistogram[pitchClass]++;
            }
        };

        dispatcher.addAudioProcessor(new PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.YIN, SAMPLE_RATE, BUFFER_SIZE, handler));
        dispatcher.run();

        final var totalDetections = Arrays.stream(pitchClassHistogram).sum();
        if (totalDetections == 0) {
            return null;
        }

        int dominantPitch = 0;
        for (int i = 1; i < 12; i++) {
            if (pitchClassHistogram[i] > pitchClassHistogram[dominantPitch]) {
                dominantPitch = i;
            }
        }

        final var majorThird = (dominantPitch + 4) % 12;
        final var minorThird = (dominantPitch + 3) % 12;
        final var mode = pitchClassHistogram[majorThird] >= pitchClassHistogram[minorThird] ? "Major" : "Minor";

        return new KeyResult(PITCH_CLASSES[dominantPitch], mode);
    }

    private Double detectEnergy(File file) throws Exception {
        final var dispatcher = createDispatcher(file);
        final var rmsValues = new ArrayList<Double>();

        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                rmsValues.add((double) audioEvent.getRMS());
                return true;
            }

            @Override
            public void processingFinished() {}
        });
        dispatcher.run();

        if (rmsValues.isEmpty()) {
            return null;
        }

        final var avgRms = rmsValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return Math.min(1.0, avgRms * 3.0);
    }

    private AudioDispatcher createDispatcher(File file) throws Exception {
        final var name = file.getName().toLowerCase();
        if (name.endsWith(".wav")) {
            return AudioDispatcherFactory.fromPipe(file.getAbsolutePath(), SAMPLE_RATE, BUFFER_SIZE, OVERLAP);
        }
        return AudioDispatcherFactory.fromPipe(file.getAbsolutePath(), SAMPLE_RATE, BUFFER_SIZE, OVERLAP);
    }

    private record KeyResult(String key, String mode) {}
}
