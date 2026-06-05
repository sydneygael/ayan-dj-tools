package com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp;

import com.djtools.ayan.musictagger.domain.model.*;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Tools for PLANIFICATION intent — max 4 */
@Component
public class PlanTools {

    private final AyanMusicTools tools;

    public PlanTools(AyanMusicTools tools) {
        this.tools = tools;
    }

    @Tool("Crée un plan de modifications de tags pour une liste de fichiers : scanne, détecte les manquants, enrichit via Spotify")
    public TaggingPlan createPlanForFiles(@P("Liste des chemins absolus des fichiers audio") List<String> filePaths) {
        return tools.createPlanForFiles(filePaths);
    }

    @Tool("Exécute un plan approuvé — écrit les tags dans les fichiers audio et retourne le résultat")
    public BatchApplyResult applyTagsPlan(@P("Identifiant du plan approuvé") String planId) {
        return tools.applyTagsPlan(planId);
    }

    @Tool("Traite le prochain fichier du plan en mode MANUAL — retourne l'opération courante à confirmer")
    public TagOperation processNextFile(@P("Identifiant du plan en cours") String planId) {
        return tools.processNextFile(planId);
    }

    @Tool("Prévisualise les modifications de tags avant application — montre les valeurs actuelles vs proposées")
    public TagPreview previewTagUpdate(
            @P("Chemin absolu du fichier audio") String filepath,
            @P("Tags à appliquer (clé: nom du tag, valeur: nouvelle valeur)") Map<String, String> tags) {
        return tools.previewTagUpdate(filepath, tags);
    }
}
