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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/files")
class FileBrowserController {

    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^(.+?)\\s*[-–—]\\s*(.+?)\\.[a-zA-Z0-9]+$");

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

    // ─── Browse ──────────────────────────────────────────────────────────────

    @GetMapping("/browse")
    FileBrowserPage browse(
            @RequestParam(defaultValue = "") String path,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws IOException {
        String resolvedPath = path.isBlank() ? System.getProperty("user.home") : path;
        if (resolvedPath.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chemin invalide : '..' non autorisé");
        }
        try {
            int clampedSize = Math.min(Math.max(size, 1), 50);
            int clampedPage = Math.max(page, 0);
            return audioScannerService.browse(Path.of(resolvedPath), clampedPage, clampedSize);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // ─── Analyze ─────────────────────────────────────────────────────────────

    record AnalyzeRequest(List<String> filePaths) {}

    record FileAnalysisItem(String filepath, String filename,
                            Map<String, String> currentTags, List<String> missingTags) {}

    @PostMapping("/analyze")
    List<FileAnalysisItem> analyze(@RequestBody AnalyzeRequest request) {
        if (request.filePaths() == null || request.filePaths().isEmpty()) {
            return List.of();
        }
        return request.filePaths().stream()
                .filter(p -> p != null && !p.contains(".."))
                .map(p -> {
                    var infoOpt = audioFileReader.readTags(new Filepath(p));
                    if (infoOpt.isEmpty()) return null;
                    var info = infoOpt.get();
                    var missing = scanMusicUseCase.detectMissingTags(new Filepath(p)).missingTags();
                    return new FileAnalysisItem(p, info.filename(), extractCurrentTags(info), missing);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    // ─── Enrich ──────────────────────────────────────────────────────────────

    record EnrichRequest(List<String> filePaths) {}

    record FileEnrichItem(String filepath, String filename,
                          String status, String message,
                          EnrichedTrackMetadata metadata) {}

    @PostMapping("/enrich")
    List<FileEnrichItem> enrich(@RequestBody EnrichRequest request) {
        if (request.filePaths() == null || request.filePaths().isEmpty()) {
            return List.of();
        }
        return request.filePaths().stream()
                .filter(p -> p != null && !p.contains(".."))
                .map(p -> {
                    var infoOpt = audioFileReader.readTags(new Filepath(p));
                    if (infoOpt.isEmpty()) {
                        return new FileEnrichItem(p, p, "ERROR", "Fichier illisible", null);
                    }
                    var info = infoOpt.get();
                    var artist = info.artist() != null ? info.artist() : suggestArtist(info.filename());
                    var title  = info.title()  != null ? info.title()  : suggestTitle(info.filename());
                    if (artist == null || title == null) {
                        return new FileEnrichItem(p, info.filename(), "ERROR",
                                "Impossible de déterminer artiste/titre depuis le nom de fichier", null);
                    }
                    var result = musicMetadataProvider.enrich(artist, title);
                    return switch (result) {
                        case EnrichmentResult.Success s ->
                            new FileEnrichItem(p, info.filename(), "SUCCESS", null, s.metadata());
                        case EnrichmentResult.NotFound _ ->
                            new FileEnrichItem(p, info.filename(), "NOT_FOUND",
                                    "Introuvable sur Spotify : " + artist + " – " + title, null);
                        case EnrichmentResult.Error e ->
                            new FileEnrichItem(p, info.filename(), "ERROR", e.message(), null);
                    };
                })
                .toList();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static Map<String, String> extractCurrentTags(MusicFileInfo info) {
        var tags = new LinkedHashMap<String, String>();
        if (info.artist()  != null && !info.artist().isBlank())  tags.put("artist",  info.artist());
        if (info.title()   != null && !info.title().isBlank())   tags.put("title",   info.title());
        if (info.album()   != null && !info.album().isBlank())   tags.put("album",   info.album());
        if (info.genre()   != null && !info.genre().isBlank())   tags.put("genre",   info.genre());
        if (info.bpm()     != null && !info.bpm().isBlank())     tags.put("bpm",     info.bpm());
        if (info.key()     != null && !info.key().isBlank())     tags.put("key",     info.key());
        return tags;
    }

    private static String suggestArtist(String filename) {
        var m = FILENAME_PATTERN.matcher(filename.trim());
        return m.matches() ? m.group(1).trim() : null;
    }

    private static String suggestTitle(String filename) {
        var m = FILENAME_PATTERN.matcher(filename.trim());
        if (m.matches()) return m.group(2).trim();
        var dotIdx = filename.lastIndexOf('.');
        return dotIdx > 0 ? filename.substring(0, dotIdx).trim() : filename.trim();
    }
}
