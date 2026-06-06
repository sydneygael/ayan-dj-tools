package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.SimilarTrackResult;
import com.djtools.ayan.musictagger.infrastructure.service.TrackVectorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rag")
@Tag(name = "RAG", description = "Recherche sémantique par similarité (Vector Store Qdrant)")
public class RagController {

    private final TrackVectorizationService vectorizationService;

    public RagController(TrackVectorizationService vectorizationService) {
        this.vectorizationService = vectorizationService;
    }

    @Operation(
        summary = "Recherche sémantique de tracks similaires",
        description = "Calcule un embedding vectoriel de la requête libre via `nomic-embed-text` (Ollama), puis interroge Qdrant pour trouver les tracks les plus proches par similarité cosinus (seuil ≥ 0.7). Chaque `SimilarTrackResult` contient les métadonnées de la track et son score de similarité."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste de tracks similaires triée par score décroissant")
    })
    @GetMapping("/similar")
    public List<SimilarTrackResult> findSimilar(
            @Parameter(description = "Requête textuelle libre : artiste, genre, BPM, tonalité, thème…", example = "dark techno 135bpm minor")
            @RequestParam String query,
            @Parameter(description = "Nombre maximum de résultats retournés (défaut 5, max configuré à 5 dans application.yml)")
            @RequestParam(defaultValue = "5") int limit) {
        return vectorizationService.findSimilarTracks(query, limit);
    }
}
