package com.djtools.ayan.musictagger.infrastructure.config;

import com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp.DiscoveryTools;
import com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp.FileOpsTools;
import com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp.PlaylistTools;
import com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp.PlanTools;
import com.djtools.ayan.musictagger.infrastructure.adapter.in.mcp.SearchTools;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence.RedisChatMemoryRepository;
import com.djtools.ayan.musictagger.infrastructure.service.AyanAssistant;
import com.djtools.ayan.musictagger.infrastructure.service.IntentType;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

@Configuration
public class AIConfig {

    private static final Logger log = LoggerFactory.getLogger(AIConfig.class);

    // ── Classifier interface (inline, fast) ───────────────────────────────────

    interface IntentClassifier {
        @SystemMessage("""
                Tu es un classificateur d'intention. Réponds UNIQUEMENT avec un seul mot parmi :
                FICHIERS, PLANIFICATION, RECHERCHE, PLAYLIST, DECOUVERTE, GENERAL

                FICHIERS       : scanner, analyser, lire tags, enrichir, BPM, tonalité, parcourir dossier
                PLANIFICATION  : créer plan, appliquer plan, mode manuel, prévisualiser modifications
                RECHERCHE      : chercher morceaux similaires, filtrer par genre/BPM/énergie/ambiance
                PLAYLIST       : générer playlist, loop mixing, mix harmonique, roue de Camelot
                DECOUVERTE     : qui est cet artiste, infos sur album, recherche externe
                GENERAL        : bonjour, que peux-tu faire, aide, questions générales

                Réponds avec EXACTEMENT un mot, sans ponctuation, sans explication.
                """)
        String classify(@UserMessage String message);
    }

    // ── Prompts système par agent ─────────────────────────────────────────────

    private static final String PROMPT_FICHIERS = """
            Tu es un agent spécialisé dans les opérations sur fichiers audio pour DJs.
            Tu peux scanner des fichiers, détecter les tags manquants, enrichir via Spotify et parcourir des dossiers.
            Réponds en français, sans markdown. Format : Artiste : X  |  Titre : Y  |  BPM : Z
            Symboles : ✓ succès  ✗ manquant  ⚠ avertissement
            [Contexte: mode=#{mode}; filePaths=#{filePaths}; currentDir=#{currentDir}]
            """;

    private static final String PROMPT_PLANIFICATION = """
            Tu es un agent spécialisé dans les plans de modification de tags pour DJs.
            Tu peux créer des plans, les appliquer, gérer le mode manuel (fichier par fichier) et prévisualiser les changements.
            Réponds en français, sans markdown.
            [Contexte: mode=#{mode}; filePaths=#{filePaths}; currentDir=#{currentDir}]
            """;

    private static final String PROMPT_RECHERCHE = """
            Tu es un agent spécialisé dans la recherche musicale dans la collection locale.
            Tu peux trouver des morceaux similaires (RAG), filtrer par critères (genre, BPM, énergie, ambiance, années)
            et suggérer des tags intelligents basés sur des morceaux proches.
            Réponds en français, sans markdown. Par résultat : Artiste | Titre | BPM | Tonalité
            """;

    private static final String PROMPT_PLAYLIST = """
            Tu es un agent spécialisé dans la génération de playlists DJ.
            Tu génères des playlists loop mixing (plage de BPM) ou harmoniques via la roue de Camelot.
            Réponds en français, sans markdown. Numérote chaque morceau avec BPM et tonalité Camelot.
            """;

    private static final String PROMPT_DECOUVERTE = """
            Tu es un agent spécialisé dans la découverte musicale via sources externes.
            Tu peux chercher des informations sur des artistes, albums et morceaux via Soundcharts, Internet et Spotify.
            Appelle lookupMusicInfo UNE SEULE FOIS puis formule la réponse directement.
            Si localTracks est non vide, mentionne les morceaux que l'utilisateur possède déjà.
            Réponds en français, sans markdown.
            """;

    private static final String PROMPT_GENERAL = """
            Tu es Ayan, un assistant intelligent pour DJs spécialisé dans la gestion de bibliothèques musicales.
            Tu parles en français avec un ton amical et professionnel.

            Quand l'utilisateur dit "bonjour", "salut", "hello" ou équivalent, réponds EXACTEMENT :

            Bonjour ! Je suis Ayan, ton assistant IA pour la gestion de bibliothèques musicales.

            Je peux t'aider à :

            FICHIERS — Scanner, analyser et enrichir tes fichiers audio
            ───────────────────────────────────────────────────────────
            Lire les tags actuels, détecter les manquants, enrichir via Spotify,
            parcourir ta bibliothèque.

            PLANS DE TAGS — Modifier les tags en toute sécurité
            ────────────────────────────────────────────────────
            Créer un plan de modifications, les appliquer en lot ou fichier par fichier,
            prévisualiser avant d'appliquer.

            RECHERCHE — Explorer ta collection
            ──────────────────────────────────
            Trouver des morceaux similaires, filtrer par genre, BPM, énergie ou ambiance.

            PLAYLISTS — Préparer tes sets
            ─────────────────────────────
            Générer des playlists loop mixing ou harmoniques via la roue de Camelot.

            DÉCOUVERTE — Informations externes
            ───────────────────────────────────
            Chercher des infos sur un artiste, album ou morceau via Soundcharts et Internet.

            Comment puis-je t'aider ?
            """;

    // ── Language models ───────────────────────────────────────────────────────

    @Bean
    ChatLanguageModel chatLanguageModel(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.chat.model:llama3.1:8b}") String model,
            @Value("${ollama.chat.temperature:0.3}") Double temperature,
            @Value("${ollama.chat.num-ctx:8192}") Integer numCtx) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl).modelName(model)
                .temperature(temperature).numCtx(numCtx)
                .timeout(Duration.ofSeconds(600))
                .build();
    }

    @Bean
    StreamingChatLanguageModel streamingChatLanguageModel(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.chat.model:llama3.1:8b}") String model,
            @Value("${ollama.chat.temperature:0.3}") Double temperature,
            @Value("${ollama.chat.num-ctx:8192}") Integer numCtx) {
        return OllamaStreamingChatModel.builder()
                .baseUrl(baseUrl).modelName(model)
                .temperature(temperature).numCtx(numCtx)
                .timeout(Duration.ofSeconds(600))
                .build();
    }

    /** Modèle rapide pour la classification d'intention : contexte minimal, sortie = 1 mot. */
    @Bean("fastChatModel")
    ChatLanguageModel fastChatModel(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.chat.model:llama3.1:8b}") String model) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl).modelName(model)
                .temperature(0.0).numCtx(512).numPredict(10)
                .timeout(Duration.ofSeconds(30))
                .build();
    }

    /** Modèle streaming léger pour les réponses générales (sans tools). */
    @Bean("generalStreamingModel")
    StreamingChatLanguageModel generalStreamingModel(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.chat.model:llama3.1:8b}") String model,
            @Value("${ollama.chat.temperature:0.3}") Double temperature,
            @Value("${dj-tagger.chat.simple-timeout-seconds:60}") Integer simpleTimeoutSeconds) {
        return OllamaStreamingChatModel.builder()
                .baseUrl(baseUrl).modelName(model)
                .temperature(temperature).numCtx(4096)
                .timeout(Duration.ofSeconds(simpleTimeoutSeconds))
                .build();
    }

    @Bean
    EmbeddingModel embeddingModel(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.embedding.model:nomic-embed-text}") String model) {
        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl).modelName(model)
                .build();
    }

    // ── Intent classifier ─────────────────────────────────────────────────────

    @Bean
    IntentClassifier intentClassifier(@Qualifier("fastChatModel") ChatLanguageModel fastModel) {
        return AiServices.builder(IntentClassifier.class)
                .chatLanguageModel(fastModel)
                .build();
    }

    // ── Shared memory provider helper ─────────────────────────────────────────

    private static dev.langchain4j.memory.ChatMemory memory(
            Object convId, RedisChatMemoryRepository store) {
        return MessageWindowChatMemory.builder()
                .id(convId).maxMessages(20).chatMemoryStore(store)
                .build();
    }

    // ── 6 specialized assistants — each with ≤4 tools ────────────────────────

    @Bean("fichiersAssistant")
    AyanAssistant fichiersAssistant(
            @Qualifier("chatLanguageModel") ChatLanguageModel chatLM,
            FileOpsTools fileOpsTools,
            RedisChatMemoryRepository memoryStore) {
        return AiServices.builder(AyanAssistant.class)
                .systemMessageProvider(id -> PROMPT_FICHIERS)
                .chatLanguageModel(chatLM)
                .tools(fileOpsTools)
                .chatMemoryProvider(id -> memory(id, memoryStore))
                .build();
    }

    @Bean("planAssistant")
    AyanAssistant planAssistant(
            @Qualifier("chatLanguageModel") ChatLanguageModel chatLM,
            PlanTools planTools,
            RedisChatMemoryRepository memoryStore) {
        return AiServices.builder(AyanAssistant.class)
                .systemMessageProvider(id -> PROMPT_PLANIFICATION)
                .chatLanguageModel(chatLM)
                .tools(planTools)
                .chatMemoryProvider(id -> memory(id, memoryStore))
                .build();
    }

    @Bean("rechercheAssistant")
    AyanAssistant rechercheAssistant(
            @Qualifier("chatLanguageModel") ChatLanguageModel chatLM,
            SearchTools searchTools,
            RedisChatMemoryRepository memoryStore) {
        return AiServices.builder(AyanAssistant.class)
                .systemMessageProvider(id -> PROMPT_RECHERCHE)
                .chatLanguageModel(chatLM)
                .tools(searchTools)
                .chatMemoryProvider(id -> memory(id, memoryStore))
                .build();
    }

    @Bean("playlistAssistant")
    AyanAssistant playlistAssistant(
            @Qualifier("chatLanguageModel") ChatLanguageModel chatLM,
            PlaylistTools playlistTools,
            RedisChatMemoryRepository memoryStore) {
        return AiServices.builder(AyanAssistant.class)
                .systemMessageProvider(id -> PROMPT_PLAYLIST)
                .chatLanguageModel(chatLM)
                .tools(playlistTools)
                .chatMemoryProvider(id -> memory(id, memoryStore))
                .build();
    }

    @Bean("decouverteAssistant")
    AyanAssistant decouverteAssistant(
            @Qualifier("chatLanguageModel") ChatLanguageModel chatLM,
            DiscoveryTools discoveryTools,
            RedisChatMemoryRepository memoryStore) {
        return AiServices.builder(AyanAssistant.class)
                .systemMessageProvider(id -> PROMPT_DECOUVERTE)
                .chatLanguageModel(chatLM)
                .tools(discoveryTools)
                .chatMemoryProvider(id -> memory(id, memoryStore))
                .build();
    }

    @Bean("generalAssistant")
    AyanAssistant generalAssistant(
            @Qualifier("generalStreamingModel") StreamingChatLanguageModel generalStreamingLM,
            @Qualifier("chatLanguageModel") ChatLanguageModel chatLM,
            RedisChatMemoryRepository memoryStore) {
        return AiServices.builder(AyanAssistant.class)
                .systemMessageProvider(id -> PROMPT_GENERAL)
                .chatLanguageModel(chatLM)
                .streamingChatLanguageModel(generalStreamingLM)
                // no tools — purely conversational
                .chatMemoryProvider(id -> memory(id, memoryStore))
                .build();
    }

    // ── Public classifier bean for AyanAgentService ───────────────────────────

    /**
     * Classifie l'intention d'un message utilisateur.
     * Retourne GENERAL si la classification échoue (fallback garanti).
     */
    @Bean
    java.util.function.Function<String, IntentType> intentClassifierFunction(IntentClassifier classifier) {
        return message -> {
            try {
                final var raw = classifier.classify(message);
                if (raw == null || raw.isBlank()) return IntentType.GENERAL;
                return IntentType.valueOf(raw.strip().toUpperCase());
            } catch (Exception e) {
                log.warn("Intent classification failed for '{}': {} — fallback GENERAL",
                        message.substring(0, Math.min(40, message.length())), e.getMessage());
                return IntentType.GENERAL;
            }
        };
    }
}
