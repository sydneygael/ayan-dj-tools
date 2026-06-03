package com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp;

import com.djtools.ayan.musictagger.domain.model.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class FileOpsTools {

    private final AyanMusicTools tools;

    public FileOpsTools(AyanMusicTools tools) {
        this.tools = tools;
    }

    @Tool(description = "Scanne un fichier audio et retourne ses tags actuels")
    public MusicFileInfo scanMusicFile(
            @ToolParam(description = "Chemin absolu du fichier audio") String filepath) {
        return tools.scanMusicFile(filepath);
    }

    @Tool(description = "Détecte les tags manquants d'un fichier audio")
    public MissingTagsReport detectMissingTags(
            @ToolParam(description = "Chemin absolu du fichier audio") String filepath) {
        return tools.detectMissingTags(filepath);
    }

    @Tool(description = "Suggère artiste et titre à partir du nom de fichier (format 'Artiste - Titre.ext')")
    public TagSuggestion suggestTagsFromFilename(
            @ToolParam(description = "Nom du fichier audio") String filename) {
        return tools.suggestTagsFromFilename(filename);
    }

    @Tool(description = "Enrichit les métadonnées via Spotify et analyse audio locale, puis indexe dans le vector store")
    public AyanMusicTools.SpotifyEnrichmentResponse enrichWithSpotify(
            @ToolParam(description = "Nom de l'artiste") String artist,
            @ToolParam(description = "Titre du morceau") String title) {
        return tools.enrichWithSpotify(artist, title);
    }

    @Tool(description = "Prévisualise les modifications de tags avant application sur un fichier")
    public TagPreview previewTagUpdate(
            @ToolParam(description = "Chemin absolu du fichier audio") String filepath,
            @ToolParam(description = "Tags à appliquer (clé: nom du tag, valeur: nouvelle valeur)") Map<String, String> tags) {
        return tools.previewTagUpdate(filepath, tags);
    }

    @Tool(description = "Suggestions intelligentes de tags basées sur Spotify + morceaux similaires (RAG)")
    public SmartTagSuggestion smartSuggestTags(
            @ToolParam(description = "Chemin absolu du fichier audio") String filepath) {
        return tools.smartSuggestTags(filepath);
    }

    @Tool(description = "Parcourt un dossier et retourne ses fichiers audio et sous-dossiers, paginés. Commencer avec page=0.")
    public FileBrowserPage browseFiles(
            @ToolParam(description = "Chemin absolu du dossier à parcourir") String directoryPath,
            @ToolParam(description = "Numéro de page, commence à 0") int page,
            @ToolParam(description = "Nombre d'entrées par page (1–50, recommandé : 20)") int pageSize) throws IOException {
        return tools.browseFiles(directoryPath, page, pageSize);
    }
}
