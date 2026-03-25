package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.Playlist;
import com.djtools.ayan.musictagger.infrastructure.service.PlaylistService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/playlist")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    record GenerateRequest(Integer bpmMin, Integer bpmMax, String genre) {}

    @PostMapping("/generate")
    public Playlist generate(@RequestBody(required = false) GenerateRequest req) {
        int bpmMin = (req != null && req.bpmMin() != null) ? req.bpmMin() : 120;
        int bpmMax = (req != null && req.bpmMax() != null) ? req.bpmMax() : 145;
        String genre = (req != null && req.genre() != null) ? req.genre() : "";
        return playlistService.generateLoopMixingPlaylist(bpmMin, bpmMax, genre);
    }
}
