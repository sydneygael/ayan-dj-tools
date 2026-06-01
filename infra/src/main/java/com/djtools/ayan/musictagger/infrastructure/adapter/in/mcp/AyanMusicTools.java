package com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.in.AudioFeatureExtractor;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.domain.port.out.AudioFeaturesCacheRepository;
import com.djtools.ayan.musictagger.domain.usecase.ScanMusicUseCase;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.audio.AudioScannerService;
import com.djtools.ayan.musictagger.infrastructure.service.ManualModeService;
import com.djtools.ayan.musictagger.infrastructure.service.PlanManagementService;
import com.djtools.ayan.musictagger.infrastructure.service.PlaylistService;
import com.djtools.ayan.musictagger.infrastructure.service.MusicLookupResult;
import com.djtools.ayan.musictagger.infrastructure.service.MusicLookupService;
import com.djtools.ayan.musictagger.infrastructure.service.SongSearchService;
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
    private final PlaylistService playlistService;
    private final SongSearchService songSearchService;
    private final MusicLookupService musicLookupService;

    public AyanMusicTools(ScanMusicUseCase scanMusicUseCase,
                          MusicMetadataProvider musicMetadataProvider,
                          AudioFeatureExtractor audioFeatureExtractor,
                          PlanManagementService planManagementService,
                          ManualModeService manualModeService,
                          TrackVectorizationService vectorizationService,
                          AudioFeaturesCacheRepository audioFeaturesCache,
                          AudioScannerService audioScannerService,
                          PlaylistService playlistService,
                          SongSearchService songSearchService,
                          MusicLookupService musicLookupService) {
        this.scanMusicUseCase = scanMusicUseCase;
        this.musicMetadataProvider = musicMetadataProvider;
        this.audioFeatureExtractor = audioFeatureExtractor;
        this.planManagementService = planManagementService;
        this.manualModeService = manualModeService;
        this.vectorizationService = vectorizationService;
        this.audioFeaturesCache = audioFeaturesCache;
        this.audioScannerService = audioScannerService;
        this.playlistService = playlistService;
        this.songSearchService = songSearchService;
        this.musicLookupService = musicLookupService;
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

    @Tool(description = "Génère une playlist de loop mixing (technique 3/4) à partir de la collection : "
            + "morceaux dans une plage de BPM, sélectionnés par similarité sémantique (RAG) et danceability.")
    public Playlist generateLoopMixingPlaylist(
            @ToolParam(description = "BPM minimum") int bpmMin,
            @ToolParam(description = "BPM maximum") int bpmMax,
            @ToolParam(description = "Genre principal (optionnel, peut être vide)") String genre) {
        return playlistService.generateLoopMixingPlaylist(bpmMin, bpmMax, genre);
    }

    @Tool(description = "Génère une playlist mixée harmoniquement via la roue de Camelot (« Mixed in Key ») : "
            + "chaque transition reste dans une tonalité compatible et privilégie un écart de ±6 BPM. "
            + "Retourne les morceaux ordonnés avec leur clé Camelot, le type de transition et des statistiques.")
    public HarmonicPlaylist generateHarmonicMixedPlaylist(
            @ToolParam(description = "Genre principal (optionnel, peut être vide)") String genre,
            @ToolParam(description = "BPM minimum") int minBpm,
            @ToolParam(description = "BPM maximum") int maxBpm,
            @ToolParam(description = "Énergie cible 0.0–1.0") double targetEnergy,
            @ToolParam(description = "Nombre de morceaux souhaité") int count) {
        return playlistService.generateHarmonicPlaylist(minBpm, maxBpm, genre, targetEnergy, count);
    }

    @Tool(description = "Recherche des informations musicales sur un artiste, un album ou un morceau "
            + "en interrogeant des sources externes dans l'ordre : Soundcharts → Internet → Spotify. "
            + "À utiliser pour les questions de découverte externe (« qui est l'artiste X ? », "
            + "« quels morceaux a sorti Y ? », « donne-moi des infos sur cet album »). "
            + "Ne pas confondre avec searchSongs qui cherche dans la bibliothèque locale de l'utilisateur. "
            + "Si rien n'est trouvé, répond simplement qu'aucun résultat n'a été trouvé. "
            + "En cas d'erreur d'une source, passe silencieusement à la suivante.")
    public MusicLookupResult lookupMusicInfo(
            @ToolParam(description = "Requête libre : nom d'artiste, titre, album ou combinaison") String query,
            @ToolParam(required = false, description = "Nom de l'artiste (si connu séparément de la requête)") String artist,
            @ToolParam(required = false, description = "Titre du morceau (si connu séparément)") String song,
            @ToolParam(required = false, description = "Nom de l'album (si connu séparément)") String album) {
        return musicLookupService.lookup(buildLookupQuery(query, artist, song, album));
    }

    private String buildLookupQuery(String query, String artist, String song, String album) {
        if (query != null && !query.isBlank()) return query.trim();
        final var sb = new StringBuilder();
        if (artist != null && !artist.isBlank()) sb.append(artist.trim()).append(' ');
        if (song != null && !song.isBlank()) sb.append(song.trim()).append(' ');
        if (album != null && !album.isBlank()) sb.append(album.trim()).append(' ');
        return sb.toString().trim();
    }

    @Tool(description = "Recherche des morceaux dans la collection à partir de critères donnés en langage naturel "
            + "(genre, plage de BPM, niveau d'énergie, plage d'années, ambiance). "
            + "Combine recherche sémantique (RAG) et filtrage par critères. "
            + "Retourne par défaut 10 morceaux classés par pertinence, avec la raison de chaque correspondance "
            + "et un résumé des critères appliqués. "
            + "À utiliser quand l'utilisateur demande des morceaux par critères plutôt que par fichier précis. "
            + "Tous les critères sont optionnels : ne renseigne que ceux mentionnés par l'utilisateur.")
    public SongSearchResult searchSongs(
            @ToolParam(required = false, description = "Genre musical (ex: house, techno, afrobeats)") String genre,
            @ToolParam(required = false, description = "Ambiance ou vibe en texte libre (ex: dark, festif, mélancolique)") String mood,
            @ToolParam(required = false, description = "BPM minimum") Integer bpmMin,
            @ToolParam(required = false, description = "BPM maximum") Integer bpmMax,
            @ToolParam(required = false, description = "Énergie minimum, 0.0–1.0") Double energyMin,
            @ToolParam(required = false, description = "Énergie maximum, 0.0–1.0") Double energyMax,
            @ToolParam(required = false, description = "Année de sortie minimum") Integer yearMin,
            @ToolParam(required = false, description = "Année de sortie maximum") Integer yearMax,
            @ToolParam(required = false, description = "Nombre de morceaux souhaité (défaut 10, max 50)") Integer limit) {
        final var criteria = new SongSearchCriteria(
                genre, mood, bpmMin, bpmMax, energyMin, energyMax, yearMin, yearMax,
                limit == null ? 0 : limit);
        return songSearchService.search(criteria);
    }

}
