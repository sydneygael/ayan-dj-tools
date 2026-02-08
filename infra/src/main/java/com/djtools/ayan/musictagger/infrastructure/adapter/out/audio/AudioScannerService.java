package com.djtools.ayan.musictagger.infrastructure.adapter.out.audio;

import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class AudioScannerService {

    private static final Logger log = LoggerFactory.getLogger(AudioScannerService.class);

    private final JAudioTaggerAdapter audioTaggerAdapter;
    private final Set<String> supportedExtensions;

    public AudioScannerService(
            JAudioTaggerAdapter audioTaggerAdapter,
            @Value("${audio.supported-extensions}") List<String> supportedExtensions
    ) {
        this.audioTaggerAdapter = audioTaggerAdapter;
        this.supportedExtensions = Set.copyOf(supportedExtensions);
    }

    public List<MusicFileInfo> scanDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Not a directory: " + directory);
        }

        log.info("Scanning directory: {}", directory);

        try (Stream<Path> walk = Files.walk(directory)) {
            List<MusicFileInfo> results = walk
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedExtension)
                    .map(path -> new Filepath(path.toString()))
                    .map(audioTaggerAdapter::readTags)
                    .flatMap(java.util.Optional::stream)
                    .toList();

            log.info("Scanned {} audio files in {}", results.size(), directory);
            return results;
        }
    }

    private boolean isSupportedExtension(Path path) {
        String filename = path.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return false;
        }
        String ext = filename.substring(dotIndex + 1).toLowerCase();
        return supportedExtensions.contains(ext);
    }
}
