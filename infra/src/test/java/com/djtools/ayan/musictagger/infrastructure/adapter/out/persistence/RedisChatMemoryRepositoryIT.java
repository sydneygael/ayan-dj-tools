package com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence;

import com.djtools.ayan.musictagger.infrastructure.config.RedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {RedisChatMemoryRepository.class, RedisConfig.class})
@EnableAutoConfiguration(exclude = {OllamaApiAutoConfiguration.class, OllamaChatAutoConfiguration.class,
        OllamaEmbeddingAutoConfiguration.class, QdrantVectorStoreAutoConfiguration.class})
@Testcontainers
class RedisChatMemoryRepositoryIT {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired RedisChatMemoryRepository repository;
    @Autowired RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void cleanRedis() {
        final var keys = redisTemplate.keys("chat-memory:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void saveAndRetrieveMessages() {
        repository.saveAll("it-conv-1", List.of(
                new UserMessage("Salut Ayan"),
                new AssistantMessage("Bonjour !")
        ));

        final var history = repository.findByConversationId("it-conv-1");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getMessageType().getValue()).isEqualTo("user");
        assertThat(history.get(0).getText()).isEqualTo("Salut Ayan");
        assertThat(history.get(1).getMessageType().getValue()).isEqualTo("assistant");
        assertThat(history.get(1).getText()).isEqualTo("Bonjour !");
    }

    @Test
    void findByConversationId_returnsEmptyWhenNoMessages() {
        assertThat(repository.findByConversationId("unknown-conv")).isEmpty();
    }

    @Test
    void deleteByConversationId_removesAllMessages() {
        repository.saveAll("it-conv-3", List.of(
                new UserMessage("Test"),
                new AssistantMessage("Réponse")
        ));

        repository.deleteByConversationId("it-conv-3");

        assertThat(repository.findByConversationId("it-conv-3")).isEmpty();
    }

    @Test
    void separateConversationsAreIsolated() {
        repository.saveAll("conv-A", List.of(new UserMessage("Conversation A")));
        repository.saveAll("conv-B", List.of(new UserMessage("Conversation B")));

        assertThat(repository.findByConversationId("conv-A")).hasSize(1);
        assertThat(repository.findByConversationId("conv-B")).hasSize(1);
        assertThat(repository.findByConversationId("conv-A").getFirst().getText()).isEqualTo("Conversation A");
        assertThat(repository.findByConversationId("conv-B").getFirst().getText()).isEqualTo("Conversation B");
    }

    @Test
    void preservesMessageOrder() {
        repository.saveAll("it-conv-order", List.of(
                new UserMessage("Message 1"),
                new AssistantMessage("Réponse 1"),
                new UserMessage("Message 2"),
                new AssistantMessage("Réponse 2")
        ));

        final var history = repository.findByConversationId("it-conv-order");

        assertThat(history).hasSize(4);
        assertThat(history.get(0).getText()).isEqualTo("Message 1");
        assertThat(history.get(2).getText()).isEqualTo("Message 2");
    }

    @Test
    void multipleSavesAppendMessages() {
        repository.saveAll("it-conv-append", List.of(new UserMessage("Msg 1")));
        repository.saveAll("it-conv-append", List.of(new AssistantMessage("Reply 1")));
        repository.saveAll("it-conv-append", List.of(new UserMessage("Msg 2")));

        final var history = repository.findByConversationId("it-conv-append");
        assertThat(history).hasSize(3);
        assertThat(history.get(0).getText()).isEqualTo("Msg 1");
        assertThat(history.get(1).getText()).isEqualTo("Reply 1");
        assertThat(history.get(2).getText()).isEqualTo("Msg 2");
    }
}
