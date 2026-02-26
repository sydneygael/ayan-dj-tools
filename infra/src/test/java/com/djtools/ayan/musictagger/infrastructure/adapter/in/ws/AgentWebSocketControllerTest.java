package com.djtools.ayan.musictagger.infrastructure.adapter.in.ws;

import com.djtools.ayan.musictagger.infrastructure.service.AyanAgentService;
import com.djtools.ayan.musictagger.infrastructure.service.ConversationHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentWebSocketControllerTest {

    @Mock AyanAgentService agentService;
    @Mock ConversationHistoryService historyService;
    @InjectMocks AgentWebSocketController controller;

    @Test
    void handleChat_returnsResponseWithConversationId() {
        when(agentService.chat(eq("conv-1"), eq("Scanne ce fichier"))).thenReturn("Fichier scanné !");
        when(historyService.getMessageCount("conv-1")).thenReturn(2L);

        var response = controller.handleChat(new AgentWebSocketController.ChatRequest("Scanne ce fichier", "conv-1"));

        assertThat(response.reply()).isEqualTo("Fichier scanné !");
        assertThat(response.conversationId()).isEqualTo("conv-1");
        assertThat(response.messageCount()).isEqualTo(2);
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void handleChat_generatesConversationIdWhenNull() {
        when(agentService.chat(anyString(), eq("Hello"))).thenReturn("Bonjour !");
        when(historyService.getMessageCount(anyString())).thenReturn(2L);

        var response = controller.handleChat(new AgentWebSocketController.ChatRequest("Hello", null));

        assertThat(response.conversationId()).isNotNull().isNotBlank();
        assertThat(response.reply()).isEqualTo("Bonjour !");
    }
}
