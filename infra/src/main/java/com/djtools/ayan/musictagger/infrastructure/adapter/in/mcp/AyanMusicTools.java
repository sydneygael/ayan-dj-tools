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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class AyanMusicTools {

    private static final Logger log = LoggerFactory.getLogger(AyanMusicTools.class);

    /** Tracks queries already executed within a single chatSync() call (same virtual thread). */
    private static final ThreadLocal<Set<String>> SEEN_LOOKUP_QUERIES =
            ThreadLocal.withInitial(HashSet::new);

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

    @Tool("Scanne un fichier audio et retourne ses tags actuels")
    public MusicFileInfo scanMusicFile(
            @P("Chemin absolu du fichier audio") String filepath) {
        log.info("[tool] scanMusicFile filepath={}", filepath);
        var path = new Filepath(filepath);
        final var results = scanMusicUseCase.execute(List.of(path));
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Impossible de lire le fichier : " + filepath);
        }
        return results.getFirst();
    }

    @Tool("Détecte les tags manquants d'un fichier audio")
    public MissingTagsReport detectMissingTags(
            @P("Chemin absolu du fichier audio") String filepath) {
        log.info("[tool] detectMissingTags filepath={}", filepath);
        return scanMusicUseCase.detectMissingTags(new Filepath(filepath));
    }

    @Tool("Suggère artiste et titre à partir du nom de fichier (format 'Artiste - Titre.ext')")
    public TagSuggestion suggestTagsFromFilename(
            @P("Nom du fichier audio") String filename) {
        log.info("[tool] suggestTagsFromFilename filename={}", filename);
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
     * Résultat d'un enrichissement retourné au LLM.
     * Status explicite pour que l'agent puisse toujours formuler une réponse claire à l'utilisateur.
     */
    record SpotifyEnrichmentResponse(String status, String message, EnrichedTrackMetadata metadata) {}

    @Tool("Enrichit les métadonnées via Soundcharts et analyse audio locale, puis indexe dans le vector store")
    public SpotifyEnrichmentResponse enrichWithSpotify(
            @P("Nom de l'artiste") String artist,
            @P("Titre du morceau") String title) {
        log.info("[tool] enrichWithSpotify artist='{}' title='{}'", artist, title);
        final var result = musicMetadataProvider.enrich(artist, title);
        if (result instanceof EnrichmentResult.Error err) {
            log.warn("[tool] enrichWithSpotify ERROR '{}' – '{}': {}", artist, title, err.message());
            return new SpotifyEnrichmentResponse(
                    "ERROR",
                    "Erreur lors de l'enrichissement pour « %s – %s » : %s".formatted(artist, title, err.message()),
                    null);
        }
        if (result instanceof EnrichmentResult.NotFound) {
            log.info("[tool] enrichWithSpotify NOT_FOUND '{}' – '{}'", artist, title);
            return new SpotifyEnrichmentResponse(
                    "NOT_FOUND",
                    "Aucun résultat trouvé sur Soundcharts pour « %s – %s ».".formatted(artist, title),
                    null);
        }
        final var data = result.data();
        vectorizationService.store(data);
        if (data.audioFeatures() != null) {
            audioFeaturesCache.save(data.artist() + " - " + data.title(), data.audioFeatures());
        }
        log.info("[tool] enrichWithSpotify OK '{}' – '{}' | album='{}' genres={} BPM={} key={}",
                data.artist(), data.title(), data.album(), data.genres(),
                data.audioFeatures() != null ? data.audioFeatures().bpm() : "—",
                data.audioFeatures() != null ? data.audioFeatures().fullKey() : "—");
        return new SpotifyEnrichmentResponse(
                "SUCCESS",
                "Enrichissement réussi : album=%s, genres=%s, BPM=%s, tonalité=%s".formatted(
                        data.album(),
                        data.genres(),
                        data.audioFeatures() != null ? data.audioFeatures().bpm() : "—",
                        data.audioFeatures() != null ? data.audioFeatures().fullKey() : "—"),
                data);
    }

    @Tool("Recherche des morceaux similaires dans la collection vectorisée (RAG)")
    public List<SimilarTrackResult> findSimilarTracks(
            @P("Requête de recherche (artiste, genre, ambiance, etc.)") String query,
            @P("Nombre max de résultats") int limit) {
        log.info("[tool] findSimilarTracks query='{}' limit={}", query, limit);
        return vectorizationService.findSimilarTracks(query, limit);
    }

    @Tool("Suggestions intelligentes de tags basées sur Soundcharts + morceaux similaires (RAG)")
    public SmartTagSuggestion smartSuggestTags(
            @P("Chemin absolu du fichier audio") String filepath) {
        log.info("[tool] smartSuggestTags filepath={}", filepath);
        return vectorizationService.smartSuggestTags(filepath);
    }

    @Tool("Crée un plan de modifications de tags pour une liste de fichiers audio. " +
            "Scanne chaque fichier, détecte les tags manquants, enrichit via Soundcharts, " +
            "et retourne un plan avec toutes les modifications suggérées.")
    public TaggingPlan createPlanForFiles(
            @P("Liste des chemins absolus des fichiers audio") List<String> filePaths) {
        log.info("[tool] createPlanForFiles files={}", filePaths.size());
        return planManagementService.createPlan(filePaths);
    }

    @Tool("Exécute un plan approuvé — écrit les tags dans les fichiers audio et retourne le résultat")
    public BatchApplyResult applyTagsPlan(
            @P("Identifiant du plan approuvé") String planId) {
        log.info("[tool] applyTagsPlan planId={}", planId);
        return planManagementService.executePlan(planId);
    }

    @Tool("Prévisualise les modifications de tags avant application sur un fichier")
    public TagPreview previewTagUpdate(
            @P("Chemin absolu du fichier audio") String filepath,
            @P("Tags à appliquer (clé: nom du tag, valeur: nouvelle valeur)") java.util.Map<String, String> tags) {
        log.info("[tool] previewTagUpdate filepath={} tags={}", filepath, tags.keySet());
        return planManagementService.previewFile(filepath, tags);
    }

    @Tool("Traite le prochain fichier du plan en mode MANUAL — retourne l'operation courante")
    public TagOperation processNextFile(
            @P("Identifiant du plan en cours") String planId) {
        log.info("[tool] processNextFile planId={}", planId);
        return manualModeService.prepareNextFile(planId);
    }

    @Tool("Retourne l'historique des modifications de tags pour un plan donné")
    public List<TaggingHistoryEntry> getTaggingHistory(
            @P("Identifiant du plan") String planId) {
        log.info("[tool] getTaggingHistory planId={}", planId);
        return planManagementService.getPlanHistory(planId);
    }

    @Tool("""
            Parcourt un dossier et retourne ses fichiers audio et sous-dossiers, paginés.
            Les dossiers apparaissent en premier, triés alphabétiquement, puis les fichiers audio.
            Pour chaque fichier audio, retourne ses tags actuels (artiste, titre, album, genre).
            Commencer avec page=0. Si totalPages > 1, appeler à nouveau avec page suivante pour voir la suite.
            Utiliser pour explorer la bibliothèque musicale de l'utilisateur avant de créer un plan.
            """)
    public FileBrowserPage browseFiles(
            @P("Chemin absolu du dossier à parcourir") String directoryPath,
            @P("Numéro de page, commence à 0") int page,
            @P("Nombre d'entrées par page (1–50, recommandé : 20)") int pageSize) throws IOException {
        log.info("[tool] browseFiles dir='{}' page={} pageSize={}", directoryPath, page, pageSize);
        final var clampedSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        final var clampedPage = Math.max(page, 0);
        return audioScannerService.browse(Path.of(directoryPath), clampedPage, clampedSize);
    }

    @Tool("Génère une playlist de loop mixing (technique 3/4) à partir de la collection : "
            + "morceaux dans une plage de BPM, sélectionnés par similarité sémantique (RAG) et danceability.")
    public Playlist generateLoopMixingPlaylist(
            @P("BPM minimum") int bpmMin,
            @P("BPM maximum") int bpmMax,
            @P("Genre principal (optionnel, peut être vide)") String genre) {
        log.info("[tool] generateLoopMixingPlaylist bpm={}-{} genre='{}'", bpmMin, bpmMax, genre);
        return playlistService.generateLoopMixingPlaylist(bpmMin, bpmMax, genre);
    }

    @Tool("Génère une playlist mixée harmoniquement via la roue de Camelot (« Mixed in Key ») : "
            + "chaque transition reste dans une tonalité compatible et privilégie un écart de ±6 BPM. "
            + "Retourne les morceaux ordonnés avec leur clé Camelot, le type de transition et des statistiques.")
    public HarmonicPlaylist generateHarmonicMixedPlaylist(
            @P("Genre principal (optionnel, peut être vide)") String genre,
            @P("BPM minimum") int minBpm,
            @P("BPM maximum") int maxBpm,
            @P("Énergie cible 0.0–1.0") double targetEnergy,
            @P("Nombre de morceaux souhaité") int count) {
        log.info("[tool] generateHarmonicMixedPlaylist bpm={}-{} genre='{}' energy={} count={}", minBpm, maxBpm, genre, targetEnergy, count);
        return playlistService.generateHarmonicPlaylist(minBpm, maxBpm, genre, targetEnergy, count);
    }

    @Tool("Recherche des informations musicales sur un artiste, un album ou un morceau "
            + "via Internet → Soundcharts → Spotify. "
            + "Retourne un résumé textuel complet — ne pas appeler d'autres outils après. "
            + "À utiliser pour les questions de découverte externe (« qui est X ? », « infos sur Y »).")
    public String lookupMusicInfo(
            @P("Requête libre : nom d'artiste, titre, album ou combinaison") String query,
            @P("Nom de l'artiste si connu séparément — optionnel") String artist,
            @P("Titre du morceau si connu séparément — optionnel") String song,
            @P("Nom de l'album si connu séparément — optionnel") String album) {
        final var effectiveQuery = buildLookupQuery(query, artist, song, album);
        log.info("[tool] lookupMusicInfo query='{}'", effectiveQuery);

        final var key = effectiveQuery.toLowerCase().strip();
        if (!SEEN_LOOKUP_QUERIES.get().add(key)) {
            log.warn("[tool] lookupMusicInfo duplicate call for '{}' — stopping loop", effectiveQuery);
            return "Recherche déjà effectuée pour \"" + effectiveQuery
                    + "\". Utilise les informations précédemment retournées pour formuler ta réponse directement.";
        }

        final var result = musicLookupService.lookup(effectiveQuery);
        return formatLookupResult(result, effectiveQuery);
    }

    private static String formatLookupResult(
            com.djtools.ayan.musictagger.infrastructure.service.MusicLookupResult result,
            String query) {
        return result.toSummary();
    }

    private static String buildLookupQuery(String query, String artist, String song, String album) {
        if (query != null && !query.isBlank()) return query.trim();
        final var sb = new StringBuilder();
        if (artist != null && !artist.isBlank()) sb.append(artist.trim()).append(' ');
        if (song != null && !song.isBlank()) sb.append(song.trim()).append(' ');
        if (album != null && !album.isBlank()) sb.append(album.trim()).append(' ');
        return sb.toString().trim();
    }

    @Tool("Recherche des morceaux dans la collection à partir de critères donnés en langage naturel "
            + "(genre, plage de BPM, niveau d'énergie, plage d'années, ambiance). "
            + "Combine recherche sémantique (RAG) et filtrage par critères. "
            + "Retourne par défaut 10 morceaux classés par pertinence, avec la raison de chaque correspondance "
            + "et un résumé des critères appliqués. "
            + "À utiliser quand l'utilisateur demande des morceaux par critères plutôt que par fichier précis. "
            + "Tous les critères sont optionnels : ne renseigne que ceux mentionnés par l'utilisateur.")
    public SongSearchResult searchSongs(
            @P("Genre musical (ex: house, techno, afrobeats)") String genre,
            @P("Ambiance ou vibe en texte libre (ex: dark, festif, mélancolique)") String mood,
            @P("BPM minimum") Integer bpmMin,
            @P("BPM maximum") Integer bpmMax,
            @P("Énergie minimum, 0.0–1.0") Double energyMin,
            @P("Énergie maximum, 0.0–1.0") Double energyMax,
            @P("Année de sortie minimum") Integer yearMin,
            @P("Année de sortie maximum") Integer yearMax,
            @P("Nombre de morceaux souhaité (défaut 10, max 50)") Integer limit) {
        log.info("[tool] searchSongs genre='{}' mood='{}' bpm={}-{} energy={}-{} year={}-{} limit={}",
                genre, mood, bpmMin, bpmMax, energyMin, energyMax, yearMin, yearMax, limit);
        final var criteria = new SongSearchCriteria(
                genre, mood, bpmMin, bpmMax, energyMin, energyMax, yearMin, yearMax,
                limit == null ? 10 : limit);
        return songSearchService.search(criteria);
    }

}
