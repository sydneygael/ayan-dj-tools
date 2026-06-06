package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.TagPreview;
import com.djtools.ayan.musictagger.domain.model.TagWriteResult;
import com.djtools.ayan.musictagger.domain.port.out.AudioFileWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tags")
@Tag(name = "Tags", description = "Application directe de tags sur fichiers audio")
public class TagController {

    private final AudioFileWriter audioFileWriter;

    public TagController(AudioFileWriter audioFileWriter) {
        this.audioFileWriter = audioFileWriter;
    }

    @Operation(
        summary = "Écrire des tags sur un fichier audio",
        description = "Applique les tags fournis directement au fichier audio (sans plan). Un backup est créé avant l'écriture ; en cas d'erreur le fichier original est restauré automatiquement. Formats supportés : MP3 (ID3v2.4), FLAC, WAV, AIFF, M4A, OGG."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Résultat de l'écriture avec statut SUCCESS ou ERROR")
    })
    @PostMapping("/apply")
    public TagWriteResult applyTags(@RequestBody ApplyTagsRequest request) {
        return audioFileWriter.writeTags(request.filepath(), request.tags());
    }

    @Operation(
        summary = "Prévisualiser les changements de tags",
        description = "Calcule le diff avant/après pour les tags fournis, sans modifier le fichier. Utile pour valider les valeurs avant d'appeler /apply."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Diff des tags : valeurs courantes vs valeurs proposées")
    })
    @PostMapping("/preview")
    public TagPreview previewTags(@RequestBody ApplyTagsRequest request) {
        return audioFileWriter.previewChanges(request.filepath(), request.tags());
    }

    @Schema(description = "Requête d'application de tags sur un fichier audio")
    public record ApplyTagsRequest(
            @Schema(description = "Chemin absolu du fichier audio", example = "/music/Daft Punk - Around the World.mp3")
            String filepath,
            @Schema(description = "Tags à écrire (clé → valeur). Clés reconnues : artist, title, album, genre, bpm, key, year, comment, label, isrc.",
                    example = "{\"artist\": \"Daft Punk\", \"bpm\": \"121\", \"key\": \"Am\"}")
            Map<String, String> tags
    ) {}
}
