package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.infrastructure.service.AyanAgentService;
import com.djtools.ayan.musictagger.infrastructure.service.ChatMessage;
import com.djtools.ayan.musictagger.infrastructure.service.ConversationHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentControllerTest {

    @Mock AyanAgentService agentService;
    @Mock ConversationHistoryService historyService;
    @InjectMocks AgentController controller;

    @Test
    void chat_returnsAgentReplyWithConversationId() {
        when(agentService.chat(eq("conv-1"), eq("Analyse mon fichier"))).thenReturn("Voici l'analyse...");
        when(historyService.getMessageCount("conv-1")).thenReturn(2L);

        var response = controller.chat(new AgentController.ChatRequest("Analyse mon fichier", "conv-1"));

        assertThat(response.reply()).isEqualTo("Voici l'analyse...");
        assertThat(response.conversationId()).isEqualTo("conv-1");
        assertThat(response.messageCount()).isEqualTo(2);
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void chat_generatesConversationIdWhenAbsent() {
        when(agentService.chat(anyString(), eq("Salut"))).thenReturn("Bonjour !");
        when(historyService.getMessageCount(anyString())).thenReturn(2L);

        var response = controller.chat(new AgentController.ChatRequest("Salut", null));

        assertThat(response.conversationId()).isNotNull().isNotBlank();
        assertThat(response.reply()).isEqualTo("Bonjour !");
    }

    @Test
    void getHistory_delegatesToService() {
        var messages = List.of(new ChatMessage("user", "Test", LocalDateTime.now()));
        when(historyService.getHistory("conv-1")).thenReturn(messages);

        var result = controller.getHistory("conv-1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().content()).isEqualTo("Test");
    }

    @Test
    void clearConversation_delegatesToService() {
        controller.clearConversation("conv-1");

        verify(historyService).clearHistory("conv-1");
    }
}
