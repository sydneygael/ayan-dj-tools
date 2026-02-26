package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.infrastructure.service.AyanAgentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentControllerTest {

    @Mock AyanAgentService agentService;
    @InjectMocks AgentController controller;

    @Test
    void chat_returnsAgentReply() {
        when(agentService.chat("Analyse mon fichier")).thenReturn("Voici l'analyse...");

        var response = controller.chat(new AgentController.ChatRequest("Analyse mon fichier"));

        assertThat(response.reply()).isEqualTo("Voici l'analyse...");
    }
}
