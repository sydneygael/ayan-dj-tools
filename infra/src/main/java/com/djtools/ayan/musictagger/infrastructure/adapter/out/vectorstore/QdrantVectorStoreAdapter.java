package com.djtools.ayan.musictagger.infrastructure.adapter.out.vectorstore;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.SimilarTrackResult;
import com.djtools.ayan.musictagger.domain.port.out.VectorStorePort;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class QdrantVectorStoreAdapter implements VectorStorePort {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStoreAdapter.class);

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final double similarityThreshold;

    public QdrantVectorStoreAdapter(EmbeddingStore<TextSegment> embeddingStore,
                                     EmbeddingModel embeddingModel,
                                     @Value("${dj-tagger.rag.similarity-threshold:0.7}") double similarityThreshold) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.similarityThreshold = similarityThreshold;
    }

    @Override
    public void store(EnrichedTrackMetadata track) {
        final var text = buildEmbeddingText(track);
        final var metadata = buildMetadata(track);
        final var documentId = UUID.nameUUIDFromBytes(track.sourceId().getBytes()).toString();

        final var segment = TextSegment.from(text, dev.langchain4j.data.document.Metadata.from(metadata));
        final var embedding = embeddingModel.embed(text).content();
        // Remove existing entry to deduplicate by sourceId, then re-add
        embeddingStore.remove(documentId);
        embeddingStore.add(embedding, segment);

        log.debug("Stored track in vector store: {} - {} (id={})", track.artist(), track.title(), documentId);
    }

    @Override
    public List<SimilarTrackResult> findSimilar(String query, int limit) {
        final Embedding queryEmbedding = embeddingModel.embed(query).content();
        final var request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(limit)
                .minScore(similarityThreshold)
                .build();

        return embeddingStore.search(request).matches().stream()
                .map(match -> new SimilarTrackResult(
                        reconstructTrack(toStringMap(match.embedded().metadata().toMap())),
                        match.score()))
                .toList();
    }

    String buildEmbeddingText(EnrichedTrackMetadata track) {
        var sb = new StringBuilder();
        sb.append("Track: ").append(track.title()).append(" by ").append(track.artist()).append(".");
        if (track.album() != null && !track.album().isBlank()) sb.append(" Album: ").append(track.album()).append(".");
        if (!track.genres().isEmpty()) sb.append(" Genres: ").append(String.join(", ", track.genres())).append(".");
        if (track.releaseYear() > 0) sb.append(" Year: ").append(track.releaseYear()).append(".");
        final var af = track.audioFeatures();
        if (af != null) {
            if (af.energy() != null) sb.append(" Energy ").append(af.energy()).append(",");
            if (af.danceability() != null) sb.append(" Danceability ").append(af.danceability()).append(".");
            if (af.bpm() != null) sb.append(" Tempo: ").append(af.bpm()).append(" BPM,");
            if (af.musicalKey() != null) sb.append(" Key: ").append(af.fullKey()).append(".");
        }
        if (track.popularity() > 0) sb.append(" Popularity: ").append(track.popularity()).append(".");
        final var th = track.themes();
        if (th != null && !th.isEmpty()) {
            final var terms = th.allTerms();
            if (!terms.isEmpty()) sb.append(" Themes: ").append(String.join(", ", terms)).append(".");
            if (th.mood() != null && !th.mood().isBlank()) sb.append(" Mood: ").append(th.mood()).append(".");
            if (th.sentiment() != null && !th.sentiment().isBlank()) sb.append(" Sentiment: ").append(th.sentiment()).append(".");
        }
        return sb.toString();
    }

    Map<String, Object> buildMetadata(EnrichedTrackMetadata track) {
        var metadata = new HashMap<String, Object>();
        metadata.put("sourceId", track.sourceId());
        metadata.put("artist", track.artist());
        metadata.put("title", track.title());
        if (track.album() != null) metadata.put("album", track.album());
        if (!track.genres().isEmpty()) metadata.put("genres", String.join(",", track.genres()));
        metadata.put("releaseYear", track.releaseYear());
        metadata.put("popularity", track.popularity());
        final var af = track.audioFeatures();
        if (af != null) {
            if (af.bpm() != null) metadata.put("bpm", af.bpm());
            if (af.energy() != null) metadata.put("energy", af.energy());
            if (af.danceability() != null) metadata.put("danceability", af.danceability());
            if (af.valence() != null) metadata.put("valence", af.valence());
            if (af.musicalKey() != null) metadata.put("musicalKey", af.musicalKey());
            if (af.mode() != null) metadata.put("mode", af.mode());
        }
        return metadata;
    }

    EnrichedTrackMetadata reconstructTrack(Map<String, String> metadata) {
        final var genresStr = metadata.getOrDefault("genres", "");
        final List<String> genres = genresStr.isBlank() ? List.of() : List.of(genresStr.split(","));
        final var audioFeatures = new AudioFeatures(
                toDouble(metadata.get("danceability")), toDouble(metadata.get("energy")),
                toDouble(metadata.get("valence")), null, null, null,
                toDouble(metadata.get("bpm")), metadata.get("musicalKey"), metadata.get("mode"), null, null, null);
        return new EnrichedTrackMetadata(
                metadata.get("sourceId"), metadata.get("artist"), metadata.get("title"),
                metadata.get("album"), genres, List.of(), null, null, null, List.of(),
                toInt(metadata.get("releaseYear")), toInt(metadata.get("popularity")), 0L, audioFeatures, null, null, null);
    }

    private static Map<String, String> toStringMap(Map<String, Object> map) {
        var result = new java.util.HashMap<String, String>();
        map.forEach((k, v) -> result.put(k, v != null ? v.toString() : null));
        return result;
    }

    private static Double toDouble(String val) {
        if (val == null || val.isBlank()) return null;
        try { return Double.parseDouble(val); } catch (NumberFormatException e) { return null; }
    }

    private static int toInt(String val) {
        if (val == null || val.isBlank()) return 0;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return 0; }
    }
}
