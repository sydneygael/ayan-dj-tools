package com.djtools.ayan.musictagger.infrastructure.integration;

import com.djtools.ayan.musictagger.domain.model.OperationStatus;
import com.djtools.ayan.musictagger.domain.model.TaggingHistoryEntry;
import com.djtools.ayan.musictagger.infrastructure.adapter.in.rest.StatsController;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence.RedisAudioFeaturesCacheRepository;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence.RedisPlanRepository;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence.RedisTaggingHistoryRepository;
import com.djtools.ayan.musictagger.infrastructure.config.RedisConfig;
import com.djtools.ayan.musictagger.infrastructure.service.StatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = {
                StatsController.class,
                StatsService.class,
                RedisTaggingHistoryRepository.class,
                RedisPlanRepository.class,
                RedisAudioFeaturesCacheRepository.class,
                RedisConfig.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@EnableAutoConfiguration(exclude = {
        OllamaApiAutoConfiguration.class,
        OllamaChatAutoConfiguration.class,
        OllamaEmbeddingAutoConfiguration.class,
        QdrantVectorStoreAutoConfiguration.class
})
@Testcontainers
class StatsControllerIT {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired WebApplicationContext context;
    @Autowired RedisTaggingHistoryRepository historyRepository;
    @Autowired RedisTemplate<String, Object> redisTemplate;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        for (String pattern : new String[]{"tagging-history:*", "plan:*", "audio-features:*"}) {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
        }
    }

    @Test
    void getStats_returnsNonZeroStatsAfterHistory() throws Exception {
        historyRepository.save(new TaggingHistoryEntry(
                "/a.mp3", "plan-stats-1",
                Map.of("artist", "Old"), Map.of("artist", "New", "genre", "Techno"),
                OperationStatus.APPLIED, null, LocalDateTime.now()
        ));
        historyRepository.save(new TaggingHistoryEntry(
                "/b.mp3", "plan-stats-1",
                Map.of(), Map.of("genre", "House", "bpm", "128"),
                OperationStatus.APPLIED, null, LocalDateTime.now()
        ));

        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPlansCreated").value(1))
                .andExpect(jsonPath("$.totalFilesEnriched").value(2))
                .andExpect(jsonPath("$.totalTagsApplied").value(4))
                .andExpect(jsonPath("$.tagsAppliedByType.genre").value(2))
                .andExpect(jsonPath("$.recentActivity").isArray());
    }

    @Test
    void getCollectionProfile_returnsGenreAndBpmDistribution() throws Exception {
        historyRepository.save(new TaggingHistoryEntry(
                "/a.mp3", "plan-col-1",
                Map.of(), Map.of("genre", "Techno", "bpm", "128", "key", "Am"),
                OperationStatus.APPLIED, null, LocalDateTime.now()
        ));
        historyRepository.save(new TaggingHistoryEntry(
                "/b.mp3", "plan-col-1",
                Map.of(), Map.of("genre", "House", "bpm", "124"),
                OperationStatus.APPLIED, null, LocalDateTime.now()
        ));

        mockMvc.perform(get("/api/stats/collection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTracksScanned").value(2))
                .andExpect(jsonPath("$.genreDistribution.Techno").value(1))
                .andExpect(jsonPath("$.genreDistribution.House").value(1))
                .andExpect(jsonPath("$.bpmHistogram").isMap())
                .andExpect(jsonPath("$.keyDistribution.Am").value(1));
    }

    @Test
    void getEnrichmentStats_returnsMatchAndErrorRates() throws Exception {
        historyRepository.save(new TaggingHistoryEntry(
                "/a.mp3", "plan-enr-1",
                Map.of(), Map.of("genre", "Techno"),
                OperationStatus.APPLIED, null, LocalDateTime.now()
        ));
        historyRepository.save(new TaggingHistoryEntry(
                "/b.mp3", "plan-enr-1",
                Map.of(), Map.of("bpm", "128"),
                OperationStatus.APPLIED, null, LocalDateTime.now()
        ));
        historyRepository.save(new TaggingHistoryEntry(
                "/c.mp3", "plan-enr-1",
                Map.of(), Map.of(),
                OperationStatus.ERROR, "Write failed", LocalDateTime.now()
        ));

        mockMvc.perform(get("/api/stats/enrichment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spotifyMatchRate").isNumber())
                .andExpect(jsonPath("$.errorRate").isNumber())
                .andExpect(jsonPath("$.enrichmentBySource.spotify").value(2))
                .andExpect(jsonPath("$.mostEnrichedTagTypes").isMap());
    }

    @Test
    void getActivityTimeline_monthPeriod_returnsTagsAndModeUsage() throws Exception {
        historyRepository.save(new TaggingHistoryEntry(
                "/a.mp3", "plan-act-1",
                Map.of(), Map.of("genre", "Techno"),
                OperationStatus.APPLIED, null, LocalDateTime.now()
        ));

        mockMvc.perform(get("/api/stats/activity").param("period", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagsAppliedPerPeriod").isMap())
                .andExpect(jsonPath("$.modeUsage").exists())
                .andExpect(jsonPath("$.plansPerPeriod").isMap());
    }
}
