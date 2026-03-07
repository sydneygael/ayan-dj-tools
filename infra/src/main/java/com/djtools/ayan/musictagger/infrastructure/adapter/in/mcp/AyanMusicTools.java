package com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.in.AudioFeatureExtractor;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.domain.usecase.ScanMusicUseCase;
import com.djtools.ayan.musictagger.infrastructure.service.PlanManagementService;
import com.djtools.ayan.musictagger.infrastructure.service.TrackVectorizationService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AyanMusicTools {

    private static final Pattern ARTIST_TITLE_PATTERN = Pattern.compile(
            "^(.+?)\\s*[-–—]\\s*(.+?)\\.[a-zA-Z0-9]+$"
    );

    private final ScanMusicUseCase scanMusicUseCase;
    private final MusicMetadataProvider musicMetadataProvider;
    private final AudioFeatureExtractor audioFeatureExtractor;
    private final PlanManagementService planManagementService;
    private final TrackVectorizationService vectorizationService;

    public AyanMusicTools(ScanMusicUseCase scanMusicUseCase,
                          MusicMetadataProvider musicMetadataProvider,
                          AudioFeatureExtractor audioFeatureExtractor,
                          PlanManagementService planManagementService,
                          TrackVectorizationService vectorizationService) {
        this.scanMusicUseCase = scanMusicUseCase;
        this.musicMetadataProvider = musicMetadataProvider;
        this.audioFeatureExtractor = audioFeatureExtractor;
        this.planManagementService = planManagementService;
        this.vectorizationService = vectorizationService;
    }

    @Tool(description = "Scanne un fichier audio et retourne ses tags actuels")
    public MusicFileInfo scanMusicFile(
            @ToolParam(description = "Chemin absolu du fichier audio") String filepath) {
        var path = new Filepath(filepath);
        List<MusicFileInfo> results = scanMusicUseCase.execute(List.of(path));
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Impossible de lire le fichier : " + filepath);
        }
        return results.getFirst();
    }

    @Tool(description = "Détecte les tags manquants d'un fichier audio")
    public MissingTagsReport detectMissingTags(
            @ToolParam(description = "Chemin absolu du fichier audio") String filepath) {
        return scanMusicUseCase.detectMissingTags(new Filepath(filepath));
    }

    @Tool(description = "Suggère artiste et titre à partir du nom de fichier (format 'Artiste - Titre.ext')")
    public TagSuggestion suggestTagsFromFilename(
            @ToolParam(description = "Nom du fichier audio") String filename) {
        Matcher matcher = ARTIST_TITLE_PATTERN.matcher(filename.trim());
        if (matcher.matches()) {
            return new TagSuggestion(matcher.group(1).trim(), matcher.group(2).trim());
        }
        String nameWithoutExt = filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf('.'))
                : filename;
        return new TagSuggestion(null, nameWithoutExt.trim());
    }

    @Tool(description = "Enrichit les métadonnées via Spotify et analyse audio locale, puis indexe dans le vector store")
    public EnrichmentResult enrichWithSpotify(
            @ToolParam(description = "Nom de l'artiste") String artist,
            @ToolParam(description = "Titre du morceau") String title) {
        EnrichmentResult result = musicMetadataProvider.enrich(artist, title);
        if (result.isSuccess()) {
            vectorizationService.store(result.data());
        }
        return result;
    }

    @Tool(description = "Recherche des morceaux similaires dans la collection vectorisée (RAG)")
    public List<SimilarTrackResult> findSimilarTracks(
            @ToolParam(description = "Requête de recherche (artiste, genre, ambiance, etc.)") String query,
            @ToolParam(description = "Nombre max de résultats") int limit) {
        return vectorizationService.findSimilarTracks(query, limit);
    }

    @Tool(description = "Suggestions intelligentes de tags basées sur Spotify + morceaux similaires (RAG)")
    public SmartTagSuggestion smartSuggestTags(
            @ToolParam(description = "Chemin absolu du fichier audio") String filepath) {
        return vectorizationService.smartSuggestTags(filepath);
    }

    @Tool(description = "Crée un plan de modifications de tags pour une liste de fichiers audio. " +
            "Scanne chaque fichier, détecte les tags manquants, enrichit via Spotify, " +
            "et retourne un plan avec toutes les modifications suggérées.")
    public TaggingPlan createPlanForFiles(
            @ToolParam(description = "Liste des chemins absolus des fichiers audio") List<String> filePaths) {
        return planManagementService.createPlan(filePaths);
    }

    @Tool(description = "Exécute un plan approuvé — écrit les tags dans les fichiers audio et retourne le résultat")
    public BatchApplyResult applyTagsPlan(
            @ToolParam(description = "Identifiant du plan approuvé") String planId) {
        return planManagementService.executePlan(planId);
    }

    @Tool(description = "Prévisualise les modifications de tags avant application sur un fichier")
    public TagPreview previewTagUpdate(
            @ToolParam(description = "Chemin absolu du fichier audio") String filepath,
            @ToolParam(description = "Tags à appliquer (clé: nom du tag, valeur: nouvelle valeur)") java.util.Map<String, String> tags) {
        return planManagementService.previewFile(filepath, tags);
    }

    @Tool(description = "Retourne l'historique des modifications de tags pour un plan donné")
    public List<TaggingHistoryEntry> getTaggingHistory(
            @ToolParam(description = "Identifiant du plan") String planId) {
        return planManagementService.getPlanHistory(planId);
    }
}
