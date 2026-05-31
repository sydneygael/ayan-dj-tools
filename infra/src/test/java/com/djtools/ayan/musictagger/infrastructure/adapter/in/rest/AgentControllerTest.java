package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.OperatingMode;
import com.djtools.ayan.musictagger.infrastructure.service.AyanAgentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentControllerTest {

    @Mock AyanAgentService agentService;
    @Mock ChatMemory chatMemory;
    @InjectMocks AgentController controller;

    @Test
    void chat_returnsAgentReplyWithConversationId() {
        when(agentService.chatWithToolCalls(eq("conv-1"), eq("Analyse mon fichier"),
                eq(OperatingMode.PLAN), any(), any()))
                .thenReturn(new AyanAgentService.ChatResult("Voici l'analyse...", "conv-1", List.of()));
        when(chatMemory.get("conv-1")).thenReturn(List.of(
                new UserMessage("Analyse mon fichier"),
                new AssistantMessage("Voici l'analyse...")
        ));

        var response = controller.chat(new AgentController.ChatRequest(
                "Analyse mon fichier", "conv-1", OperatingMode.PLAN, List.of(), null));

        assertThat(response.reply()).isEqualTo("Voici l'analyse...");
        assertThat(response.conversationId()).isEqualTo("conv-1");
        assertThat(response.messageCount()).isEqualTo(2);
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.toolCalls()).isEmpty();
    }

    @Test
    void chat_generatesConversationIdWhenAbsent() {
        when(agentService.chatWithToolCalls(anyString(), eq("Salut"),
                eq(OperatingMode.PLAN), any(), any()))
                .thenAnswer(inv -> new AyanAgentService.ChatResult("Bonjour !", inv.getArgument(0), List.of()));
        when(chatMemory.get(anyString())).thenReturn(List.of());

        var response = controller.chat(new AgentController.ChatRequest(
                "Salut", null, null, null, null));

        assertThat(response.conversationId()).isNotNull().isNotBlank();
        assertThat(response.reply()).isEqualTo("Bonjour !");
    }

    @Test
    void chat_forwardsContextToAgent() {
        when(agentService.chatWithToolCalls(anyString(), anyString(), any(), any(), any()))
                .thenReturn(new AyanAgentService.ChatResult("ok", "c1", List.of()));
        when(chatMemory.get(anyString())).thenReturn(List.of());

        controller.chat(new AgentController.ChatRequest(
                "tag ça", "c1", OperatingMode.APPLY, List.of("C:/a.mp3", "C:/b.mp3"), "C:/music"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> paths = ArgumentCaptor.forClass(List.class);
        verify(agentService).chatWithToolCalls(eq("c1"), eq("tag ça"),
                eq(OperatingMode.APPLY), paths.capture(), eq("C:/music"));
        assertThat(paths.getValue()).containsExactly("C:/a.mp3", "C:/b.mp3");
    }

    @Test
    void getHistory_returnsMessagesFromChatMemory() {
        when(chatMemory.get("conv-1")).thenReturn(List.of(
                new UserMessage("Test"),
                new AssistantMessage("Réponse")
        ));

        var result = controller.getHistory("conv-1");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).role()).isEqualTo("user");
        assertThat(result.get(0).content()).isEqualTo("Test");
        assertThat(result.get(1).role()).isEqualTo("assistant");
        assertThat(result.get(1).content()).isEqualTo("Réponse");
    }

    @Test
    void getHistory_returnsEmptyListWhenNoMessages() {
        when(chatMemory.get("conv-empty")).thenReturn(List.of());

        var result = controller.getHistory("conv-empty");

        assertThat(result).isEmpty();
    }

    @Test
    void clearConversation_delegatesToChatMemory() {
        controller.clearConversation("conv-1");
        verify(chatMemory).clear("conv-1");
    }
}
