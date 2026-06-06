package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.infrastructure.service.PlaylistExportService;
import com.djtools.ayan.musictagger.infrastructure.service.PlaylistExportService.TrackExportEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Playlist", description = "Génération de playlists (loop-mixing, harmonique Camelot, thématique) et export M3U")
class PlaylistExportController {

    private final PlaylistExportService exportService;

    PlaylistExportController(PlaylistExportService exportService) {
        this.exportService = exportService;
    }

    @Schema(description = "Track à inclure dans l'export M3U")
    record ExportTrack(
            @Schema(description = "Nom de l'artiste", example = "Daft Punk") String artist,
            @Schema(description = "Titre de la track", example = "Around the World") String title,
            @Schema(description = "Durée en millisecondes (pour #EXTINF)", example = "428000") Long durationMs
    ) {}

    @Schema(description = "Corps de la requête d'export M3U")
    record ExportRequest(
            @Schema(description = "Nom de la playlist (utilisé pour le nom du fichier téléchargé)", example = "Set Berlin 2026")
            String playlistName,
            @Schema(description = "Liste ordonnée des tracks à exporter")
            List<ExportTrack> tracks
    ) {}

    @Operation(
        summary = "Exporter une playlist au format M3U",
        description = "Génère un fichier M3U8 (Extended M3U) téléchargeable avec entête `#EXTM3U`, une entrée `#EXTINF` par track (durée en secondes + 'Artiste - Titre'), et le chemin de fichier ou titre. Retourné avec `Content-Disposition: attachment`."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fichier M3U (text/plain;charset=UTF-8) avec Content-Disposition attachment")
    })
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
