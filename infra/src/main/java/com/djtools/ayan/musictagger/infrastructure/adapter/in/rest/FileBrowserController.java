package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import com.djtools.ayan.musictagger.domain.model.FileBrowserPage;
import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.in.AudioFileReader;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.domain.usecase.ScanMusicUseCase;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.audio.AudioScannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/files")
class FileBrowserController {

    private static final Logger log = LoggerFactory.getLogger(FileBrowserController.class);
    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^(.+?)\\s*[-–—]\\s*(.+?)\\.[a-zA-Z0-9]+$");
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_PARALLEL_HTTP_WORK = 4;

    private final AudioScannerService audioScannerService;
    private final AudioFileReader audioFileReader;
    private final ScanMusicUseCase scanMusicUseCase;
    private final MusicMetadataProvider musicMetadataProvider;

    FileBrowserController(AudioScannerService audioScannerService,
                          AudioFileReader audioFileReader,
                          ScanMusicUseCase scanMusicUseCase,
                          MusicMetadataProvider musicMetadataProvider) {
        this.audioScannerService = audioScannerService;
        this.audioFileReader = audioFileReader;
        this.scanMusicUseCase = scanMusicUseCase;
        this.musicMetadataProvider = musicMetadataProvider;
    }

    @GetMapping("/browse")
    FileBrowserPage browse(@RequestParam(defaultValue = "") String path,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size) throws IOException {
        final var resolvedPath = path.isBlank() ? System.getProperty("user.home") : path;
        if (resolvedPath.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chemin invalide : '..' non autorise");
        }
        try {
            final var clampedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
            final var clampedPage = Math.max(page, 0);
            return audioScannerService.browse(Path.of(resolvedPath), clampedPage, clampedSize);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    record AnalyzeRequest(List<String> filePaths) {}

    record FileAnalysisItem(String filepath,
                            String filename,
                            Map<String, String> currentTags,
                            List<String> missingTags) {}

    @PostMapping("/analyze")
    List<FileAnalysisItem> analyze(@RequestBody AnalyzeRequest request) {
        if (request.filePaths() == null || request.filePaths().isEmpty()) {
            return List.of();
        }
        final var inputs = sanitizePaths(request.filePaths());
        return runInParallel(inputs, this::analyzeOne).stream()
                .filter(Objects::nonNull)
                .toList();
    }

    record EnrichRequest(List<String> filePaths) {}

    record FileEnrichItem(String filepath,
                          String filename,
                          String status,
                          String message,
                          EnrichedTrackMetadata metadata) {}

    @PostMapping("/enrich")
    List<FileEnrichItem> enrich(@RequestBody EnrichRequest request) {
        if (request.filePaths() == null || request.filePaths().isEmpty()) {
            return List.of();
        }
        final var inputs = sanitizePaths(request.filePaths());
        return runInParallel(inputs, this::enrichOne);
    }

    private FileAnalysisItem analyzeOne(String path) {
        try {
            final var infoOpt = audioFileReader.readTags(new Filepath(path));
            if (infoOpt.isEmpty()) {
                return null;
            }
            final var info = infoOpt.get();
            final var missing = scanMusicUseCase.detectMissingTags(new Filepath(path)).missingTags();
            return new FileAnalysisItem(path, info.filename(), extractCurrentTags(info), missing);
        } catch (Exception e) {
            log.warn("Analyze failed for '{}': {}", path, e.getMessage());
            return null;
        }
    }

    private FileEnrichItem enrichOne(String path) {
        try {
            final var infoOpt = audioFileReader.readTags(new Filepath(path));
            if (infoOpt.isEmpty()) {
                return new FileEnrichItem(path, path, "ERROR", "Fichier illisible", null);
            }

            final var info = infoOpt.get();
            final var artist = info.artist() != null ? info.artist() : suggestArtist(info.filename());
            final var title = info.title() != null ? info.title() : suggestTitle(info.filename());
            if (artist == null || title == null) {
                return new FileEnrichItem(
                        path,
                        info.filename(),
                        "ERROR",
                        "Impossible de determiner artiste/titre depuis le nom de fichier",
                        null
                );
            }

            final var result = musicMetadataProvider.enrich(artist, title);
            return switch (result) {
                case EnrichmentResult.Success success ->
                        new FileEnrichItem(path, info.filename(), "SUCCESS", null, success.metadata());
                case EnrichmentResult.NotFound ignored ->
                        new FileEnrichItem(path, info.filename(), "NOT_FOUND",
                                "Introuvable sur Soundcharts : " + artist + " - " + title, null);
                case EnrichmentResult.Error error ->
                        new FileEnrichItem(path, info.filename(), "ERROR", error.message(), null);
            };
        } catch (Exception e) {
            log.warn("Enrichment failed for '{}': {}", path, e.getMessage());
            return new FileEnrichItem(path, path, "ERROR", e.getMessage(), null);
        }
    }

    private List<IndexedPath> sanitizePaths(List<String> rawPaths) {
        return java.util.stream.IntStream.range(0, rawPaths.size())
                .mapToObj(i -> new IndexedPath(i, rawPaths.get(i)))
                .filter(entry -> entry.path() != null && !entry.path().contains(".."))
                .toList();
    }

    private <T> List<T> runInParallel(List<IndexedPath> inputs, Function<String, T> task) {
        if (inputs.isEmpty()) {
            return List.of();
        }
        final var parallelism = Math.min(MAX_PARALLEL_HTTP_WORK, inputs.size());
        try (var executor = Executors.newFixedThreadPool(parallelism)) {
            final var futures = inputs.stream()
                    .map(entry -> CompletableFuture.supplyAsync(
                            () -> new IndexedValue<>(entry.index(), task.apply(entry.path())),
                            executor
                    ))
                    .toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .sorted(Comparator.comparingInt(IndexedValue::index))
                    .map(IndexedValue::value)
                    .toList();
        }
    }

    private static Map<String, String> extractCurrentTags(MusicFileInfo info) {
        final var tags = new LinkedHashMap<String, String>();
        if (info.artist() != null && !info.artist().isBlank()) tags.put("artist", info.artist());
        if (info.title() != null && !info.title().isBlank()) tags.put("title", info.title());
        if (info.album() != null && !info.album().isBlank()) tags.put("album", info.album());
        if (info.genre() != null && !info.genre().isBlank()) tags.put("genre", info.genre());
        if (info.bpm() != null && !info.bpm().isBlank()) tags.put("bpm", info.bpm());
        if (info.key() != null && !info.key().isBlank()) tags.put("key", info.key());
        return tags;
    }

    private static String suggestArtist(String filename) {
        final var matcher = FILENAME_PATTERN.matcher(filename.trim());
        return matcher.matches() ? matcher.group(1).trim() : null;
    }

    private static String suggestTitle(String filename) {
        final var matcher = FILENAME_PATTERN.matcher(filename.trim());
        if (matcher.matches()) return matcher.group(2).trim();
        final var dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex).trim() : filename.trim();
    }

    private record IndexedPath(int index, String path) {}

    private record IndexedValue<T>(int index, T value) {}
}
