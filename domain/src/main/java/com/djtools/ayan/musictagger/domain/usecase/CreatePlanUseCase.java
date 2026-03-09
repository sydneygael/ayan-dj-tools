package com.djtools.ayan.musictagger.domain.usecase;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.in.AudioFileReader;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;

import java.time.LocalDateTime;
import java.util.*;

public class CreatePlanUseCase {

    private final AudioFileReader audioFileReader;
    private final MusicMetadataProvider metadataProvider;

    public CreatePlanUseCase(AudioFileReader audioFileReader, MusicMetadataProvider metadataProvider) {
        this.audioFileReader = audioFileReader;
        this.metadataProvider = metadataProvider;
    }

    public TaggingPlan execute(String planId, List<Filepath> paths) {
        List<TagOperation> operations = new ArrayList<>();
        int filesWithMissing = 0;

        for (Filepath path : paths) {
            Optional<MusicFileInfo> infoOpt = audioFileReader.readTags(path);
            if (infoOpt.isEmpty()) {
                continue;
            }

            MusicFileInfo info = infoOpt.get();
            Map<String, String> currentTags = extractCurrentTags(info);
            Map<String, String> suggestedTags = new LinkedHashMap<>();
            String message = null;

            boolean hasMissing = !info.hasArtistAndTitle()
                    || info.isMissingTag("album")
                    || info.isMissingTag("genre")
                    || info.isMissingTag("bpm")
                    || info.isMissingTag("key");

            if (hasMissing) {
                filesWithMissing++;
            }

            String artist = info.artist();
            String title = info.title();

            if (!info.hasArtistAndTitle()) {
                TagSuggestion suggestion = suggestFromFilename(info.filename());
                if (suggestion.artist() != null) {
                    artist = suggestion.artist();
                    suggestedTags.put("artist", artist);
                }
                if (suggestion.title() != null) {
                    title = suggestion.title();
                    suggestedTags.put("title", title);
                }
            }

            if (artist != null && title != null) {
                EnrichmentResult result = metadataProvider.enrich(artist, title);
                if (result.isSuccess()) {
                    EnrichedTrackMetadata metadata = result.data();
                    enrichSuggestedTags(info, metadata, suggestedTags);
                } else if (result instanceof EnrichmentResult.NotFound) {
                    message = "Aucun résultat Spotify trouvé pour %s - %s".formatted(artist, title);
                } else if (result instanceof EnrichmentResult.Error(String errorMsg)) {
                    message = "Erreur enrichissement : " + errorMsg;
                }
            }

            operations.add(new TagOperation(
                    path.value(),
                    currentTags,
                    suggestedTags,
                    OperationStatus.PENDING,
                    message
            ));
        }

        return new TaggingPlan(
                planId,
                operations,
                LocalDateTime.now(),
                operations.isEmpty() ? PlanStatus.DRAFT : PlanStatus.READY_FOR_REVIEW,
                paths.size(),
                filesWithMissing
        );
    }

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

    private void enrichSuggestedTags(MusicFileInfo info, EnrichedTrackMetadata metadata, Map<String, String> suggestedTags) {
        if (info.isMissingTag("album") && isPresent(metadata.album())) {
            suggestedTags.put("album", metadata.album());
        }
        if (info.isMissingTag("genre") && !metadata.genres().isEmpty()) {
            suggestedTags.put("genre", String.join(", ", metadata.genres()));
        }
        if (info.isMissingTag("bpm") && metadata.audioFeatures() != null && metadata.audioFeatures().bpm() != null) {
            suggestedTags.put("bpm", String.valueOf(metadata.audioFeatures().bpm().intValue()));
        }
        if (info.isMissingTag("key") && metadata.audioFeatures() != null && isPresent(metadata.audioFeatures().fullKey())) {
            suggestedTags.put("key", metadata.audioFeatures().fullKey());
        }
        if (info.isMissingTag("artist") && isPresent(metadata.artist())) {
            suggestedTags.putIfAbsent("artist", metadata.artist());
        }
        if (info.isMissingTag("title") && isPresent(metadata.title())) {
            suggestedTags.putIfAbsent("title", metadata.title());
        }
    }

    private TagSuggestion suggestFromFilename(String filename) {
        var matcher = java.util.regex.Pattern.compile("^(.+?)\\s*[-–—]\\s*(.+?)\\.[a-zA-Z0-9]+$")
                .matcher(filename.trim());
        if (matcher.matches()) {
            return new TagSuggestion(matcher.group(1).trim(), matcher.group(2).trim());
        }
        String nameWithoutExt = filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf('.'))
                : filename;
        return new TagSuggestion(null, nameWithoutExt.trim());
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
