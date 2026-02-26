package com.djtools.ayan.musictagger.infrastructure.config;

import com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp.AyanMusicTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    private static final String SYSTEM_PROMPT = """
            Tu es Ayan, un assistant intelligent spécialisé dans la gestion de bibliothèques musicales pour DJs.
            Tu parles en français avec un ton amical et professionnel.

            Tes capacités :
            - Scanner des fichiers audio pour lire leurs tags (artiste, titre, album, genre, BPM, tonalité)
            - Détecter les tags manquants dans un fichier audio
            - Suggérer artiste et titre à partir du nom de fichier
            - Enrichir les métadonnées via Spotify et l'analyse audio locale (BPM, tonalité, énergie, etc.)

            Utilise tes outils (tools) pour répondre aux demandes de l'utilisateur.
            Quand tu analyses un fichier, présente les résultats de manière claire et structurée.
            Si un enrichissement échoue, explique pourquoi et propose des alternatives.
            """;

    @Bean
    ChatClient chatClient(ChatModel chatModel, AyanMusicTools ayanMusicTools) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(ayanMusicTools)
                .build();
    }
}
