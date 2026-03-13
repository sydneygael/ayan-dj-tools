package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.infrastructure.config.RedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {ConversationHistoryService.class, RedisConfig.class})
@EnableAutoConfiguration(exclude = {OllamaApiAutoConfiguration.class, OllamaChatAutoConfiguration.class, OllamaEmbeddingAutoConfiguration.class, QdrantVectorStoreAutoConfiguration.class})
@Testcontainers
class ConversationHistoryServiceIT {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired
    ConversationHistoryService historyService;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void cleanRedis() {
        var keys = redisTemplate.keys("conversation:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void saveAndRetrieveMessages() {
        var now = LocalDateTime.of(2026, 2, 26, 14, 0, 0);

        historyService.saveMessage("it-conv-1", new ChatMessage("user", "Salut Ayan", now));
        historyService.saveMessage("it-conv-1", new ChatMessage("assistant", "Bonjour !", now.plusSeconds(1)));

        List<ChatMessage> history = historyService.getHistory("it-conv-1");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).role()).isEqualTo("user");
        assertThat(history.get(0).content()).isEqualTo("Salut Ayan");
        assertThat(history.get(1).role()).isEqualTo("assistant");
        assertThat(history.get(1).content()).isEqualTo("Bonjour !");
    }

    @Test
    void getMessageCount_reflectsStoredMessages() {
        var now = LocalDateTime.now();

        assertThat(historyService.getMessageCount("it-conv-2")).isZero();

        historyService.saveMessage("it-conv-2", new ChatMessage("user", "Message 1", now));
        historyService.saveMessage("it-conv-2", new ChatMessage("assistant", "Réponse 1", now));
        historyService.saveMessage("it-conv-2", new ChatMessage("user", "Message 2", now));

        assertThat(historyService.getMessageCount("it-conv-2")).isEqualTo(3);
    }

    @Test
    void clearHistory_removesAllMessages() {
        var now = LocalDateTime.now();
        historyService.saveMessage("it-conv-3", new ChatMessage("user", "Test", now));
        historyService.saveMessage("it-conv-3", new ChatMessage("assistant", "Réponse", now));

        historyService.clearHistory("it-conv-3");

        assertThat(historyService.getHistory("it-conv-3")).isEmpty();
        assertThat(historyService.getMessageCount("it-conv-3")).isZero();
    }

    @Test
    void separateConversationsAreIsolated() {
        var now = LocalDateTime.now();
        historyService.saveMessage("conv-A", new ChatMessage("user", "Conversation A", now));
        historyService.saveMessage("conv-B", new ChatMessage("user", "Conversation B", now));

        assertThat(historyService.getHistory("conv-A")).hasSize(1);
        assertThat(historyService.getHistory("conv-B")).hasSize(1);
        assertThat(historyService.getHistory("conv-A").getFirst().content()).isEqualTo("Conversation A");
        assertThat(historyService.getHistory("conv-B").getFirst().content()).isEqualTo("Conversation B");
    }

    @Test
    void getHistory_preservesMessageOrder() {
        var now = LocalDateTime.now();
        for (int i = 1; i <= 5; i++) {
            historyService.saveMessage("it-conv-order", new ChatMessage("user", "Message " + i, now.plusSeconds(i)));
        }

        List<ChatMessage> history = historyService.getHistory("it-conv-order");

        assertThat(history).hasSize(5);
        for (int i = 0; i < 5; i++) {
            assertThat(history.get(i).content()).isEqualTo("Message " + (i + 1));
        }
    }
}
