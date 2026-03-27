package com.djtools.ayan.musictagger.infrastructure.adapter.out.audio;

import com.djtools.ayan.musictagger.domain.model.FileBrowserPage;
import com.djtools.ayan.musictagger.domain.model.FileEntry;
import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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

    /** Scan récursif d'un dossier — retourne tous les fichiers audio trouvés avec leurs tags. */
    public List<MusicFileInfo> scanDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Not a directory: " + directory);
        }

        log.info("Scanning directory: {}", directory);

        try (Stream<Path> walk = Files.walk(directory)) {
            final var results = walk
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

    /**
     * Parcourt un dossier (non-récursif) et retourne une page de résultats.
     * Chaque entrée est soit un sous-dossier, soit un fichier audio avec ses tags.
     * Les entrées sont triées : dossiers d'abord, puis fichiers, par ordre alphabétique.
     */
    public FileBrowserPage browse(Path directory, int page, int pageSize) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Not a directory: " + directory);
        }

        final List<Path> entries;
        try (final var stream = Files.list(directory)) {
            entries = stream
                    .filter(p -> Files.isDirectory(p) || isSupportedExtension(p))
                    .sorted(Comparator
                            .comparing((Path p) -> Files.isDirectory(p) ? 0 : 1)
                            .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                    .toList();
        }

        final var total = entries.size();
        final var totalPages = total == 0 ? 1 : (total + pageSize - 1) / pageSize;
        final var from = Math.min(page * pageSize, total);
        final var to = Math.min(from + pageSize, total);

        final var pageEntries = entries.subList(from, to).stream()
                .map(this::toFileEntry)
                .toList();

        log.debug("browse({}, page={}, size={}) → {}/{} entries", directory, page, pageSize, pageEntries.size(), total);
        return new FileBrowserPage(directory.toString(), page, pageSize, total, totalPages, pageEntries);
    }

    private FileEntry toFileEntry(Path path) {
        final var name = path.getFileName().toString();
        final var absolutePath = path.toString();

        if (Files.isDirectory(path)) {
            return new FileEntry(name, absolutePath, true, 0L, null, null, null, null, false);
        }

        final var sizeBytes = getFileSizeQuietly(path);
        return audioTaggerAdapter.readTags(new Filepath(absolutePath))
                .map(info -> new FileEntry(
                        name, absolutePath, false, sizeBytes,
                        info.artist(), info.title(), info.album(), info.genre(),
                        info.hasArtistAndTitle() && isPresent(info.album()) && isPresent(info.genre())
                ))
                .orElse(new FileEntry(name, absolutePath, false, sizeBytes, null, null, null, null, false));
    }

    private long getFileSizeQuietly(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }

    private boolean isSupportedExtension(Path path) {
        final var filename = path.getFileName().toString();
        final var dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return false;
        }
        final var ext = filename.substring(dotIndex + 1).toLowerCase();
        return supportedExtensions.contains(ext);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
