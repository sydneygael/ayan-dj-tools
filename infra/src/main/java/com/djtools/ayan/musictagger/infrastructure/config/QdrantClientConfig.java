package com.djtools.ayan.musictagger.infrastructure.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantClientConfig {

    private static final Logger log = LoggerFactory.getLogger(QdrantClientConfig.class);

    // nomic-embed-text produces 768-dimensional embeddings
    private static final int EMBEDDING_DIMENSION = 768;

    @Bean
    public QdrantClient qdrantClient(
            @Value("${qdrant.host:localhost}") String host,
            @Value("${qdrant.port:6334}") int port) {
        return new QdrantClient(QdrantGrpcClient.newBuilder(host, port, false).build());
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            QdrantClient qdrantClient,
            @Value("${qdrant.collection-name:dj-tracks}") String collectionName) {
        initCollectionIfAbsent(qdrantClient, collectionName);
        return QdrantEmbeddingStore.builder()
                .client(qdrantClient)
                .collectionName(collectionName)
                .build();
    }

    private void initCollectionIfAbsent(QdrantClient client, String collectionName) {
        try {
            client.getCollectionInfoAsync(collectionName).get();
            log.info("Qdrant collection '{}' already exists", collectionName);
        } catch (Exception e) {
            log.info("Creating Qdrant collection '{}'", collectionName);
            try {
                client.createCollectionAsync(collectionName,
                        VectorParams.newBuilder()
                                .setSize(EMBEDDING_DIMENSION)
                                .setDistance(Distance.Cosine)
                                .build())
                        .get();
                log.info("Qdrant collection '{}' created", collectionName);
            } catch (Exception ex) {
                log.warn("Could not create Qdrant collection '{}': {}", collectionName, ex.getMessage());
            }
        }
    }
}
