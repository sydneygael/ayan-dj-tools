package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;
import com.djtools.ayan.musictagger.domain.port.out.ScannedTrackRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library")
@Tag(name = "Library", description = "Bibliothèque des fichiers audio scannés (persistance PostgreSQL)")
public class LibraryController {

    private final ScannedTrackRepository scannedTrackRepository;

    public LibraryController(ScannedTrackRepository scannedTrackRepository) {
        this.scannedTrackRepository = scannedTrackRepository;
    }

    @Operation(
        summary = "Lister tous les fichiers de la bibliothèque",
        description = "Retourne tous les `MusicFileInfo` indexés dans PostgreSQL (table `scanned_tracks`). Les entrées sont persistées lors des scans et des enrichissements. Pas de pagination — réservé aux bibliothèques de taille raisonnable (< 10 000 fichiers)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste complète des fichiers scannés")
    })
    @GetMapping
    public List<MusicFileInfo> findAll() {
        return scannedTrackRepository.findAll();
    }

    @Operation(
        summary = "Supprimer un fichier de la bibliothèque",
        description = "Supprime l'entrée de la table `scanned_tracks` pour le chemin fourni. Le fichier audio lui-même n'est pas touché. Utile pour retirer un fichier déplacé ou supprimé du disque."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entrée supprimée de la bibliothèque")
    })
    @DeleteMapping
    public void delete(
            @Parameter(description = "Chemin absolu du fichier audio à retirer de la bibliothèque", example = "/music/Daft Punk - Around the World.mp3")
            @RequestParam String filepath) {
        scannedTrackRepository.delete(filepath);
    }
}
