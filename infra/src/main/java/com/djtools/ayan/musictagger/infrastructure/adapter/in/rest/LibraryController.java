package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;
import com.djtools.ayan.musictagger.domain.port.out.ScannedTrackRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final ScannedTrackRepository scannedTrackRepository;

    public LibraryController(ScannedTrackRepository scannedTrackRepository) {
        this.scannedTrackRepository = scannedTrackRepository;
    }

    @GetMapping
    public List<MusicFileInfo> findAll() {
        return scannedTrackRepository.findAll();
    }

    @DeleteMapping
    public void delete(@RequestParam String filepath) {
        scannedTrackRepository.delete(filepath);
    }
}
