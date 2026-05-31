package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence.RedisChatMemoryRepository;
import com.djtools.ayan.musictagger.infrastructure.config.RedisConfig;
import com.djtools.ayan.musictagger.infrastructure.service.AyanAgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = {
                AgentControllerIT.TestConfig.class,
                RedisConfig.class,
                RedisChatMemoryRepository.class,
                AgentController.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@EnableAutoConfiguration(exclude = {OllamaApiAutoConfiguration.class, OllamaChatAutoConfiguration.class,
        OllamaEmbeddingAutoConfiguration.class, QdrantVectorStoreAutoConfiguration.class})
@Testcontainers
class AgentControllerIT {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Configuration
    static class TestConfig {
        @Bean
        AyanAgentService agentService() {
            final var mock = mock(AyanAgentService.class);
            when(mock.chatWithToolCalls(anyString(), anyString(), any(), any(), any()))
                    .thenAnswer(inv -> new AyanAgentService.ChatResult(
                            "Réponse d'Ayan !", inv.getArgument(0), List.of()));
            return mock;
        }

        @Bean
        ChatMemory chatMemory(RedisChatMemoryRepository repository) {
            return MessageWindowChatMemory.builder()
                    .chatMemoryRepository(repository)
                    .maxMessages(100)
                    .build();
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired ChatMemory chatMemory;
    @Autowired RedisTemplate<String, Object> redisTemplate;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        final var keys = redisTemplate.keys("chat-memory:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void postChat_returnsReplyWithConversationId() throws Exception {
        mockMvc.perform(post("/api/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Salut Ayan", "conversationId": "it-rest-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Réponse d'Ayan !"))
                .andExpect(jsonPath("$.conversationId").value("it-rest-1"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void postChat_generatesConversationIdWhenAbsent() throws Exception {
        mockMvc.perform(post("/api/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Hello"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").isNotEmpty())
                .andExpect(jsonPath("$.reply").value("Réponse d'Ayan !"));
    }

    @Test
    void getHistory_returnsStoredMessages() throws Exception {
        chatMemory.add("it-rest-2", List.of(
                new UserMessage("Test message"),
                new AssistantMessage("Test reply")
        ));

        mockMvc.perform(get("/api/agent/conversations/it-rest-2/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[0].content").value("Test message"))
                .andExpect(jsonPath("$[1].role").value("assistant"))
                .andExpect(jsonPath("$[1].content").value("Test reply"));
    }

    @Test
    void deleteConversation_clearsHistory() throws Exception {
        chatMemory.add("it-rest-3", List.of(new UserMessage("To delete")));

        mockMvc.perform(delete("/api/agent/conversations/it-rest-3"))
                .andExpect(status().isOk());

        assertThat(chatMemory.get("it-rest-3")).isEmpty();
    }

    @Test
    void getHistory_returnsEmptyForUnknownConversation() throws Exception {
        mockMvc.perform(get("/api/agent/conversations/unknown/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
