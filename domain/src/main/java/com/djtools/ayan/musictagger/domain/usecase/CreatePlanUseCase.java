package com.djtools.ayan.musictagger.domain.usecase;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.in.AudioFileReader;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Crée un plan de tagging à partir d'une liste de fichiers audio.
 *
 * Étapes : lecture des tags existants → détection des tags manquants
 * → suggestion depuis le nom de fichier → enrichissement Spotify → construction du plan.
 */
public class CreatePlanUseCase {

    private static final java.util.regex.Pattern FILENAME_PATTERN =
            java.util.regex.Pattern.compile("^(.+?)\\s*[-–—]\\s*(.+?)\\.[a-zA-Z0-9]+$");

    private final AudioFileReader audioFileReader;
    private final MusicMetadataProvider metadataProvider;

    public CreatePlanUseCase(AudioFileReader audioFileReader, MusicMetadataProvider metadataProvider) {
        this.audioFileReader = audioFileReader;
        this.metadataProvider = metadataProvider;
    }

    /** Point d'entrée : génère un TaggingPlan pour les fichiers donnés. */
    public TaggingPlan execute(String planId, List<Filepath> paths) {
        List<TagOperation> operations = new ArrayList<>();
        int filesWithMissing = 0;

        for (Filepath path : paths) {
            Optional<MusicFileInfo> infoOpt = audioFileReader.readTags(path);
            if (infoOpt.isEmpty()) {
                continue;
            }

            MusicFileInfo info = infoOpt.get();
            if (hasAnyMissingTag(info)) {
                filesWithMissing++;
            }

            operations.add(buildOperation(path, info));
        }

        return buildPlan(planId, operations, paths.size(), filesWithMissing);
    }

    // --- Construction d'une opération par fichier ---

    /** Construit une TagOperation : tags actuels, suggestions, enrichissement. */
    private TagOperation buildOperation(Filepath path, MusicFileInfo info) {
        Map<String, String> currentTags = extractCurrentTags(info);
        Map<String, String> suggestedTags = new LinkedHashMap<>();

        // Étape 1 : tenter de deviner artiste/titre depuis le nom de fichier
        String artist = info.artist();
        String title = info.title();
        if (!info.hasArtistAndTitle()) {
            TagSuggestion suggestion = suggestFromFilename(info.filename());
            artist = applySuggestion(suggestion.artist(), artist, "artist", suggestedTags);
            title = applySuggestion(suggestion.title(), title, "title", suggestedTags);
        }

        // Étape 2 : enrichir via Spotify si artiste et titre connus
        var message = enrichFromSpotify(artist, title, info, suggestedTags);

        return new TagOperation(path.value(), currentTags, suggestedTags, OperationStatus.PENDING, message);
    }

    /** Applique une suggestion de tag si non null, et l'ajoute aux suggestedTags. */
    private String applySuggestion(String suggested, String current, String tagName, Map<String, String> suggestedTags) {
        if (suggested != null) {
            suggestedTags.put(tagName, suggested);
            return suggested;
        }
        return current;
    }

    /** Enrichit les tags manquants via Spotify. Retourne un message d'erreur ou null. */
    private String enrichFromSpotify(String artist, String title, MusicFileInfo info, Map<String, String> suggestedTags) {
        if (artist == null || title == null) {
            return null;
        }

        EnrichmentResult result = metadataProvider.enrich(artist, title);
        if (result.isSuccess()) {
            fillMissingTagsFromMetadata(info, result.data(), suggestedTags);
            return null;
        }
        if (result instanceof EnrichmentResult.NotFound) {
            return "Aucun résultat Spotify trouvé pour %s - %s".formatted(artist, title);
        }
        if (result instanceof EnrichmentResult.Error(String errorMsg)) {
            return "Erreur enrichissement : " + errorMsg;
        }
        return null;
    }

    // --- Extraction et enrichissement des tags ---

    /** Extrait les tags déjà présents dans le fichier audio. */
    private Map<String, String> extractCurrentTags(MusicFileInfo info) {
        Map<String, String> tags = new LinkedHashMap<>();
        if (isPresent(info.artist())) tags.put("artist", info.artist());
        if (isPresent(info.title())) tags.put("title", info.title());
        if (isPresent(info.album())) tags.put("album", info.album());
        if (isPresent(info.genre())) tags.put("genre", info.genre());
        if (isPresent(info.bpm())) tags.put("bpm", info.bpm());
        if (isPresent(info.key())) tags.put("key", info.key());
        return tags;
    }

    /** Complète les suggestedTags avec les données Spotify pour chaque tag manquant. */
    private void fillMissingTagsFromMetadata(MusicFileInfo info, EnrichedTrackMetadata metadata, Map<String, String> suggestedTags) {
        if (info.isMissingTag("album") && isPresent(metadata.album())) {
            suggestedTags.put("album", metadata.album());
        }
        if (info.isMissingTag("genre") && !metadata.genres().isEmpty()) {
            suggestedTags.put("genre", String.join(", ", metadata.genres()));
        }
        if (info.isMissingTag("bpm") && hasBpm(metadata)) {
            suggestedTags.put("bpm", String.valueOf(metadata.audioFeatures().bpm().intValue()));
        }
        if (info.isMissingTag("key") && hasKey(metadata)) {
            suggestedTags.put("key", metadata.audioFeatures().fullKey());
        }
        // putIfAbsent : ne pas écraser les suggestions venant du nom de fichier
        if (info.isMissingTag("artist") && isPresent(metadata.artist())) {
            suggestedTags.putIfAbsent("artist", metadata.artist());
        }
        if (info.isMissingTag("title") && isPresent(metadata.title())) {
            suggestedTags.putIfAbsent("title", metadata.title());
        }
    }

    // --- Suggestion depuis le nom de fichier ---

    /** Parse "Artiste - Titre.mp3" → TagSuggestion. Sinon, titre = nom sans extension. */
    private TagSuggestion suggestFromFilename(String filename) {
        var matcher = FILENAME_PATTERN.matcher(filename.trim());
        if (matcher.matches()) {
            return new TagSuggestion(matcher.group(1).trim(), matcher.group(2).trim());
        }
        String nameWithoutExt = filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf('.'))
                : filename;
        return new TagSuggestion(null, nameWithoutExt.trim());
    }

    // --- Helpers ---

    private boolean hasAnyMissingTag(MusicFileInfo info) {
        return !info.hasArtistAndTitle()
                || info.isMissingTag("album")
                || info.isMissingTag("genre")
                || info.isMissingTag("bpm")
                || info.isMissingTag("key");
    }

    private boolean hasBpm(EnrichedTrackMetadata metadata) {
        return metadata.audioFeatures() != null && metadata.audioFeatures().bpm() != null;
    }

    private boolean hasKey(EnrichedTrackMetadata metadata) {
        return metadata.audioFeatures() != null && isPresent(metadata.audioFeatures().fullKey());
    }

    private TaggingPlan buildPlan(String planId, List<TagOperation> operations, int totalFiles, int filesWithMissing) {
        var status = operations.isEmpty() ? PlanStatus.DRAFT : PlanStatus.READY_FOR_REVIEW;
        return new TaggingPlan(planId, operations, LocalDateTime.now(), status, totalFiles, filesWithMissing);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
