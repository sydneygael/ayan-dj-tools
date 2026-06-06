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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Files", description = "Navigation système de fichiers, analyse et enrichissement de fichiers audio")
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

    @Operation(
        summary = "Naviguer dans le système de fichiers",
        description = "Parcourt un répertoire et retourne les fichiers audio (mp3, flac, wav, aiff, m4a, ogg) et sous-répertoires avec pagination. `path` doit être un chemin absolu accessible au processus serveur. Sans `path`, retourne le répertoire home de l'utilisateur courant."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page de navigation avec fichiers et sous-répertoires"),
        @ApiResponse(responseCode = "400", description = "Chemin invalide (contient '..') ou répertoire inaccessible")
    })
    @GetMapping("/browse")
    FileBrowserPage browse(
            @Parameter(description = "Chemin absolu du répertoire à parcourir. Défaut : home de l'utilisateur.", example = "/home/user/music")
            @RequestParam(defaultValue = "") String path,
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page (1–50, défaut 20)") @RequestParam(defaultValue = "20") int size)
            throws IOException {
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

    @Schema(description = "Requête d'analyse de tags")
    record AnalyzeRequest(
            @Schema(description = "Chemins absolus des fichiers audio à analyser") List<String> filePaths
    ) {}

    @Schema(description = "Résultat d'analyse des tags d'un fichier")
    record FileAnalysisItem(
            @Schema(description = "Chemin absolu du fichier") String filepath,
            @Schema(description = "Nom du fichier (sans chemin)") String filename,
            @Schema(description = "Tags actuellement présents dans le fichier (clé → valeur)") Map<String, String> currentTags,
            @Schema(description = "Liste des tags absents ou vides (artist, title, album, genre, bpm, key…)") List<String> missingTags
    ) {}

    @Operation(
        summary = "Analyser les tags d'une liste de fichiers",
        description = "Lit les tags ID3/Vorbis actuels de chaque fichier et détecte les champs manquants. Exécuté en parallèle (4 threads max). Les fichiers illisibles sont silencieusement ignorés."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des analyses — un item par fichier lisible")
    })
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

    @Schema(description = "Requête d'enrichissement via Soundcharts")
    record EnrichRequest(
            @Schema(description = "Chemins absolus des fichiers audio à enrichir") List<String> filePaths
    ) {}

    @Schema(description = "Résultat d'enrichissement d'un fichier")
    record FileEnrichItem(
            @Schema(description = "Chemin absolu du fichier") String filepath,
            @Schema(description = "Nom du fichier (sans chemin)") String filename,
            @Schema(description = "Statut de l'enrichissement : SUCCESS, NOT_FOUND, ERROR") String status,
            @Schema(description = "Message d'erreur si status ≠ SUCCESS", nullable = true) String message,
            @Schema(description = "Métadonnées enrichies depuis Soundcharts (null si status ≠ SUCCESS)", nullable = true)
            EnrichedTrackMetadata metadata
    ) {}

    @Operation(
        summary = "Enrichir les métadonnées via Soundcharts",
        description = "Interroge l'API Soundcharts pour chaque fichier et retourne les métadonnées disponibles (genres, BPM, tonalité, label, pays, popularité, ISRC…). L'artiste et le titre sont extraits des tags ID3 existants, ou déduits du nom de fichier ('Artiste - Titre.ext'). Exécuté en parallèle (4 threads max)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des résultats — status SUCCESS / NOT_FOUND / ERROR par fichier")
    })
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
