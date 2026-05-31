package com.djtools.ayan.musictagger.infrastructure.integration;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.domain.port.out.ScannedTrackRepository;
import com.djtools.ayan.musictagger.infrastructure.adapter.in.rest.PlanController;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.audio.JAudioTaggerAdapter;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence.RedisPlanRepository;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence.RedisTaggingHistoryRepository;
import com.djtools.ayan.musictagger.infrastructure.config.DomainConfig;
import com.djtools.ayan.musictagger.infrastructure.config.RedisConfig;
import com.djtools.ayan.musictagger.infrastructure.service.ApplyModeService;
import com.djtools.ayan.musictagger.infrastructure.service.ManualModeService;
import com.djtools.ayan.musictagger.infrastructure.service.PlanManagementService;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = {
                PlanWorkflowIT.TestConfig.class,
                PlanController.class,
                PlanManagementService.class,
                JAudioTaggerAdapter.class,
                RedisPlanRepository.class,
                RedisTaggingHistoryRepository.class,
                RedisConfig.class,
                DomainConfig.class
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
class PlanWorkflowIT {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Configuration
    static class TestConfig {

        @Bean
        MusicMetadataProvider musicMetadataProvider() {
            var mock = mock(MusicMetadataProvider.class);
            when(mock.enrich(anyString(), anyString())).thenReturn(
                    EnrichmentResult.success(new EnrichedTrackMetadata(
                            "spotify:123", "Test Artist", "Test Title", "Test Album",
                            List.of("Electronic"), List.of(), null, null, null, List.of(),
                            2024, 80, 210000L,
                            new AudioFeatures(0.8, 0.7, 0.6, 0.1, 0.0, 0.05, 128.0, "A", "minor", 4)
                    ))
            );
            return mock;
        }

        @Bean
        ScannedTrackRepository scannedTrackRepository() {
            return mock(ScannedTrackRepository.class);
        }

        @Bean
        ManualModeService manualModeService() {
            return mock(ManualModeService.class);
        }

        @Bean
        ApplyModeService applyModeService() {
            return mock(ApplyModeService.class);
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired RedisTemplate<String, Object> redisTemplate;

    @TempDir Path tempDir;
    MockMvc mockMvc;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        var planKeys = redisTemplate.keys("plan:*");
        if (planKeys != null && !planKeys.isEmpty()) redisTemplate.delete(planKeys);
        var historyKeys = redisTemplate.keys("tagging-history:*");
        if (historyKeys != null && !historyKeys.isEmpty()) redisTemplate.delete(historyKeys);
    }

    @Test
    void createAndGetPlan() throws Exception {
        Path mp3 = createMp3WithMissingTags("missing-tags.mp3");

        String response = mockMvc.perform(post("/api/plan/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filePaths": ["%s"]}
                                """.formatted(mp3.toString().replace("\\", "/"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("READY_FOR_REVIEW"))
                .andExpect(jsonPath("$.totalFiles").value(1))
                .andReturn().getResponse().getContentAsString();

        var plan = objectMapper.readValue(response, TaggingPlan.class);

        mockMvc.perform(get("/api/plan/" + plan.planId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(plan.planId()))
                .andExpect(jsonPath("$.operations").isArray())
                .andExpect(jsonPath("$.operations[0].filepath").isNotEmpty())
                .andExpect(jsonPath("$.operations[0].suggestedTags").isNotEmpty());
    }

    @Test
    void fullWorkflow_createApproveExecute_writesTagsToDisk() throws Exception {
        Path mp3 = createMp3WithMissingTags("workflow-test.mp3");

        // Create plan
        String createResponse = mockMvc.perform(post("/api/plan/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filePaths": ["%s"]}
                                """.formatted(mp3.toString().replace("\\", "/"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var plan = objectMapper.readValue(createResponse, TaggingPlan.class);
        String planId = plan.planId();

        // Approve plan
        mockMvc.perform(put("/api/plan/" + planId + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Execute plan
        mockMvc.perform(post("/api/plan/" + planId + "/execute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").isNumber());

        // Verify tags were actually written to the MP3 file
        var audioFile = AudioFileIO.read(mp3.toFile());
        var tag = audioFile.getTag();
        assertThat(tag).isNotNull();

        // The file had only artist+title; Spotify enrichment should have added genre + album
        String genre = tag.getFirst(FieldKey.GENRE);
        String album = tag.getFirst(FieldKey.ALBUM);
        assertThat(genre).isNotBlank();
        assertThat(album).isNotBlank();
    }

    // --- Test MP3 helper (mirrors TestAudioFileHelper, which is package-private) ---

    private Path createMp3WithMissingTags(String filename) throws Exception {
        Path file = createMinimalMp3(filename);
        var audioFile = AudioFileIO.read(file.toFile());
        var tag = new org.jaudiotagger.tag.id3.ID3v24Tag();
        tag.setField(FieldKey.ARTIST, "Partial Artist");
        tag.setField(FieldKey.TITLE, "Partial Title");
        audioFile.setTag(tag);
        audioFile.commit();
        return file;
    }

    private Path createMinimalMp3(String filename) throws Exception {
        java.nio.file.Files.createDirectories(tempDir);
        Path file = tempDir.resolve(filename);
        try (var os = java.nio.file.Files.newOutputStream(file)) {
            byte[] frameHeader = {(byte) 0xFF, (byte) 0xFB, (byte) 0x90, 0x00};
            byte[] frameData = new byte[413];
            for (int i = 0; i < 5; i++) {
                os.write(frameHeader);
                os.write(frameData);
            }
        }
        return file;
    }
}
