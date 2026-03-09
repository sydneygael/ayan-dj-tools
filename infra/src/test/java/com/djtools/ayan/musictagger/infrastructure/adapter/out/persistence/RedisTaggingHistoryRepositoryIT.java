package com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence;

import com.djtools.ayan.musictagger.domain.model.OperationStatus;
import com.djtools.ayan.musictagger.domain.model.TaggingHistoryEntry;
import com.djtools.ayan.musictagger.infrastructure.config.RedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {RedisTaggingHistoryRepository.class, RedisConfig.class})
@EnableAutoConfiguration(exclude = {OllamaApiAutoConfiguration.class, OllamaChatAutoConfiguration.class, OllamaEmbeddingAutoConfiguration.class})
@Testcontainers
class RedisTaggingHistoryRepositoryIT {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired
    RedisTaggingHistoryRepository repository;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void cleanRedis() {
        var keys = redisTemplate.keys("tagging-history:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void shouldSaveAndFindByPlanId() {
        var entry = new TaggingHistoryEntry(
                "/a.mp3", "plan-1",
                Map.of("artist", "Old Artist"), Map.of("artist", "Old Artist", "genre", "Techno"),
                OperationStatus.APPLIED, null, LocalDateTime.now()
        );

        repository.save(entry);

        List<TaggingHistoryEntry> results = repository.findByPlanId("plan-1");
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().filepath()).isEqualTo("/a.mp3");
        assertThat(results.getFirst().status()).isEqualTo(OperationStatus.APPLIED);
    }

    @Test
    void shouldReturnEmptyForUnknownPlan() {
        assertThat(repository.findByPlanId("unknown")).isEmpty();
    }

    @Test
    void shouldSaveMultipleEntries() {
        var entry1 = new TaggingHistoryEntry(
                "/a.mp3", "plan-2", Map.of(), Map.of("genre", "Techno"),
                OperationStatus.APPLIED, null, LocalDateTime.now()
        );
        var entry2 = new TaggingHistoryEntry(
                "/b.mp3", "plan-2", Map.of(), Map.of("bpm", "128"),
                OperationStatus.ERROR, "Permission denied", LocalDateTime.now()
        );

        repository.save(entry1);
        repository.save(entry2);

        List<TaggingHistoryEntry> results = repository.findByPlanId("plan-2");
        assertThat(results).hasSize(2);
    }

    @Test
    void shouldFindByFilepath() {
        var entry = new TaggingHistoryEntry(
                "/specific.mp3", "plan-3", Map.of(), Map.of("genre", "House"),
                OperationStatus.APPLIED, null, LocalDateTime.now()
        );

        repository.save(entry);

        List<TaggingHistoryEntry> results = repository.findByFilepath("/specific.mp3");
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().planId()).isEqualTo("plan-3");
    }
}
