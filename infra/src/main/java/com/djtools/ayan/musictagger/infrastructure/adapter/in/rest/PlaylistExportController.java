package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.infrastructure.service.PlaylistExportService;
import com.djtools.ayan.musictagger.infrastructure.service.PlaylistExportService.TrackExportEntry;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/playlist")
class PlaylistExportController {

    private final PlaylistExportService exportService;

    PlaylistExportController(PlaylistExportService exportService) {
        this.exportService = exportService;
    }

    record ExportTrack(String artist, String title, Long durationMs) {}
    record ExportRequest(String playlistName, List<ExportTrack> tracks) {}

    @PostMapping(value = "/export-m3u", produces = "text/plain;charset=UTF-8")
    ResponseEntity<String> exportM3u(@RequestBody ExportRequest request) {
        final var name   = PlaylistExportService.safeFilename(request.playlistName());
        final var tracks = request.tracks() != null ? request.tracks() : List.<ExportTrack>of();

        final var entries = tracks.stream()
                .map(t -> new TrackExportEntry(t.artist(), t.title(), t.durationMs()))
                .toList();

        final var content = exportService.buildM3uContent(entries);
        final var headers = downloadHeaders(name + ".m3u");
        return ResponseEntity.ok().headers(headers).body(content);
    }

    private static HttpHeaders downloadHeaders(String filename) {
        final var headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        return headers;
    }
}
