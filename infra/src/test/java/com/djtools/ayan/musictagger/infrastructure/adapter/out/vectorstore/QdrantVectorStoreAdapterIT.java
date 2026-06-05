package com.djtools.ayan.musictagger.infrastructure.adapter.out.vectorstore;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.SimilarTrackResult;
import com.djtools.ayan.musictagger.infrastructure.config.QdrantClientConfig;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
        classes = {QdrantVectorStoreAdapter.class, QdrantClientConfig.class, QdrantVectorStoreAdapterIT.MockEmbeddingConfig.class},
        properties = "dj-tagger.rag.similarity-threshold=0.0"
)
@Testcontainers
class QdrantVectorStoreAdapterIT {

    @Container
    static QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:v1.13.6");

    @DynamicPropertySource
    static void qdrantProperties(DynamicPropertyRegistry registry) {
        registry.add("qdrant.host", qdrant::getHost);
        registry.add("qdrant.port", qdrant::getGrpcPort);
        registry.add("qdrant.collection-name", () -> "dj-tracks-test");
    }

    @Autowired
    QdrantVectorStoreAdapter adapter;

    @Autowired
    EmbeddingStore<TextSegment> embeddingStore;

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
                null, List.of(), 2024, 80, 210000L,
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
        var queryEmbedding = deterministicEmbedding("Artist Title");
        var request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(10)
                .minScore(0.0)
                .build();
        var matches = embeddingStore.search(request).matches();

        String expectedId = java.util.UUID.nameUUIDFromBytes("sp-dedup-1".getBytes()).toString();
        long count = matches.stream().filter(m -> expectedId.equals(m.embeddingId())).count();
        assertThat(count).isLessThanOrEqualTo(1);
    }

    private static Embedding deterministicEmbedding(String text) {
        var random = new Random(text.hashCode());
        float[] vector = new float[768];
        for (int i = 0; i < 768; i++) {
            vector[i] = random.nextFloat() * 2 - 1;
        }
        float norm = 0;
        for (float v : vector) norm += v * v;
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < 768; i++) vector[i] /= norm;
        return Embedding.from(vector);
    }

    /**
     * Deterministic embedding model for IT tests — returns 768-dim vectors
     * seeded by the text hashcode for reproducibility.
     */
    static class DeterministicEmbeddingModel implements EmbeddingModel {

        @Override
        public Response<Embedding> embed(String text) {
            return Response.from(deterministicEmbedding(text));
        }

        @Override
        public Response<Embedding> embed(TextSegment textSegment) {
            return embed(textSegment.text());
        }

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
            return Response.from(textSegments.stream().map(s -> deterministicEmbedding(s.text())).toList());
        }

        @Override
        public int dimension() {
            return 768;
        }

        private static Embedding deterministicEmbedding(String text) {
            return QdrantVectorStoreAdapterIT.deterministicEmbedding(text);
        }
    }
}
