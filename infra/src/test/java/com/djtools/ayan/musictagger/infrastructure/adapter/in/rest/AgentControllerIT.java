package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.infrastructure.config.RedisConfig;
import com.djtools.ayan.musictagger.infrastructure.service.AyanAgentService;
import com.djtools.ayan.musictagger.infrastructure.service.ChatMessage;
import com.djtools.ayan.musictagger.infrastructure.service.ConversationHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = {
                AgentControllerIT.TestConfig.class,
                RedisConfig.class,
                ConversationHistoryService.class,
                AyanAgentService.class,
                AgentController.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@EnableAutoConfiguration(exclude = {OllamaApiAutoConfiguration.class, OllamaChatAutoConfiguration.class, OllamaEmbeddingAutoConfiguration.class})
@Testcontainers
class AgentControllerIT {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Configuration
    static class TestConfig {
        @Bean
        ChatClient chatClient() {
            var requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
            var callResponseSpec = mock(ChatClient.CallResponseSpec.class);
            var client = mock(ChatClient.class);

            when(client.prompt()).thenReturn(requestSpec);
            when(requestSpec.user(anyString())).thenReturn(requestSpec);
            when(requestSpec.call()).thenReturn(callResponseSpec);
            when(callResponseSpec.content()).thenReturn("Réponse d'Ayan !");

            return client;
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired ConversationHistoryService historyService;
    @Autowired RedisTemplate<String, Object> redisTemplate;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        var keys = redisTemplate.keys("conversation:*");
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
                .andExpect(jsonPath("$.messageCount").value(2))
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
        historyService.saveMessage("it-rest-2", new ChatMessage("user", "Test message", LocalDateTime.now()));
        historyService.saveMessage("it-rest-2", new ChatMessage("assistant", "Test reply", LocalDateTime.now()));

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
        historyService.saveMessage("it-rest-3", new ChatMessage("user", "To delete", LocalDateTime.now()));

        mockMvc.perform(delete("/api/agent/conversations/it-rest-3"))
                .andExpect(status().isOk());

        assertThat(historyService.getHistory("it-rest-3")).isEmpty();
    }

    @Test
    void fullConversationFlow_maintainsHistory() throws Exception {
        // Premier message
        mockMvc.perform(post("/api/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Scanne test.mp3", "conversationId": "it-flow"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageCount").value(2));

        // Deuxième message — l'historique grandit
        mockMvc.perform(post("/api/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Quels tags manquent ?", "conversationId": "it-flow"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageCount").value(4));

        // Vérifier l'historique complet
        mockMvc.perform(get("/api/agent/conversations/it-flow/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[0].content").value("Scanne test.mp3"))
                .andExpect(jsonPath("$[2].role").value("user"))
                .andExpect(jsonPath("$[2].content").value("Quels tags manquent ?"));

        // Supprimer la conversation
        mockMvc.perform(delete("/api/agent/conversations/it-flow"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/agent/conversations/it-flow/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
