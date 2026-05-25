package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.OperatingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AyanAgentServiceTest {

    @Mock ChatClient chatClient;
    @Mock ConversationHistoryService historyService;
    @Mock ChatClient.ChatClientRequestSpec requestSpec;
    @Mock ChatClient.CallResponseSpec callResponseSpec;

    AyanAgentService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new AyanAgentService(chatClient, historyService);
        lenient().when(chatClient.prompt()).thenReturn(requestSpec);
        lenient().when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        lenient().when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    @Test
    void chatWithToolCalls_savesUserAndAssistantMessages() {
        when(historyService.getHistory("conv-1")).thenReturn(List.of(
                new ChatMessage("user", "Salut", LocalDateTime.now())
        ));

        service.chatWithToolCalls("conv-1", "Salut", OperatingMode.PLAN, null, null);

        var captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(historyService, times(2)).saveMessage(eq("conv-1"), captor.capture());

        assertThat(captor.getAllValues().get(0).role()).isEqualTo("user");
        assertThat(captor.getAllValues().get(1).role()).isEqualTo("assistant");
    }

    @Test
    void chatWithToolCalls_generatesConversationIdWhenNull() {
        when(historyService.getHistory(anyString())).thenReturn(List.of(
                new ChatMessage("user", "Hello", LocalDateTime.now())
        ));

        service.chatWithToolCalls(null, "Hello", OperatingMode.PLAN, null, null);

        verify(historyService, times(2)).saveMessage(argThat(id -> !id.isBlank()), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void chatWithToolCalls_includesPreviousMessagesAsSpringAiTypes() {
        when(historyService.getHistory("conv-1")).thenReturn(List.of(
                new ChatMessage("user", "Scanne test.mp3", LocalDateTime.now()),
                new ChatMessage("assistant", "Fichier scanné", LocalDateTime.now()),
                new ChatMessage("user", "Quels tags manquent ?", LocalDateTime.now())
        ));

        service.chatWithToolCalls("conv-1", "Quels tags manquent ?", OperatingMode.PLAN, null, null);

        var captor = ArgumentCaptor.forClass(List.class);
        verify(requestSpec).messages(captor.capture());
        List<Message> messages = (List<Message>) captor.getValue();

        // 2 messages précédents + 1 message courant
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getText()).isEqualTo("Scanne test.mp3");
        assertThat(messages.get(1).getText()).isEqualTo("Fichier scanné");
        assertThat(messages.get(2).getText()).contains("Quels tags manquent ?");
    }

    @Test
    @SuppressWarnings("unchecked")
    void chatWithToolCalls_sendsOnlyCurrentMessageWhenNoHistory() {
        when(historyService.getHistory(anyString())).thenReturn(List.of(
                new ChatMessage("user", "Bonjour", LocalDateTime.now())
        ));

        service.chatWithToolCalls("conv-1", "Bonjour", OperatingMode.PLAN, null, null);

        var captor = ArgumentCaptor.forClass(List.class);
        verify(requestSpec).messages(captor.capture());
        List<Message> messages = (List<Message>) captor.getValue();

        // Aucun historique précédent — juste le message courant
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(0).getText()).contains("Bonjour");
    }
}
