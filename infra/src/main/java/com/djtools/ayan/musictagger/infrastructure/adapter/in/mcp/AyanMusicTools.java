package com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.in.AudioFeatureExtractor;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.domain.port.out.AudioFeaturesCacheRepository;
import com.djtools.ayan.musictagger.domain.usecase.ScanMusicUseCase;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.audio.AudioScannerService;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.web.WebSearchAdapter;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.web.WebSearchResult;
import com.djtools.ayan.musictagger.infrastructure.service.ManualModeService;
import com.djtools.ayan.musictagger.infrastructure.service.PlanManagementService;
import com.djtools.ayan.musictagger.infrastructure.service.TrackVectorizationService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class AyanMusicTools {

    private static final Pattern ARTIST_TITLE_PATTERN = Pattern.compile(
            "^(.+?)\\s*[-–—]\\s*(.+?)\\.[a-zA-Z0-9]+$"
    );

    private static final int MAX_PAGE_SIZE = 50;

    private final ScanMusicUseCase scanMusicUseCase;
    private final MusicMetadataProvider musicMetadataProvider;
    private final AudioFeatureExtractor audioFeatureExtractor;
    private final PlanManagementService planManagementService;
    private final ManualModeService manualModeService;
    private final TrackVectorizationService vectorizationService;
    private final AudioFeaturesCacheRepository audioFeaturesCache;
    private final AudioScannerService audioScannerService;
    private final WebSearchAdapter webSearchAdapter;

    public AyanMusicTools(ScanMusicUseCase scanMusicUseCase,
                          MusicMetadataProvider musicMetadataProvider,
                          AudioFeatureExtractor audioFeatureExtractor,
                          PlanManagementService planManagementService,
                          ManualModeService manualModeService,
                          TrackVectorizationService vectorizationService,
                          AudioFeaturesCacheRepository audioFeaturesCache,
                          AudioScannerService audioScannerService,
                          WebSearchAdapter webSearchAdapter) {
        this.scanMusicUseCase = scanMusicUseCase;
        this.musicMetadataProvider = musicMetadataProvider;
        this.audioFeatureExtractor = audioFeatureExtractor;
        this.planManagementService = planManagementService;
        this.manualModeService = manualModeService;
        this.vectorizationService = vectorizationService;
        this.audioFeaturesCache = audioFeaturesCache;
        this.audioScannerService = audioScannerService;
        this.webSearchAdapter = webSearchAdapter;
    }

    @Tool(description = "Scanne un fichier audio et retourne ses tags actuels")
    public MusicFileInfo scanMusicFile(
            @ToolParam(description = "Chemin absolu du fichier audio") String filepath) {
        var path = new Filepath(filepath);
        final var results = scanMusicUseCase.execute(List.of(path));
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
        final var matcher = ARTIST_TITLE_PATTERN.matcher(filename.trim());
        if (matcher.matches()) {
            return new TagSuggestion(matcher.group(1).trim(), matcher.group(2).trim());
        }
        final var nameWithoutExt = filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf('.'))
                : filename;
        return new TagSuggestion(null, nameWithoutExt.trim());
    }

    /**
     * Résultat d'un appel enrichWithSpotify retourné au LLM.
     * Status explicite pour que l'agent puisse toujours formuler une réponse claire à l'utilisateur.
     */
    record SpotifyEnrichmentResponse(String status, String message, EnrichedTrackMetadata metadata) {}

    @Tool(description = "Enrichit les métadonnées via Spotify et analyse audio locale, puis indexe dans le vector store")
    public SpotifyEnrichmentResponse enrichWithSpotify(
            @ToolParam(description = "Nom de l'artiste") String artist,
            @ToolParam(description = "Titre du morceau") String title) {
        final var result = musicMetadataProvider.enrich(artist, title);
        if (result instanceof EnrichmentResult.Error err) {
            return new SpotifyEnrichmentResponse(
                    "ERROR",
                    "Erreur lors de l'enrichissement Spotify pour « %s – %s » : %s".formatted(artist, title, err.message()),
                    null);
        }
        if (result instanceof EnrichmentResult.NotFound) {
            return new SpotifyEnrichmentResponse(
                    "NOT_FOUND",
                    "Aucun résultat trouvé sur Spotify pour « %s – %s ».".formatted(artist, title),
                    null);
        }
        final var data = result.data();
        vectorizationService.store(data);
        if (data.audioFeatures() != null) {
            audioFeaturesCache.save(data.artist() + " - " + data.title(), data.audioFeatures());
        }
        return new SpotifyEnrichmentResponse(
                "SUCCESS",
                "Enrichissement réussi : album=%s, genres=%s, BPM=%s, tonalité=%s".formatted(
                        data.album(),
                        data.genres(),
                        data.audioFeatures() != null ? data.audioFeatures().bpm() : "—",
                        data.audioFeatures() != null ? data.audioFeatures().fullKey() : "—"),
                data);
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

    @Tool(description = "Traite le prochain fichier du plan en mode MANUAL — retourne l'operation courante")
    public TagOperation processNextFile(
            @ToolParam(description = "Identifiant du plan en cours") String planId) {
        return manualModeService.prepareNextFile(planId);
    }

    @Tool(description = "Retourne l'historique des modifications de tags pour un plan donné")
    public List<TaggingHistoryEntry> getTaggingHistory(
            @ToolParam(description = "Identifiant du plan") String planId) {
        return planManagementService.getPlanHistory(planId);
    }

    @Tool(description = """
            Parcourt un dossier et retourne ses fichiers audio et sous-dossiers, paginés.
            Les dossiers apparaissent en premier, triés alphabétiquement, puis les fichiers audio.
            Pour chaque fichier audio, retourne ses tags actuels (artiste, titre, album, genre).
            Commencer avec page=0. Si totalPages > 1, appeler à nouveau avec page suivante pour voir la suite.
            Utiliser pour explorer la bibliothèque musicale de l'utilisateur avant de créer un plan.
            """)
    public FileBrowserPage browseFiles(
            @ToolParam(description = "Chemin absolu du dossier à parcourir") String directoryPath,
            @ToolParam(description = "Numéro de page, commence à 0") int page,
            @ToolParam(description = "Nombre d'entrées par page (1–50, recommandé : 20)") int pageSize) throws IOException {
        final var clampedSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        final var clampedPage = Math.max(page, 0);
        return audioScannerService.browse(Path.of(directoryPath), clampedPage, clampedSize);
    }

    @Tool(description = """
            Recherche des informations sur un artiste, un album ou un morceau sur le web.
            Utile quand Spotify ne trouve pas de résultat ou pour compléter les informations
            (biographie, discographie, date de sortie, label, contexte culturel, etc.).
            Retourne un résumé et plusieurs sources. Utilise les résultats pour enrichir les tags.
            """)
    public WebSearchResult searchWeb(
            @ToolParam(description = "Requête de recherche, ex : 'Angélique Kidjo Agolo album 1994'") String query) {
        return webSearchAdapter.search(query);
    }
}
