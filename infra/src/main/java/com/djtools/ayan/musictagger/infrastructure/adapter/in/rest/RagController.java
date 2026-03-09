package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.SimilarTrackResult;
import com.djtools.ayan.musictagger.infrastructure.service.TrackVectorizationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final TrackVectorizationService vectorizationService;

    public RagController(TrackVectorizationService vectorizationService) {
        this.vectorizationService = vectorizationService;
    }

    @GetMapping("/similar")
    public List<SimilarTrackResult> findSimilar(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        return vectorizationService.findSimilarTracks(query, limit);
    }
}
