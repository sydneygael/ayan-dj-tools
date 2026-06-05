package com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence;

import com.djtools.ayan.musictagger.infrastructure.config.RedisConfig;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {RedisChatMemoryRepository.class, RedisConfig.class})
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
        if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
    }

    @Test
    void saveAndRetrieveMessages() {
        repository.updateMessages("it-conv-1", List.of(
                UserMessage.from("Salut Ayan"),
                AiMessage.from("Bonjour !")
        ));

        final var history = repository.getMessages("it-conv-1");

        assertThat(history).hasSize(2);
        assertThat(((UserMessage) history.get(0)).singleText()).isEqualTo("Salut Ayan");
        assertThat(((AiMessage) history.get(1)).text()).isEqualTo("Bonjour !");
    }

    @Test
    void getMessages_returnsEmptyWhenNoMessages() {
        assertThat(repository.getMessages("unknown-conv")).isEmpty();
    }

    @Test
    void deleteMessages_removesAllMessages() {
        repository.updateMessages("it-conv-3", List.of(
                UserMessage.from("Test"),
                AiMessage.from("Réponse")
        ));

        repository.deleteMessages("it-conv-3");

        assertThat(repository.getMessages("it-conv-3")).isEmpty();
    }

    @Test
    void separateConversationsAreIsolated() {
        repository.updateMessages("conv-A", List.of(UserMessage.from("Conversation A")));
        repository.updateMessages("conv-B", List.of(UserMessage.from("Conversation B")));

        assertThat(repository.getMessages("conv-A")).hasSize(1);
        assertThat(repository.getMessages("conv-B")).hasSize(1);
        assertThat(((UserMessage) repository.getMessages("conv-A").getFirst()).singleText()).isEqualTo("Conversation A");
    }

    @Test
    void preservesMessageOrder() {
        repository.updateMessages("it-conv-order", List.of(
                UserMessage.from("Message 1"),
                AiMessage.from("Réponse 1"),
                UserMessage.from("Message 2"),
                AiMessage.from("Réponse 2")
        ));

        final var history = repository.getMessages("it-conv-order");

        assertThat(history).hasSize(4);
        assertThat(((UserMessage) history.get(0)).singleText()).isEqualTo("Message 1");
        assertThat(((UserMessage) history.get(2)).singleText()).isEqualTo("Message 2");
    }

    @Test
    void updateMessages_replacesExistingMessages_noDuplication() {
        repository.updateMessages("it-conv-replace", List.of(
                UserMessage.from("Msg 1"), AiMessage.from("Reply 1")
        ));
        repository.updateMessages("it-conv-replace", List.of(
                UserMessage.from("Msg 1"), AiMessage.from("Reply 1"),
                UserMessage.from("Msg 2"), AiMessage.from("Reply 2")
        ));

        assertThat(repository.getMessages("it-conv-replace")).hasSize(4);
    }

    @Test
    void updateMessages_filtersOutEmptyAiMessages() {
        repository.updateMessages("it-conv-tools", List.of(
                UserMessage.from("Lance l'analyse"),
                AiMessage.from(""),
                AiMessage.from("J'ai scanné le fichier.")
        ));

        final var history = repository.getMessages("it-conv-tools");
        assertThat(history).hasSize(2);
        assertThat(((UserMessage) history.get(0)).singleText()).isEqualTo("Lance l'analyse");
        assertThat(((AiMessage) history.get(1)).text()).isEqualTo("J'ai scanné le fichier.");
    }
}
