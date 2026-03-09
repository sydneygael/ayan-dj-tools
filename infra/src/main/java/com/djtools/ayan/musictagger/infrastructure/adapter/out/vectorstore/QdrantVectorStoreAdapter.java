package com.djtools.ayan.musictagger.infrastructure.adapter.out.vectorstore;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.SimilarTrackResult;
import com.djtools.ayan.musictagger.domain.port.out.VectorStorePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class QdrantVectorStoreAdapter implements VectorStorePort {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStoreAdapter.class);

    private final VectorStore vectorStore;
    private final double similarityThreshold;

    public QdrantVectorStoreAdapter(VectorStore vectorStore,
                                     @Value("${dj-tagger.rag.similarity-threshold:0.7}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.similarityThreshold = similarityThreshold;
    }

    @Override
    public void store(EnrichedTrackMetadata track) {
        String embeddingText = buildEmbeddingText(track);
        Map<String, Object> metadata = buildMetadata(track);

        var document = new Document(track.sourceId(), embeddingText, metadata);
        vectorStore.add(List.of(document));

        log.debug("Stored track in vector store: {} - {} (id={})", track.artist(), track.title(), track.sourceId());
    }

    @Override
    public List<SimilarTrackResult> findSimilar(String query, int limit) {
        var request = SearchRequest.builder()
                .query(query)
                .topK(limit)
                .similarityThreshold(similarityThreshold)
                .build();

        var results = vectorStore.similaritySearch(request);

        return results.stream()
                .map(doc -> new SimilarTrackResult(
                        reconstructTrack(doc.getMetadata()),
                        doc.getScore() != null ? doc.getScore() : 0.0
                ))
                .toList();
    }

    String buildEmbeddingText(EnrichedTrackMetadata track) {
        var sb = new StringBuilder();
        sb.append("Track: ").append(track.title()).append(" by ").append(track.artist()).append(".");

        if (track.album() != null && !track.album().isBlank()) {
            sb.append(" Album: ").append(track.album()).append(".");
        }
        if (!track.genres().isEmpty()) {
            sb.append(" Genres: ").append(String.join(", ", track.genres())).append(".");
        }
        if (track.releaseYear() > 0) {
            sb.append(" Year: ").append(track.releaseYear()).append(".");
        }

        AudioFeatures af = track.audioFeatures();
        if (af != null) {
            if (af.energy() != null) sb.append(" Energy ").append(af.energy()).append(",");
            if (af.danceability() != null) sb.append(" Danceability ").append(af.danceability()).append(".");
            if (af.bpm() != null) sb.append(" Tempo: ").append(af.bpm()).append(" BPM,");
            if (af.musicalKey() != null) sb.append(" Key: ").append(af.fullKey()).append(".");
        }

        if (track.popularity() > 0) {
            sb.append(" Popularity: ").append(track.popularity()).append(".");
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

        AudioFeatures af = track.audioFeatures();
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

    EnrichedTrackMetadata reconstructTrack(Map<String, Object> metadata) {
        String genresStr = metadata.getOrDefault("genres", "").toString();
        List<String> genres = genresStr.isBlank() ? List.of() : List.of(genresStr.split(","));

        Double bpm = toDouble(metadata.get("bpm"));
        Double energy = toDouble(metadata.get("energy"));
        Double danceability = toDouble(metadata.get("danceability"));
        Double valence = toDouble(metadata.get("valence"));
        String musicalKey = toStr(metadata.get("musicalKey"));
        String mode = toStr(metadata.get("mode"));

        AudioFeatures audioFeatures = new AudioFeatures(
                danceability, energy, valence,
                null, null, null,
                bpm, musicalKey, mode, null
        );

        return new EnrichedTrackMetadata(
                toStr(metadata.get("sourceId")),
                toStr(metadata.get("artist")),
                toStr(metadata.get("title")),
                toStr(metadata.get("album")),
                genres,
                List.of(),
                null,
                null,
                null,
                List.of(),
                toInt(metadata.get("releaseYear")),
                toInt(metadata.get("popularity")),
                0L,
                audioFeatures
        );
    }

    private static Double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        return null;
    }

    private static String toStr(Object val) {
        return val != null ? val.toString() : null;
    }

    private static int toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        return 0;
    }
}
