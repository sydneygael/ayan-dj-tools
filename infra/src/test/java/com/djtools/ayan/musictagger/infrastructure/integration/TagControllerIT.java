package com.djtools.ayan.musictagger.infrastructure.integration;

import com.djtools.ayan.musictagger.infrastructure.adapter.in.rest.TagController;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.audio.JAudioTaggerAdapter;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence.RedisTaggingHistoryRepository;
import com.djtools.ayan.musictagger.infrastructure.config.RedisConfig;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = {
                TagController.class,
                JAudioTaggerAdapter.class,
                RedisTaggingHistoryRepository.class,
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
class TagControllerIT {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired WebApplicationContext context;
    @Autowired RedisTemplate<String, Object> redisTemplate;

    @TempDir Path tempDir;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        var keys = redisTemplate.keys("tagging-history:*");
        if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
    }

    @Test
    void applyTags_writesTagsToMp3File() throws Exception {
        Path mp3 = createMinimalMp3("apply-test.mp3");
        String filepath = mp3.toString().replace("\\", "/");

        mockMvc.perform(post("/api/tags/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filepath": "%s", "tags": {"artist": "DJ Test", "genre": "Techno", "bpm": "140"}}
                                """.formatted(filepath)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filepath").value(filepath))
                .andExpect(jsonPath("$.status").value("APPLIED"));

        // Verify tags were actually written to the file
        var audioFile = AudioFileIO.read(mp3.toFile());
        var tag = audioFile.getTag();
        assertThat(tag).isNotNull();
        assertThat(tag.getFirst(FieldKey.ARTIST)).isEqualTo("DJ Test");
        assertThat(tag.getFirst(FieldKey.GENRE)).isEqualTo("Techno");
        assertThat(tag.getFirst(FieldKey.BPM)).isEqualTo("140");
    }

    @Test
    void previewTags_returnsPreviewWithoutWriting() throws Exception {
        Path mp3 = createMp3WithTags("preview-test.mp3");
        String filepath = mp3.toString().replace("\\", "/");

        mockMvc.perform(post("/api/tags/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filepath": "%s", "tags": {"genre": "House", "bpm": "125"}}
                                """.formatted(filepath)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filepath").value(filepath))
                .andExpect(jsonPath("$.changes").isArray());

        // Verify original tags were NOT modified
        var audioFile = AudioFileIO.read(mp3.toFile());
        var tag = audioFile.getTag();
        assertThat(tag.getFirst(FieldKey.GENRE)).isEqualTo("Electronic");
        assertThat(tag.getFirst(FieldKey.BPM)).isEqualTo("128");
    }

    // --- MP3 helpers ---

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

    private Path createMp3WithTags(String filename) throws Exception {
        Path file = createMinimalMp3(filename);
        var audioFile = AudioFileIO.read(file.toFile());
        var tag = new org.jaudiotagger.tag.id3.ID3v24Tag();
        tag.setField(FieldKey.ARTIST, "Test Artist");
        tag.setField(FieldKey.TITLE, "Test Title");
        tag.setField(FieldKey.GENRE, "Electronic");
        tag.setField(FieldKey.BPM, "128");
        audioFile.setTag(tag);
        audioFile.commit();
        return file;
    }
}
