package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.OperatingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AyanAgentServiceTest {

    @Mock ChatClient chatClient;
    @Mock ChatClient.ChatClientRequestSpec requestSpec;
    @Mock ChatClient.CallResponseSpec callResponseSpec;

    AyanAgentService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new AyanAgentService(chatClient, 120);
        lenient().when(chatClient.prompt()).thenReturn(requestSpec);
        lenient().when(requestSpec.user(anyString())).thenReturn(requestSpec);
        lenient().when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        lenient().when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    @Test
    void chatWithToolCalls_generatesConversationIdWhenNull() {
        var result = service.chatWithToolCalls(null, "Hello", OperatingMode.PLAN, null, null);
        assertThat(result.conversationId()).isNotBlank();
    }

    @Test
    void chatWithToolCalls_generatesConversationIdWhenBlank() {
        var result = service.chatWithToolCalls("  ", "Hello", OperatingMode.PLAN, null, null);
        assertThat(result.conversationId()).isNotBlank();
        assertThat(result.conversationId()).isNotEqualTo("  ");
    }

    @Test
    void chatWithToolCalls_usesProvidedConversationId() {
        var result = service.chatWithToolCalls("conv-42", "Hello", OperatingMode.PLAN, null, null);
        assertThat(result.conversationId()).isEqualTo("conv-42");
    }

    @Test
    @SuppressWarnings("unchecked")
    void chatWithToolCalls_injectsContextPrefixWithMode() {
        var captor = ArgumentCaptor.forClass(String.class);

        service.chatWithToolCalls("conv-1", "Quels tags manquent ?", OperatingMode.APPLY,
                List.of("/music/test.mp3"), "/music");

        verify(requestSpec).user(captor.capture());
        assertThat(captor.getValue())
                .contains("[Contexte:")
                .contains("mode=APPLY")
                .contains("Quels tags manquent ?");
    }

    @Test
    @SuppressWarnings("unchecked")
    void chatWithToolCalls_passesConversationIdViaAdvisorParam() {
        service.chatWithToolCalls("conv-1", "Hello", OperatingMode.PLAN, null, null);
        verify(requestSpec).advisors(any(Consumer.class));
    }

    @Test
    void chatWithToolCalls_returnsEmptyToolCallsWhenNull() {
        var result = service.chatWithToolCalls("conv-1", "Hello", OperatingMode.PLAN, null, null);
        assertThat(result.toolCalls()).isEmpty();
    }
}
