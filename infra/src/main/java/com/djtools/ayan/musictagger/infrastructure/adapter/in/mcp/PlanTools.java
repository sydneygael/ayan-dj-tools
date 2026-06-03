package com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp;

import com.djtools.ayan.musictagger.domain.model.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class PlanTools {

    private final AyanMusicTools tools;

    public PlanTools(AyanMusicTools tools) {
        this.tools = tools;
    }

    @Tool(description = "Crée un plan de modifications de tags pour une liste de fichiers audio. Scanne, détecte les manquants, enrichit via Spotify.")
    public TaggingPlan createPlanForFiles(
            @ToolParam(description = "Liste des chemins absolus des fichiers audio") List<String> filePaths) {
        return tools.createPlanForFiles(filePaths);
    }

    @Tool(description = "Exécute un plan approuvé — écrit les tags dans les fichiers audio et retourne le résultat")
    public BatchApplyResult applyTagsPlan(
            @ToolParam(description = "Identifiant du plan approuvé") String planId) {
        return tools.applyTagsPlan(planId);
    }

    @Tool(description = "Traite le prochain fichier du plan en mode MANUAL — retourne l'operation courante")
    public TagOperation processNextFile(
            @ToolParam(description = "Identifiant du plan en cours") String planId) {
        return tools.processNextFile(planId);
    }

    @Tool(description = "Retourne l'historique des modifications de tags pour un plan donné")
    public List<TaggingHistoryEntry> getTaggingHistory(
            @ToolParam(description = "Identifiant du plan") String planId) {
        return tools.getTaggingHistory(planId);
    }

    @Tool(description = "Parcourt un dossier et retourne ses fichiers audio et sous-dossiers, paginés. Commencer avec page=0.")
    public FileBrowserPage browseFiles(
            @ToolParam(description = "Chemin absolu du dossier à parcourir") String directoryPath,
            @ToolParam(description = "Numéro de page, commence à 0") int page,
            @ToolParam(description = "Nombre d'entrées par page (1–50, recommandé : 20)") int pageSize) throws IOException {
        return tools.browseFiles(directoryPath, page, pageSize);
    }

    @Tool(description = "Prévisualise les modifications de tags avant application sur un fichier")
    public TagPreview previewTagUpdate(
            @ToolParam(description = "Chemin absolu du fichier audio") String filepath,
            @ToolParam(description = "Tags à appliquer (clé: nom du tag, valeur: nouvelle valeur)") Map<String, String> tags) {
        return tools.previewTagUpdate(filepath, tags);
    }
}
