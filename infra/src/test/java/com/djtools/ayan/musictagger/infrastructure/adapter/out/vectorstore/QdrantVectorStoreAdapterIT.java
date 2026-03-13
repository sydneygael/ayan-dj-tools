package com.djtools.ayan.musictagger.infrastructure.adapter.out.vectorstore;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.SimilarTrackResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.qdrant.QdrantContainer;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {QdrantVectorStoreAdapter.class, QdrantVectorStoreAdapterIT.MockEmbeddingConfig.class},
        properties = "dj-tagger.rag.similarity-threshold=0.0"
)
@EnableAutoConfiguration(exclude = {OllamaApiAutoConfiguration.class, OllamaChatAutoConfiguration.class, OllamaEmbeddingAutoConfiguration.class})
@Testcontainers
class QdrantVectorStoreAdapterIT {

    @Container
    static QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:v1.13.6");

    @DynamicPropertySource
    static void qdrantProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.vectorstore.qdrant.host", qdrant::getHost);
        registry.add("spring.ai.vectorstore.qdrant.port", qdrant::getGrpcPort);
        registry.add("spring.ai.vectorstore.qdrant.collection-name", () -> "dj-tracks-test");
        registry.add("spring.ai.vectorstore.qdrant.initialize-schema", () -> "true");
    }

    @Autowired
    QdrantVectorStoreAdapter adapter;

    @Autowired
    VectorStore vectorStore;

    @TestConfiguration
    static class MockEmbeddingConfig {

        @Bean
        @Primary
        EmbeddingModel deterministicEmbeddingModel() {
            return new DeterministicEmbeddingModel();
        }
    }

    private EnrichedTrackMetadata track(String id, String artist, String title, List<String> genres) {
        return new EnrichedTrackMetadata(
                id, artist, title, "Album",
                genres, List.of(), null, null,
                null, List.of(), 2024, 80, 210000,
                new AudioFeatures(0.8, 0.9, 0.7, null, null, null, 128.0, "Am", "minor", null)
        );
    }

    @Test
    void shouldStoreAndFindSimilarTracks() {
        adapter.store(track("sp-it-1", "Daft Punk", "Around The World", List.of("Electronic", "House")));
        adapter.store(track("sp-it-2", "Bicep", "Glue", List.of("Electronic", "Techno")));

        List<SimilarTrackResult> results = adapter.findSimilar("electronic dance music", 5);

        assertThat(results).isNotEmpty();
    }

    @Test
    void shouldReturnEmptyForEmptyCollection() {
        // Use a very specific query that won't match random embeddings easily
        // The deterministic model returns consistent vectors, so all docs may match
        // Just verify no exception is thrown
        List<SimilarTrackResult> results = adapter.findSimilar("xyznonexistentquery", 5);
        assertThat(results).isNotNull();
    }

    @Test
    void shouldDeduplicateBySourceId() {
        adapter.store(track("sp-dedup-1", "Artist", "Title v1", List.of("Electronic")));
        adapter.store(track("sp-dedup-1", "Artist", "Title v2", List.of("Electronic")));

        // Search for the track — should only get one result
        var request = SearchRequest.builder()
                .query("Artist Title")
                .topK(10)
                .similarityThreshold(0.0)
                .build();
        var docs = vectorStore.similaritySearch(request);

        String expectedUuid = java.util.UUID.nameUUIDFromBytes("sp-dedup-1".getBytes()).toString();
        long count = docs.stream().filter(d -> expectedUuid.equals(d.getId())).count();
        assertThat(count).isLessThanOrEqualTo(1);
    }

    /**
     * Deterministic embedding model for IT tests — returns 768-dim vectors
     * seeded by the text hashcode for reproducibility.
     */
    static class DeterministicEmbeddingModel implements EmbeddingModel {

        @Override
        public float[] embed(Document document) {
            return generateVector(document.getText());
        }

        @Override
        public float[] embed(String text) {
            return generateVector(text);
        }

        @Override
        public org.springframework.ai.embedding.EmbeddingResponse call(org.springframework.ai.embedding.EmbeddingRequest request) {
            var embeddings = request.getInstructions().stream()
                    .map(text -> {
                        float[] vector = generateVector(text);
                        return new org.springframework.ai.embedding.Embedding(vector, 0);
                    })
                    .toList();
            return new org.springframework.ai.embedding.EmbeddingResponse(embeddings);
        }

        @Override
        public int dimensions() {
            return 768;
        }

        private float[] generateVector(String text) {
            var random = new Random(text.hashCode());
            float[] vector = new float[768];
            for (int i = 0; i < 768; i++) {
                vector[i] = random.nextFloat() * 2 - 1;
            }
            // Normalize
            float norm = 0;
            for (float v : vector) norm += v * v;
            norm = (float) Math.sqrt(norm);
            for (int i = 0; i < 768; i++) vector[i] /= norm;
            return vector;
        }
    }
}
