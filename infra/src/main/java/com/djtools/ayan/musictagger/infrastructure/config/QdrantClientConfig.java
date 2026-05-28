package com.djtools.ayan.musictagger.infrastructure.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantConnectionDetails;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@Configuration
public class QdrantClientConfig {

    @Bean
    @Primary
    public QdrantClient qdrantClient(QdrantVectorStoreProperties properties, QdrantConnectionDetails connectionDetails) {
        var builder = QdrantGrpcClient.newBuilder(
                connectionDetails.getHost(),
                connectionDetails.getPort(),
                properties.isUseTls(),
                false
        );

        if (StringUtils.hasText(connectionDetails.getApiKey())) {
            builder.withApiKey(connectionDetails.getApiKey());
        }

        return new QdrantClient(builder.build());
    }
}
