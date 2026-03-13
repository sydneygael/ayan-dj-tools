package com.djtools.ayan.musictagger.infrastructure.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AyanAgentServiceTest {

    @Mock ChatClient chatClient;
    @Mock ConversationHistoryService historyService;
    @Mock ChatClient.ChatClientRequestSpec requestSpec;
    @Mock ChatClient.CallResponseSpec callResponseSpec;

    AyanAgentService service;

    @BeforeEach
    void setUp() {
        service = new AyanAgentService(chatClient, historyService);
        lenient().when(chatClient.prompt()).thenReturn(requestSpec);
        lenient().when(requestSpec.user(anyString())).thenReturn(requestSpec);
        lenient().when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    @Test
    void chat_savesUserAndAssistantMessages() {
        when(callResponseSpec.content()).thenReturn("Bonjour !");
        when(historyService.getHistory("conv-1")).thenReturn(List.of(
                new ChatMessage("user", "Salut", LocalDateTime.now())
        ));

        service.chat("conv-1", "Salut");

        var captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(historyService, times(2)).saveMessage(eq("conv-1"), captor.capture());

        assertThat(captor.getAllValues().get(0).role()).isEqualTo("user");
        assertThat(captor.getAllValues().get(1).role()).isEqualTo("assistant");
        assertThat(captor.getAllValues().get(1).content()).isEqualTo("Bonjour !");
    }

    @Test
    void chat_generatesConversationIdWhenNull() {
        when(callResponseSpec.content()).thenReturn("Réponse");
        when(historyService.getHistory(anyString())).thenReturn(List.of());

        String reply = service.chat(null, "Hello");

        assertThat(reply).isEqualTo("Réponse");
        verify(historyService, times(2)).saveMessage(argThat(id -> !id.isBlank()), any());
    }

    @Test
    void chat_buildsContextualPromptWithHistory() {
        when(callResponseSpec.content()).thenReturn("Je me souviens !");
        when(historyService.getHistory("conv-1")).thenReturn(List.of(
                new ChatMessage("user", "Scanne test.mp3", LocalDateTime.now()),
                new ChatMessage("assistant", "Fichier scanné", LocalDateTime.now()),
                new ChatMessage("user", "Quels tags manquent ?", LocalDateTime.now())
        ));

        service.chat("conv-1", "Quels tags manquent ?");

        var captor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(captor.capture());
        assertThat(captor.getValue()).contains("Contexte de la conversation");
        assertThat(captor.getValue()).contains("Scanne test.mp3");
        assertThat(captor.getValue()).contains("Quels tags manquent ?");
    }

    @Test
    void chat_sendsDirectMessageWhenNoHistory() {
        when(callResponseSpec.content()).thenReturn("Salut !");
        when(historyService.getHistory(anyString())).thenReturn(List.of(
                new ChatMessage("user", "Bonjour", LocalDateTime.now())
        ));

        service.chat("conv-1", "Bonjour");

        var captor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(captor.capture());
        // With single message history (<=1), prompt is mode prefix + message (no conversation context)
        assertThat(captor.getValue()).contains("Bonjour");
        assertThat(captor.getValue()).doesNotContain("Contexte de la conversation");
    }
}
