package com.djtools.ayan.musictagger.infrastructure.adapter.in.ws;

import com.djtools.ayan.musictagger.infrastructure.service.AyanAgentService;
import com.djtools.ayan.musictagger.infrastructure.service.ConversationHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentWebSocketControllerTest {

    @Mock AyanAgentService agentService;
    @Mock ConversationHistoryService historyService;
    @Mock SimpMessagingTemplate messagingTemplate;
    @InjectMocks AgentWebSocketController controller;

    @Test
    void handleChat_streamsChunksAndSendsDoneEvent() throws InterruptedException {
        when(agentService.chatStream(eq("conv-1"), eq("Scanne ce fichier")))
                .thenReturn(Flux.just("Fichier", " scanné", " !"));
        when(historyService.getMessageCount("conv-1")).thenReturn(2L);

        controller.handleChat(new AgentWebSocketController.ChatRequest("Scanne ce fichier", "conv-1"));

        // Attendre la fin du flux asynchrone
        Thread.sleep(200);

        var captor = ArgumentCaptor.forClass(AgentWebSocketController.ChatStreamEvent.class);
        verify(messagingTemplate, atLeast(1)).convertAndSend(eq("/topic/responses/conv-1"), captor.capture());

        final var events = captor.getAllValues();
        final var chunks = events.stream().filter(e -> "chunk".equals(e.type())).toList();
        final var doneEvents = events.stream().filter(e -> "done".equals(e.type())).toList();

        assertThat(chunks).hasSize(3);
        assertThat(chunks.stream().map(AgentWebSocketController.ChatStreamEvent::token).toList())
                .containsExactly("Fichier", " scanné", " !");
        assertThat(doneEvents).hasSize(1);
        assertThat(doneEvents.get(0).reply()).isEqualTo("Fichier scanné !");
        assertThat(doneEvents.get(0).conversationId()).isEqualTo("conv-1");
    }

    @Test
    void handleChat_generatesConversationIdWhenNull() throws InterruptedException {
        when(agentService.chatStream(anyString(), eq("Hello"))).thenReturn(Flux.just("Bonjour !"));
        when(historyService.getMessageCount(anyString())).thenReturn(1L);

        controller.handleChat(new AgentWebSocketController.ChatRequest("Hello", null));

        Thread.sleep(200);

        var captor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate, atLeast(1)).convertAndSend(captor.capture(), any(AgentWebSocketController.ChatStreamEvent.class));

        // Le topic doit contenir un UUID généré (non vide, non "null")
        final var topic = captor.getAllValues().stream()
                .filter(t -> t.startsWith("/topic/responses/"))
                .findFirst().orElseThrow();
        final var generatedId = topic.replace("/topic/responses/", "");
        assertThat(generatedId).isNotBlank().doesNotContain("null");
    }

    @Test
    void handleChat_sendsErrorEventOnFailure() throws InterruptedException {
        when(agentService.chatStream(eq("conv-err"), eq("Erreur ?")))
                .thenReturn(Flux.error(new RuntimeException("Ollama timeout")));

        controller.handleChat(new AgentWebSocketController.ChatRequest("Erreur ?", "conv-err"));

        Thread.sleep(200);

        var captor = ArgumentCaptor.forClass(AgentWebSocketController.ChatStreamEvent.class);
        verify(messagingTemplate, atLeast(1)).convertAndSend(eq("/topic/responses/conv-err"), captor.capture());

        final var errorEvents = captor.getAllValues().stream().filter(e -> "error".equals(e.type())).toList();
        assertThat(errorEvents).hasSize(1);
        assertThat(errorEvents.get(0).token()).contains("Ollama timeout");
    }
}
