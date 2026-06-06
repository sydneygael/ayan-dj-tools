package com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp;

import com.djtools.ayan.musictagger.domain.model.*;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Tools for FICHIERS intent — max 4 */
@Component
public class FileOpsTools {

    private final AyanMusicTools tools;

    public FileOpsTools(AyanMusicTools tools) {
        this.tools = tools;
    }

    @Tool("Scanne un fichier audio et retourne ses tags actuels (artiste, titre, album, genre, BPM, tonalité)")
    public MusicFileInfo scanMusicFile(@P("Chemin absolu du fichier audio") String filepath) {
        return tools.scanMusicFile(filepath);
    }

    @Tool("Détecte les tags manquants d'un fichier audio")
    public MissingTagsReport detectMissingTags(@P("Chemin absolu du fichier audio") String filepath) {
        return tools.detectMissingTags(filepath);
    }

    @Tool("Enrichit les métadonnées via Soundcharts (album, genres, BPM, tonalité, popularité) et indexe dans la collection")
    public AyanMusicTools.SpotifyEnrichmentResponse enrichWithSpotify(
            @P("Nom de l'artiste") String artist,
            @P("Titre du morceau") String title) {
        return tools.enrichWithSpotify(artist, title);
    }

    @Tool("Parcourt un dossier et retourne ses fichiers audio et sous-dossiers, paginés. Commencer avec page=0.")
    public FileBrowserPage browseFiles(
            @P("Chemin absolu du dossier à parcourir") String directoryPath,
            @P("Numéro de page, commence à 0") int page,
            @P("Nombre d'entrées par page (1–50, recommandé : 20)") int pageSize) throws IOException {
        return tools.browseFiles(directoryPath, page, pageSize);
    }
}
