package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.FileBrowserPage;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.audio.AudioScannerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
class FileBrowserController {

    private final AudioScannerService audioScannerService;

    FileBrowserController(AudioScannerService audioScannerService) {
        this.audioScannerService = audioScannerService;
    }

    @GetMapping("/browse")
    FileBrowserPage browse(
            @RequestParam String path,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws IOException {
        if (path.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chemin invalide : '..' non autorisé");
        }
        try {
            int clampedSize = Math.min(Math.max(size, 1), 50);
            int clampedPage = Math.max(page, 0);
            return audioScannerService.browse(Path.of(path), clampedPage, clampedSize);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
