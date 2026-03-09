package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.in.AudioFileReader;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.domain.port.out.VectorStorePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrackVectorizationService {

    private static final Logger log = LoggerFactory.getLogger(TrackVectorizationService.class);

    private final VectorStorePort vectorStorePort;
    private final AudioFileReader audioFileReader;
    private final MusicMetadataProvider musicMetadataProvider;

    public TrackVectorizationService(VectorStorePort vectorStorePort,
                                      AudioFileReader audioFileReader,
                                      MusicMetadataProvider musicMetadataProvider) {
        this.vectorStorePort = vectorStorePort;
        this.audioFileReader = audioFileReader;
        this.musicMetadataProvider = musicMetadataProvider;
    }

    public void store(EnrichedTrackMetadata track) {
        try {
            vectorStorePort.store(track);
        } catch (Exception e) {
            log.warn("Failed to store track in vector store: {} - {}", track.artist(), track.title(), e);
        }
    }

    public List<SimilarTrackResult> findSimilarTracks(String query, int limit) {
        return vectorStorePort.findSimilar(query, limit);
    }

    public SmartTagSuggestion smartSuggestTags(String filepath) {
        MusicFileInfo fileInfo = audioFileReader.readTags(new Filepath(filepath))
                .orElseThrow(() -> new IllegalArgumentException("Cannot read file: " + filepath));

        String artist = fileInfo.artist();
        String title = fileInfo.title();

        if ((artist == null || artist.isBlank()) && (title == null || title.isBlank())) {
            return new SmartTagSuggestion(filepath, Map.of(), List.of(), 0.0, "none");
        }

        String searchQuery = (artist != null ? artist : "") + " " + (title != null ? title : "");
        EnrichmentResult enrichment = musicMetadataProvider.enrich(
                artist != null ? artist : "",
                title != null ? title : ""
        );

        Map<String, String> suggestedTags = new LinkedHashMap<>();
        List<SimilarTrackResult> similarTracks = List.of();
        double confidence = 0.0;
        String source = "none";

        if (enrichment.isSuccess()) {
            EnrichedTrackMetadata metadata = enrichment.data();
            store(metadata);

            suggestedTags = buildTagsFromMetadata(metadata);
            confidence = 0.7;
            source = "spotify";

            similarTracks = vectorStorePort.findSimilar(searchQuery.trim(), 5);

            if (!similarTracks.isEmpty()) {
                Map<String, String> ragTags = aggregateGenresFromSimilar(similarTracks);
                ragTags.forEach(suggestedTags::putIfAbsent);
                confidence = Math.min(1.0, confidence + 0.1 * similarTracks.size());
                source = "spotify+rag";
            }
        } else {
            similarTracks = vectorStorePort.findSimilar(searchQuery.trim(), 5);

            if (!similarTracks.isEmpty()) {
                suggestedTags = aggregateGenresFromSimilar(similarTracks);
                confidence = 0.1 * similarTracks.size();
                source = "rag";
            }
        }

        return new SmartTagSuggestion(filepath, suggestedTags, similarTracks, Math.min(confidence, 1.0), source);
    }

    private Map<String, String> buildTagsFromMetadata(EnrichedTrackMetadata metadata) {
        var tags = new LinkedHashMap<String, String>();
        if (metadata.artist() != null) tags.put("artist", metadata.artist());
        if (metadata.title() != null) tags.put("title", metadata.title());
        if (metadata.album() != null) tags.put("album", metadata.album());
        if (!metadata.genres().isEmpty()) tags.put("genre", metadata.genres().getFirst());
        if (metadata.releaseYear() > 0) tags.put("year", String.valueOf(metadata.releaseYear()));

        AudioFeatures af = metadata.audioFeatures();
        if (af != null) {
            if (af.bpm() != null) tags.put("bpm", String.valueOf(af.bpm().intValue()));
            if (af.musicalKey() != null) tags.put("key", af.fullKey());
        }
        return tags;
    }

    private Map<String, String> aggregateGenresFromSimilar(List<SimilarTrackResult> similarTracks) {
        var genreVotes = new LinkedHashMap<String, Integer>();
        for (SimilarTrackResult result : similarTracks) {
            for (String genre : result.track().genres()) {
                genreVotes.merge(genre, 1, Integer::sum);
            }
        }

        var tags = new LinkedHashMap<String, String>();
        genreVotes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(1)
                .findFirst()
                .ifPresent(entry -> tags.put("genre", entry.getKey()));

        return tags;
    }
}
