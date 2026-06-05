package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.OperatingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AyanAgentServiceTest {

    @Mock AyanAssistant fichiersAssistant;
    @Mock AyanAssistant planAssistant;
    @Mock AyanAssistant rechercheAssistant;
    @Mock AyanAssistant playlistAssistant;
    @Mock AyanAssistant decouverteAssistant;
    @Mock AyanAssistant generalAssistant;

    AyanAgentService service;

    @BeforeEach
    void setUp() {
        when(generalAssistant.chatSync(anyString(), anyString())).thenReturn("Bonjour !");
        when(fichiersAssistant.chatSync(anyString(), anyString())).thenReturn("Scan effectué.");
        when(planAssistant.chatSync(anyString(), anyString())).thenReturn("Plan créé.");
        when(decouverteAssistant.chatSync(anyString(), anyString())).thenReturn("Wizkid est un artiste afrobeats.");

        // Classifier: route by keyword for tests
        Function<String, IntentType> testClassifier = msg -> {
            final var lower = msg.toLowerCase();
            if (lower.contains("scan") || lower.contains("fichier")) return IntentType.FICHIERS;
            if (lower.contains("plan")) return IntentType.PLANIFICATION;
            if (lower.contains("similaire") || lower.contains("cherche")) return IntentType.RECHERCHE;
            if (lower.contains("playlist")) return IntentType.PLAYLIST;
            if (lower.contains("qui est") || lower.contains("artiste")) return IntentType.DECOUVERTE;
            return IntentType.GENERAL;
        };

        service = new AyanAgentService(
                fichiersAssistant, planAssistant, rechercheAssistant,
                playlistAssistant, decouverteAssistant, generalAssistant,
                testClassifier, 600, 60);
    }

    @Test
    void chatWithToolCalls_generatesConversationIdWhenNull() {
        var result = service.chatWithToolCalls(null, "bonjour", OperatingMode.PLAN, null, null);
        assertThat(result.conversationId()).isNotBlank();
    }

    @Test
    void chatWithToolCalls_usesProvidedConversationId() {
        var result = service.chatWithToolCalls("conv-42", "bonjour", OperatingMode.PLAN, null, null);
        assertThat(result.conversationId()).isEqualTo("conv-42");
    }

    @Test
    void chatWithToolCalls_routesToGeneralForGreeting() {
        var result = service.chatWithToolCalls("c1", "bonjour", OperatingMode.PLAN, null, null);
        assertThat(result.reply()).isEqualTo("Bonjour !");
    }

    @Test
    void chatWithToolCalls_routesToFichiersForScan() {
        var result = service.chatWithToolCalls("c1", "scan ce fichier", OperatingMode.PLAN, null, null);
        assertThat(result.reply()).isEqualTo("Scan effectué.");
    }

    @Test
    void chatWithToolCalls_forcesPlannificationForApplyMode() {
        var result = service.chatWithToolCalls("c1", "applique", OperatingMode.APPLY, null, null);
        assertThat(result.reply()).isEqualTo("Plan créé.");
    }

    @Test
    void chatWithToolCalls_routesToDecouverteForArtistQuery() {
        var result = service.chatWithToolCalls("c1", "qui est wizkid artiste", OperatingMode.PLAN, null, null);
        assertThat(result.reply()).isEqualTo("Wizkid est un artiste afrobeats.");
    }

    @Test
    void chatWithToolCalls_returnsEmptyToolCalls() {
        var result = service.chatWithToolCalls("c1", "bonjour", OperatingMode.PLAN, null, null);
        assertThat(result.toolCalls()).isEmpty();
    }
}
