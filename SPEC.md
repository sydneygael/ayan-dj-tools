# DJ Music Tagger - Project Specification

**Nom de l'agent IA** : **Ayan** (inspiré du tambour sacré yoruba)

## 📚 Guide d'Utilisation des Skills

Ce projet utilise un système de **skills spécialisés** pour organiser les bonnes pratiques et patterns de développement.

### Skills Disponibles

#### 🏗️ Architecture & Bonnes Pratiques
- **`hexagonal-ddd.skill.md`** - Architecture hexagonale + DDD (2 couches, simple & concis)
  - Structure domain/infrastructure
  - Use cases, ports, adapters
  - Value Objects, entités, agrégats
  - Sécurité (fichiers autorisés uniquement)

#### 💻 Backend
- **`backend-java.skill.md`** - Java 25, Spring Boot 4.0.2, Gradle 9.2.1
  - Records pattern
  - Services, controllers, repositories
  - Configuration, tests
  
- **`spring-ai.skill.md`** - Spring AI 2.0.0-M2 + MCP
  - @Tool functions
  - Structured outputs
  - Harmonic playlist generator (Camelot Wheel)
  - Gestion questions agent
  
- **`spotify-integration.skill.md`** - API Spotify
  - @HttpExchange client déclaratif
  - OAuth2, cache, rate limiting
  - Records réponses Spotify
  
- **`audio-processing.skill.md`** - JAudiotagger
  - Lecture/écriture tags
  - Validation fichiers
  - Backup & restore
  
- **`rag-vectordb.skill.md`** - Qdrant + RAG
  - Vectorisation
  - Recherche similarité
  - Filtres métadonnées

#### 🎨 Frontend
- **`frontend-angular.skill.md`** - Angular 21 + Electron + Material
  - Standalone components
  - Signals, inject(), input()/output()
  - Control flow (@if, @for, @switch)
  - File picker sécurisé
  - WebSocket temps réel

### Comment Utiliser les Skills

```bash
# Lors du développement, référencez le skill approprié

# Architecture globale
"Organise le code selon hexagonal-ddd.skill.md"

# Backend
"Crée le service d'enrichissement Spotify selon spotify-integration.skill.md"
"Implémente les fonction MCP selon spring-ai.skill.md"
"Configure Qdrant selon rag-vectordb.skill.md"

# Frontend
"Crée le composant file-picker selon frontend-angular.skill.md"
"Utilise les patterns Angular 21 de frontend-angular.skill.md"
```

---

## 📑 Table des Matières

1. [Vue d'ensemble](#vue-densemble)
2. [Stack Technique](#stack-technique)
3. [Architecture Globale](#architecture-globale)
4. [Fonctionnalités Principales](#fonctionnalités-principales)
5. [Structure Projet Backend](#structure-du-projet-backend-architecture-hexagonale--ddd)
6. [Records Clés](#records-clés)
7. [Fonctions MCP (@Tool)](#fonctions-mcp-tool)
8. [Configuration](#configuration)
9. [Plan d'Implémentation (13 Phases)](#plan-dimplémentation-13-phases)
10. [Commandes Démarrage](#commandes-démarrage)

---

## Vue d'ensemble

Application desktop pour DJ permettant de gérer et enrichir automatiquement les tags de fichiers audio locaux avec l'aide d'Ayan, un agent IA. L'application scanne les fichiers audio (sélectionnés manuellement), détecte les tags manquants, enrichit les métadonnées via l'API Spotify, et propose des modifications que l'utilisateur peut valider selon différents modes.

## Stack Technique

> **📖 Voir les skills** pour détails d'implémentation

### Backend
- **Java 25** (dernière version JDK)
- **Spring Boot 4.0.2** (stable)
- **Spring AI 2.0.0-M2** avec support Ollama/Mistral et Structured Outputs
  - Source: https://spring.io/blog/2026/01/23/spring-ai-2-0-0-M2-available-now
  - 📖 Skill: `spring-ai.skill.md`
- **Gradle 9.2.1** (build tool)
  - 📖 Skill: `backend-java.skill.md`
- **PostgreSQL** avec pgvector (base de données)
- **JAudiotagger** (manipulation des tags audio ID3, FLAC, etc.)
  - 📖 Skill: `audio-processing.skill.md`
- **Spotify API** (enrichissement métadonnées)
  - 📖 Skill: `spotify-integration.skill.md`
- **Qdrant** (vector database pour RAG)
  - 📖 Skill: `rag-vectordb.skill.md`

### Frontend
- **Electron** (application desktop multiplateforme)
- **Angular 21** (framework frontend)
- **Angular Material 21** (composants UI Material Design 3)
- **TypeScript 5.x**
  - 📖 Skill: `frontend-angular.skill.md`

### Infrastructure Docker
- **Ollama** avec modèle Mistral (agent IA)
- **Qdrant** (vector store)
- **PostgreSQL** avec extension pgvector

## Architecture Globale

> **📖 Détails architecture**: `hexagonal-ddd.skill.md`

```
┌─────────────────────────────────────────────┐
│   Frontend (Electron + Angular 21)          │
│   - Interface chat avec agent               │
│   - Gestion des 3 modes (Plan/Manual/Apply) │
│   - Visualisation des modifications         │
│   - Confirmation des changements            │
└──────────────┬──────────────────────────────┘
               │ REST API / WebSocket
┌──────────────▼──────────────────────────────┐
│   Backend (Spring Boot 4 + Java 25)         │
│                                              │
│   ┌────────────────────────────────────┐   │
│   │  Spring AI + MCP Server            │   │
│   │  - Agent conversationnel           │   │
│   │  - Tool/Function calling           │   │
│   │  - RAG pour suggestions            │   │
│   └────────────────────────────────────┘   │
│                                              │
│   ┌────────────────────────────────────┐   │
│   │  Services Métier                   │   │
│   │  - AudioTagService (JAudiotagger)  │   │
│   │  - SpotifyEnrichmentService        │   │
│   │  - VectorStoreService (RAG)        │   │
│   │  - PlanManagementService           │   │
│   └────────────────────────────────────┘   │
│                                              │
│   ┌────────────────────────────────────┐   │
│   │  Repositories                      │   │
│   │  - PostgreSQL (données structurées)│   │
│   │  - Qdrant (embeddings vectoriels)  │   │
│   └────────────────────────────────────┘   │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│   Infrastructure Docker                     │
│   - Ollama (Mistral)                        │
│   - Qdrant                                  │
│   - PostgreSQL + pgvector                   │
└─────────────────────────────────────────────┘
```

## 📖 Comment Lire ce Document

Ce document `CLAUDE.md` contient la **spécification complète** du projet. 

### Organisation

- **Ce fichier** = Vue d'ensemble + architecture + plan d'implémentation
- **Skills (`/skills/`)** = Détails techniques et bonnes pratiques par technologie

### Workflow de Développement

1. **Lire CLAUDE.md** pour comprendre le contexte global
2. **Référencer le skill approprié** pour les détails d'implémentation
3. **Coder en suivant les patterns** du skill

### Exemple

```
Tâche: "Créer le service d'enrichissement Spotify"

1. Lire section "Enrichissement via Spotify" (CLAUDE.md)
   → Comprendre le besoin métier

2. Ouvrir spotify-integration.skill.md
   → Patterns @HttpExchange, OAuth2, cache

3. Implémenter selon le skill
   → Code conforme aux bonnes pratiques
```

---

## Fonctionnalités Principales

> **📖 Détails implémentation**: Voir skills référencés dans chaque section

### 1. Scan et Analyse

> **📖 Skill**: `audio-processing.skill.md` + `frontend-angular.skill.md` (file picker)

**Sécurité**: Seuls les fichiers **explicitement sélectionnés via l'interface** sont scannés.

- **File picker Electron** : Sélection manuelle multi-fichiers
- **Formats supportés** : MP3, FLAC, WAV, AIFF, M4A, OGG
- **Lecture tags existants** : Artiste, titre, album, genre, BPM, clé
- **Identification tags manquants** : Détection automatique

### 2. Enrichissement via Spotify

> **📖 Skill**: `spotify-integration.skill.md`

- Recherche automatique sur Spotify API
- Récupération des métadonnées riches :
  - Informations de base (artiste, titre, album, année)
  - Genres détaillés
  - Audio features (BPM, clé musicale, énergie, danceability, valence)
  - Popularité
- Stockage en base de données pour réutilisation
- Vectorisation pour recherche sémantique (RAG)

### 3. Agent IA avec MCP

> **📖 Skill**: `spring-ai.skill.md`

L'agent utilise Spring AI et expose des fonctions MCP (@Tool) :

#### Fonctions d'Analyse
- `scanMusicFile(filepath)` - Analyse un fichier et retourne ses tags
- `detectMissingTags(filepath)` - Liste les tags manquants
- `suggestTagsFromFilename(filename)` - Extrait infos du nom de fichier
- `analyzeAudioFeatures(filepath)` - Détecte BPM, clé (optionnel)

#### Fonctions d'Enrichissement
- `enrichWithSpotify(filepath, artist, title)` - Recherche et enrichit via Spotify
- `findSimilarTracks(query, limit)` - Recherche similarité dans RAG
- `smartSuggestTags(filepath)` - Suggestions intelligentes (Spotify + RAG)

#### Fonctions de Modification
- `previewTagUpdate(filepath, newTags)` - Prévisualise les changements
- `applyTags(filepath, tags)` - Applique les tags à un fichier
- `batchApplyTags(updates[], autoConfirm)` - Application batch

#### Fonctions de Plan
- `createPlanForFolder(folderPath)` - Génère un plan complet de modifications
- `approvePlan(planId)` - Valide un plan
- `executePlan(planId)` - Exécute un plan approuvé

### 4. Les 3 Modes de Fonctionnement

#### Mode PLAN (Review)
1. L'utilisateur demande l'analyse d'un dossier
2. L'agent scanne tous les fichiers
3. **Pour les cas ambigus ou incertains** : L'agent pose des questions à l'utilisateur avant de générer le plan (ex: "Plusieurs artistes possibles pour ce fichier, lequel préférez-vous?")
4. Génère un plan complet avec toutes les modifications proposées
5. L'utilisateur review le plan dans l'interface
6. Validation globale avant application batch

**Gestion des questions non résolues :**
- Si l'agent détecte plusieurs possibilités pour un tag (ex: plusieurs résultats Spotify)
- Si la confiance est faible (< 70%)
- Si des conventions utilisateur doivent être clarifiées (ex: format de genre préféré)
- L'agent interrompt l'analyse et pose une question
- Une fois répondu, l'agent continue avec ce contexte mémorisé pour les fichiers similaires

#### Mode MANUAL (Un par un)
1. L'agent traite les fichiers séquentiellement
2. Pour chaque fichier : analyse → suggestion → attente confirmation
3. L'utilisateur valide ou modifie chaque suggestion
4. Passage au fichier suivant après validation
5. Contrôle total fichier par fichier

#### Mode APPLY (Auto)
1. L'utilisateur lance le mode automatique
2. L'agent applique directement les suggestions
3. Affichage de la progression en temps réel
4. Log complet des modifications effectuées
5. Option de rollback en cas d'erreur

### 5. RAG (Retrieval Augmented Generation)

**Objectif** : Améliorer la qualité des suggestions en utilisant l'historique et les similarités

**Workflow** :
1. Lors de l'enrichissement Spotify, les données sont vectorisées
2. Stockage dans Qdrant (vector database)
3. Pour chaque suggestion, recherche de tracks similaires
4. L'agent utilise ces contextes pour des suggestions cohérentes
5. Apprentissage continu basé sur la collection de l'utilisateur

**Données vectorisées** :
- Métadonnées Spotify (genres, année, popularité)
- Audio features (BPM, clé, énergie, danceability, valence)
- Texte descriptif enrichi pour l'embedding

## Intégration Spotify avec @HttpExchange

Spring 6.1+ introduit les **HTTP Interfaces** qui permettent de définir des clients HTTP de manière déclarative, similaire à Spring Data repositories.

### Configuration du client Spotify

```java
@Configuration
public class SpotifyConfig {
    
    @Bean
    public SpotifyApiClient spotifyApiClient(
        @Value("${spotify.client-id}") String clientId,
        @Value("${spotify.client-secret}") String clientSecret
    ) {
        WebClient webClient = WebClient.builder()
            .baseUrl("https://api.spotify.com/v1")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .filter(spotifyAuthFilter(clientId, clientSecret))
            .build();
            
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(WebClientAdapter.create(webClient))
            .build();
            
        return factory.createClient(SpotifyApiClient.class);
    }
    
    private ExchangeFilterFunction spotifyAuthFilter(String clientId, String clientSecret) {
        return (request, next) -> {
            // Logique d'authentification OAuth2
            String token = getOrRefreshAccessToken(clientId, clientSecret);
            ClientRequest filtered = ClientRequest.from(request)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
            return next.exchange(filtered);
        };
    }
}
```

### Interface Spotify API avec @HttpExchange

```java
/**
 * Client déclaratif pour l'API Spotify utilisant Spring HTTP Interface
 */
public interface SpotifyApiClient {
    
    /**
     * Recherche de tracks
     */
    @GetExchange("/search")
    SpotifySearchResponse searchTracks(
        @RequestParam("q") String query,
        @RequestParam("type") String type,
        @RequestParam(value = "limit", defaultValue = "10") int limit
    );
    
    /**
     * Récupération des audio features d'un track
     */
    @GetExchange("/audio-features/{id}")
    SpotifyAudioFeatures getAudioFeatures(@PathVariable String id);
    
    /**
     * Récupération des détails d'un track
     */
    @GetExchange("/tracks/{id}")
    SpotifyTrack getTrack(@PathVariable String id);
    
    /**
     * Récupération de plusieurs tracks en batch
     */
    @GetExchange("/tracks")
    SpotifyTracksResponse getTracks(@RequestParam("ids") String trackIds);
    
    /**
     * Récupération des recommandations basées sur des seeds
     */
    @GetExchange("/recommendations")
    SpotifyRecommendationsResponse getRecommendations(
        @RequestParam(value = "seed_tracks", required = false) String seedTracks,
        @RequestParam(value = "seed_artists", required = false) String seedArtists,
        @RequestParam(value = "seed_genres", required = false) String seedGenres,
        @RequestParam(value = "limit", defaultValue = "20") int limit,
        @RequestParam(value = "target_energy", required = false) Double targetEnergy,
        @RequestParam(value = "target_danceability", required = false) Double targetDanceability
    );
    
    /**
     * Récupération de l'artiste
     */
    @GetExchange("/artists/{id}")
    SpotifyArtist getArtist(@PathVariable String id);
}
```

### Records pour les réponses Spotify

```java
// Réponse de recherche
public record SpotifySearchResponse(
    SpotifySearchTracks tracks
) {}

public record SpotifySearchTracks(
    List<SpotifyTrackItem> items,
    int total,
    int limit,
    int offset
) {}

public record SpotifyTrackItem(
    String id,
    String name,
    List<SpotifyArtistItem> artists,
    SpotifyAlbum album,
    int duration_ms,
    int popularity,
    boolean explicit,
    String preview_url
) {}

public record SpotifyArtistItem(
    String id,
    String name,
    String type
) {}

public record SpotifyAlbum(
    String id,
    String name,
    String release_date,
    List<SpotifyImage> images,
    String album_type
) {}

public record SpotifyImage(
    String url,
    int height,
    int width
) {}

// Audio features
public record SpotifyAudioFeatures(
    String id,
    double danceability,
    double energy,
    int key,
    double loudness,
    int mode,
    double speechiness,
    double acousticness,
    double instrumentalness,
    double liveness,
    double valence,
    double tempo,
    int duration_ms,
    int time_signature
) {
    public String getMusicalKey() {
        String[] keys = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return keys[key];
    }
    
    public String getMode() {
        return mode == 1 ? "Major" : "Minor";
    }
}

// Track complet
public record SpotifyTrack(
    String id,
    String name,
    List<SpotifyArtistItem> artists,
    SpotifyAlbum album,
    int duration_ms,
    boolean explicit,
    String href,
    int popularity,
    String preview_url,
    String type,
    String uri
) {}

// Recommandations
public record SpotifyRecommendationsResponse(
    List<SpotifyTrack> tracks,
    List<SpotifySeed> seeds
) {}

public record SpotifySeed(
    int initialPoolSize,
    int afterFilteringSize,
    int afterRelinkingSize,
    String id,
    String type,
    String href
) {}

// Artiste
public record SpotifyArtist(
    String id,
    String name,
    List<String> genres,
    int popularity,
    List<SpotifyImage> images,
    int followers
) {}
```

### Utilisation dans le service

```java
@Service
@Slf4j
public class SpotifyEnrichmentService {
    
    private final SpotifyApiClient spotifyClient;
    private final VectorStore vectorStore;
    private final SpotifyTrackRepository repository;
    
    public SpotifyEnrichmentService(
        SpotifyApiClient spotifyClient,
        VectorStore vectorStore,
        SpotifyTrackRepository repository
    ) {
        this.spotifyClient = spotifyClient;
        this.vectorStore = vectorStore;
        this.repository = repository;
    }
    
    @Tool(name = "enrichWithSpotify",
          description = "Recherche et enrichit les métadonnées d'un fichier audio via Spotify API")
    public SpotifyEnrichmentResult enrichTrack(
        @ToolParam(description = "Chemin du fichier") String filepath,
        @ToolParam(description = "Artiste approximatif") String artist,
        @ToolParam(description = "Titre approximatif") String title
    ) {
        try {
            // 1. Recherche avec @HttpExchange
            String query = String.format("artist:%s track:%s", artist, title);
            SpotifySearchResponse searchResult = spotifyClient.searchTracks(query, "track", 5);
            
            if (searchResult.tracks().items().isEmpty()) {
                return SpotifyEnrichmentResult.notFound();
            }
            
            SpotifyTrackItem bestMatch = searchResult.tracks().items().get(0);
            
            // 2. Récupération des audio features
            SpotifyAudioFeatures audioFeatures = spotifyClient.getAudioFeatures(bestMatch.id());
            
            // 3. Récupération du track complet pour plus de détails
            SpotifyTrack fullTrack = spotifyClient.getTrack(bestMatch.id());
            
            // 4. Conversion vers notre modèle interne
            SpotifyTrackData enrichedData = convertToInternalModel(
                filepath, 
                fullTrack, 
                audioFeatures
            );
            
            // 5. Vectorisation et stockage
            vectorizeAndStore(enrichedData);
            
            log.info("Track enriched: {} - {}", artist, title);
            
            return SpotifyEnrichmentResult.success(enrichedData);
            
        } catch (Exception e) {
            log.error("Spotify enrichment failed for: {} - {}", artist, title, e);
            return SpotifyEnrichmentResult.error(e.getMessage());
        }
    }
    
    private SpotifyTrackData convertToInternalModel(
        String filepath,
        SpotifyTrack track,
        SpotifyAudioFeatures features
    ) {
        return new SpotifyTrackData(
            track.id(),
            filepath,
            track.artists().get(0).name(),
            track.name(),
            track.album().name(),
            List.of(), // Genres récupérés de l'artiste si besoin
            extractYear(track.album().release_date()),
            (double) track.popularity(),
            track.duration_ms(),
            new AudioFeatures(
                features.danceability(),
                features.energy(),
                features.valence(),
                features.acousticness(),
                features.instrumentalness(),
                features.speechiness(),
                (int) features.tempo(),
                features.getMusicalKey(),
                features.getMode(),
                features.time_signature()
            ),
            null, // embedding sera ajouté après
            LocalDateTime.now()
        );
    }
}
```

## Spring AI 2.0 Structured Outputs

Spring AI 2.0 introduit le support natif des **Structured Outputs** qui garantit que l'IA retourne toujours un JSON conforme à votre schéma.

### Configuration

```java
@Configuration
public class AIConfig {
    
    @Bean
    public ChatClient chatClient(
        ChatClient.Builder builder,
        DJMusicTools tools
    ) {
        return builder
            .defaultOptions(
                ChatOptionsBuilder.builder()
                    .withModel("mistral")
                    .withTemperature(0.7)
                    .build()
            )
            .defaultTools(tools)
            .build();
    }
}
```

### Utilisation des Structured Outputs

```java
@Service
public class DJAgentService {
    
    private final ChatClient chatClient;
    
    /**
     * Exemple 1: Réponse structurée simple
     */
    public TagSuggestions getSuggestionsStructured(String filepath, String userQuery) {
        return chatClient.prompt()
            .user(userQuery)
            .call()
            .entity(TagSuggestions.class); // Structured output automatique
    }
    
    /**
     * Exemple 2: Plan complet structuré
     */
    public TaggingPlan createStructuredPlan(String folderPath) {
        String prompt = String.format("""
            Analyse tous les fichiers audio dans le dossier: %s
            
            Pour chaque fichier:
            1. Détecte les tags manquants
            2. Suggère des valeurs appropriées
            3. Indique le niveau de confiance
            
            Retourne un plan de modifications complet.
            """, folderPath);
            
        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(TaggingPlan.class); // Le modèle retourne directement un TaggingPlan
    }
    
    /**
     * Exemple 3: Batch suggestions avec structured output
     */
    public record BatchSuggestionsRequest(
        List<String> filepaths,
        boolean includeSpotifyEnrichment,
        double confidenceThreshold
    ) {}
    
    public record BatchSuggestionsResponse(
        List<TagSuggestions> suggestions,
        int totalProcessed,
        int successCount,
        List<String> errors
    ) {}
    
    public BatchSuggestionsResponse getBatchSuggestions(BatchSuggestionsRequest request) {
        String prompt = String.format("""
            Analyse ces %d fichiers audio et suggère des tags pour chacun.
            
            Fichiers: %s
            
            Paramètres:
            - Enrichissement Spotify: %s
            - Seuil de confiance minimum: %.2f
            
            Retourne une liste complète de suggestions avec statistiques.
            """, 
            request.filepaths().size(),
            String.join(", ", request.filepaths()),
            request.includeSpotifyEnrichment(),
            request.confidenceThreshold()
        );
        
        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(BatchSuggestionsResponse.class);
    }
    
    /**
     * Exemple 4: Analyse de qualité avec structured output
     */
    public record MusicCollectionAnalysis(
        int totalFiles,
        int filesWithCompleteTags,
        int filesWithMissingTags,
        Map<String, Integer> missingTagsBreakdown,
        List<String> recommendedActions,
        double overallCompleteness
    ) {}
    
    public MusicCollectionAnalysis analyzeCollection(String folderPath) {
        return chatClient.prompt()
            .user("Analyse la qualité des tags de ma collection dans: " + folderPath)
            .call()
            .entity(MusicCollectionAnalysis.class);
    }
    
    /**
     * Exemple 5: Avec tools ET structured output combinés
     */
    public record SmartEnrichmentResult(
        String filepath,
        boolean spotifyFound,
        TagSuggestions suggestions,
        List<SpotifyTrackData> similarTracks,
        String reasoning,
        double confidence
    ) {}
    
    public SmartEnrichmentResult smartEnrich(String filepath) {
        String prompt = String.format("""
            Pour le fichier: %s
            
            1. Utilise smartSuggestTags pour obtenir des suggestions
            2. Si possible, enrichis avec Spotify
            3. Trouve des tracks similaires dans la base
            4. Explique ton raisonnement
            5. Donne un score de confiance global
            
            Retourne un résultat structuré complet.
            """, filepath);
            
        // L'agent va appeler les tools nécessaires ET retourner un résultat structuré
        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(SmartEnrichmentResult.class);
    }
}
```

### Avantages des Structured Outputs

✅ **Type-safety** : Pas de parsing JSON manuel, conversion automatique  
✅ **Validation** : Le modèle garantit la conformité au schéma  
✅ **Immutabilité** : Parfait avec les records Java  
✅ **Lisibilité** : Code plus clair et maintenable  
✅ **Fiabilité** : Moins d'erreurs de parsing ou de format

### Schema validation avec Structured Outputs

Spring AI 2.0 génère automatiquement le JSON Schema à partir de vos records :

```java
// Ce record
public record TagSuggestions(
    @JsonProperty(required = true)
    String filepath,
    
    @JsonPropertyDescription("Suggested artist name extracted from filename or metadata")
    String suggestedArtist,
    
    @JsonPropertyDescription("Suggested track title")
    String suggestedTitle,
    
    @Min(0) @Max(300)
    String suggestedBpm,
    
    @Pattern(regexp = "^[A-G](#|b)? (Major|Minor)$")
    String suggestedKey,
    
    @Min(0) @Max(1)
    double confidence
) {}

// Devient automatiquement ce JSON Schema envoyé au modèle
{
  "type": "object",
  "properties": {
    "filepath": { "type": "string" },
    "suggestedArtist": { 
      "type": "string",
      "description": "Suggested artist name extracted from filename or metadata"
    },
    "suggestedTitle": { 
      "type": "string",
      "description": "Suggested track title"
    },
    "suggestedBpm": { 
      "type": "string",
      "minimum": 0,
      "maximum": 300
    },
    "suggestedKey": {
      "type": "string",
      "pattern": "^[A-G](#|b)? (Major|Minor)$"
    },
    "confidence": {
      "type": "number",
      "minimum": 0,
      "maximum": 1
    }
  },
  "required": ["filepath"]
}
```

## Structure des Données (Records Java 25)

### Records Principaux

```java
// Information d'un fichier audio local
public record MusicFileInfo(
    String filepath,
    String filename,
    String artist,
    String title,
    String album,
    String genre,
    String bpm,
    String key,
    long fileSize,
    LocalDateTime lastModified
) {
    public boolean hasArtistAndTitle() { ... }
    public boolean isMissingTag(String tag) { ... }
}

// Audio features Spotify
public record AudioFeatures(
    double danceability,    // 0.0 - 1.0
    double energy,          // 0.0 - 1.0
    double valence,         // 0.0 - 1.0 (positivity)
    double acousticness,
    double instrumentalness,
    double speechiness,
    int tempo,              // BPM
    String key,             // C, D, E, F, G, A, B
    String mode,            // Major/Minor
    int timeSignature
)

// Données enrichies Spotify
public record SpotifyTrackData(
    String spotifyId,
    String localFilePath,
    String artist,
    String title,
    String album,
    List<String> genres,
    Integer releaseYear,
    Double popularity,
    Integer durationMs,
    AudioFeatures audioFeatures,
    float[] embedding,              // Vecteur pour RAG
    LocalDateTime fetchedAt
) {
    public String toEmbeddingText() { ... }
    public Map<String, Object> toMetadata() { ... }
}

// Tags manquants détectés
public record MissingTagsReport(
    String filepath,
    List<String> missingTags
) {
    public boolean hasMissingTags() { ... }
    public int missingCount() { ... }
}

// Suggestions de l'agent
public record TagSuggestions(
    String filepath,
    String suggestedArtist,
    String suggestedTitle,
    String suggestedGenre,
    String suggestedBpm,
    String suggestedKey,
    SpotifyTrackData spotifyMatch,
    List<SpotifyTrackData> similarTracks,
    double confidence
)

// Opération de modification
public record TagOperation(
    String filepath,
    Map<String, String> currentTags,
    Map<String, String> suggestedTags,
    OperationStatus status,          // PENDING, APPROVED, REJECTED, APPLIED, ERROR
    String message
) {
    public TagOperation withStatus(OperationStatus newStatus) { ... }
}

// Plan complet de modifications
public record TaggingPlan(
    String folderPath,
    List<TagOperation> operations,
    LocalDateTime createdAt,
    PlanStatus status,               // DRAFT, READY_FOR_REVIEW, APPROVED, APPLYING, COMPLETED
    int totalFiles,
    int filesWithMissingTags
) {
    public TaggingPlan withStatus(PlanStatus newStatus) { ... }
    public int pendingCount() { ... }
}

// Résultat d'enrichissement Spotify
public record SpotifyEnrichmentResult(
    ResultStatus status,              // SUCCESS, NOT_FOUND, ERROR
    SpotifyTrackData data,
    String errorMessage
) {
    public static SpotifyEnrichmentResult success(SpotifyTrackData data) { ... }
    public static SpotifyEnrichmentResult notFound() { ... }
    public static SpotifyEnrichmentResult error(String message) { ... }
}

// Résultat d'application batch
public record BatchApplyResult(
    int totalProcessed,
    int successCount,
    int errorCount,
    List<TagOperation> failedOperations,
    Duration executionTime
) {
    public double successRate() { ... }
}

// Configuration du mode
public record AgentMode(
    ModeType type,                    // PLAN, MANUAL, APPLY
    boolean autoConfirm,
    int batchSize
) {
    public static AgentMode plan() { ... }
    public static AgentMode manual() { ... }
    public static AgentMode apply(int batchSize) { ... }
}

// Question de l'agent pour clarification (mode PLAN)
public record AgentQuestion(
    String questionId,
    String filepath,                  // Fichier concerné (peut être null pour question globale)
    QuestionType type,                // MULTIPLE_CHOICE, PREFERENCE, CONFIRMATION
    String question,                  // La question posée à l'utilisateur
    List<String> options,            // Options possibles (pour MULTIPLE_CHOICE)
    String context,                  // Contexte additionnel
    double currentConfidence         // Niveau de confiance actuel de l'agent
) {
    public enum QuestionType {
        MULTIPLE_CHOICE,  // Choisir parmi plusieurs options
        PREFERENCE,       // Préférence utilisateur à mémoriser
        CONFIRMATION      // Simple oui/non
    }
}

// Réponse de l'utilisateur à une question
public record AgentQuestionResponse(
    String questionId,
    String selectedOption,           // Réponse choisie
    boolean applyToSimilar          // Appliquer cette réponse aux cas similaires
)
```

## Dépendances Gradle

### gradle-wrapper.properties
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.2.1-bin.zip
```

### build.gradle (Backend)

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.2'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.djtools'
version = '1.0.0'
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }
}

ext {
    springAiVersion = '2.0.0-M2'
}

dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    // Spring AI 2.0
    implementation 'org.springframework.ai:spring-ai-ollama-spring-boot-starter'
    implementation 'org.springframework.ai:spring-ai-qdrant-store-spring-boot-starter'
    
    // Database
    implementation 'org.postgresql:postgresql'
    implementation 'com.pgvector:pgvector:0.1.5'
    
    // Audio manipulation
    implementation 'net.jthink:jaudiotagger:3.0.1'
    
    // Spotify API (utilisera @HttpExchange, pas de lib externe nécessaire)
    // Spring WebClient suffit avec HTTP Interface
    
    // Utilities
    implementation 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // Jackson pour annotations JSON dans les records
    implementation 'com.fasterxml.jackson.core:jackson-annotations'
    
    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'io.projectreactor:reactor-test'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:${springAiVersion}"
    }
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## Configuration Docker

### docker-compose.yml

```yaml
version: '3.8'

services:
  # Ollama avec Mistral
  ollama:
    image: ollama/ollama:latest
    container_name: dj-tagger-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    environment:
      - OLLAMA_MODELS=/root/.ollama/models
    healthcheck:
      test: ["CMD", "ollama", "list"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - dj-tagger-network
      
  # Vector Database (Qdrant)
  qdrant:
    image: qdrant/qdrant:latest
    container_name: dj-tagger-qdrant
    ports:
      - "6333:6333"  # HTTP API
      - "6334:6334"  # gRPC
    volumes:
      - qdrant_data:/qdrant/storage
    environment:
      - QDRANT__SERVICE__GRPC_PORT=6334
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:6333/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - dj-tagger-network
      
  # PostgreSQL avec pgvector
  postgres:
    image: ankane/pgvector:latest
    container_name: dj-tagger-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: musicdb
      POSTGRES_USER: djuser
      POSTGRES_PASSWORD: djpass
      POSTGRES_INITDB_ARGS: "-E UTF8 --locale=en_US.UTF-8"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U djuser -d musicdb"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - dj-tagger-network

volumes:
  ollama_data:
    driver: local
  qdrant_data:
    driver: local
  postgres_data:
    driver: local

networks:
  dj-tagger-network:
    driver: bridge
```

### init-db.sql

```sql
-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Table pour les tracks Spotify enrichis
CREATE TABLE IF NOT EXISTS spotify_tracks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    spotify_id VARCHAR(255) UNIQUE NOT NULL,
    local_file_path TEXT NOT NULL,
    artist VARCHAR(500),
    title VARCHAR(500),
    album VARCHAR(500),
    genres TEXT[],
    release_year INTEGER,
    popularity DOUBLE PRECISION,
    duration_ms INTEGER,
    -- Audio features
    danceability DOUBLE PRECISION,
    energy DOUBLE PRECISION,
    valence DOUBLE PRECISION,
    acousticness DOUBLE PRECISION,
    instrumentalness DOUBLE PRECISION,
    speechiness DOUBLE PRECISION,
    tempo INTEGER,
    key VARCHAR(10),
    mode VARCHAR(20),
    time_signature INTEGER,
    -- Embedding (optionnel si on utilise seulement Qdrant)
    embedding vector(1536),
    fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index pour recherche rapide
CREATE INDEX idx_spotify_tracks_spotify_id ON spotify_tracks(spotify_id);
CREATE INDEX idx_spotify_tracks_local_path ON spotify_tracks(local_file_path);
CREATE INDEX idx_spotify_tracks_artist ON spotify_tracks(artist);

-- Table pour l'historique des modifications
CREATE TABLE IF NOT EXISTS tagging_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_path TEXT NOT NULL,
    operation_type VARCHAR(50) NOT NULL, -- SCAN, ENRICH, APPLY, ROLLBACK
    old_tags JSONB,
    new_tags JSONB,
    applied_by VARCHAR(100),
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) -- SUCCESS, ERROR
);

CREATE INDEX idx_tagging_history_file_path ON tagging_history(file_path);
CREATE INDEX idx_tagging_history_applied_at ON tagging_history(applied_at DESC);
```

## Configuration Application

### application.yml

```yaml
spring:
  application:
    name: dj-music-tagger
    
  # Database
  datasource:
    url: jdbc:postgresql://localhost:5432/musicdb
    username: djuser
    password: djpass
    driver-class-name: org.postgresql.Driver
    
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        
  # Spring AI Configuration
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: mistral
          temperature: 0.7
          top-p: 0.9
      embedding:
        options:
          model: nomic-embed-text
          
    vectorstore:
      qdrant:
        host: localhost
        port: 6333
        collection-name: spotify_tracks
        initialize-schema: true

# Spotify API Configuration
spotify:
  client-id: ${SPOTIFY_CLIENT_ID}
  client-secret: ${SPOTIFY_CLIENT_SECRET}
  redirect-uri: http://localhost:8080/spotify/callback
  
# Application Configuration
dj-tagger:
  audio:
    supported-formats:
      - mp3
      - flac
      - wav
      - aiff
      - m4a
    max-file-size-mb: 500
    
  agent:
    default-mode: PLAN
    batch-size: 50
    confidence-threshold: 0.7
    
  rag:
    similarity-threshold: 0.75
    max-similar-tracks: 5
    embedding-dimension: 1536

server:
  port: 8080
  
logging:
  level:
    com.djtools: DEBUG
    org.springframework.ai: INFO
    org.hibernate.SQL: DEBUG
```

## Structure du Projet Backend (Architecture Hexagonale + DDD)

```
music-tagger-backend/
├── build.gradle
├── settings.gradle
├── docker-compose.yml
├── init-db.sql
├── README.md
├── CLAUDE.md
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/djtools/musictagger/
    │   │       │
    │   │       ├── domain/                    # DOMAIN (cœur métier)
    │   │       │   │
    │   │       │   ├── model/                 # Entités & Value Objects
    │   │       │   │   ├── MusicFile.java
    │   │       │   │   ├── Tag.java
    │   │       │   │   ├── TaggingPlan.java
    │   │       │   │   ├── HarmonicPlaylist.java
    │   │       │   │   └── vo/               # Value Objects
    │   │       │   │       ├── Filepath.java
    │   │       │   │       ├── BPM.java
    │   │       │   │       ├── MusicalKey.java
    │   │       │   │       └── CamelotKey.java
    │   │       │   │
    │   │       │   ├── service/              # Services métier
    │   │       │   │   ├── TaggingService.java
    │   │       │   │   ├── PlaylistGenerator.java
    │   │       │   │   └── HarmonicMixingService.java
    │   │       │   │
    │   │       │   ├── usecase/              # Use Cases (orchestration)
    │   │       │   │   ├── ScanMusicUseCase.java
    │   │       │   │   ├── EnrichTagsUseCase.java
    │   │       │   │   ├── CreatePlanUseCase.java
    │   │       │   │   ├── ApplyTagsUseCase.java
    │   │       │   │   ├── GeneratePlaylistUseCase.java
    │   │       │   │   └── AyanAgentUseCase.java
    │   │       │   │
    │   │       │   └── port/                 # Ports (interfaces)
    │   │       │       ├── in/               # Ports entrants (abstractions consommées par le domain)
    │   │       │       │   └── MusicMetadataProvider.java  # Enrichissement générique (Spotify = 1 impl)
    │   │       │       └── out/              # Ports sortants uniquement
    │   │       │           ├── AudioRepository.java
    │   │       │           ├── VectorStorePort.java
    │   │       │           └── AIAgentPort.java
    │   │       │
    │   │       └── infrastructure/           # INFRASTRUCTURE (adaptateurs)
    │   │           │
    │   │           ├── adapter/
    │   │           │   ├── in/               # Adaptateurs entrants
    │   │           │   │   ├── rest/
    │   │           │   │   │   ├── MusicController.java
    │   │           │   │   │   ├── PlanController.java
    │   │           │   │   │   └── AgentController.java
    │   │           │   │   │
    │   │           │   │   ├── mcp/          # MCP Tools (adaptateur IA)
    │   │           │   │   │   └── AyanMusicTools.java
    │   │           │   │   │
    │   │           │   │   └── spotify/          # Implémente MusicMetadataProvider
    │   │           │   │       ├── SpotifyMusicMetadataAdapter.java
    │   │           │   │       ├── SpotifyApiClient.java
    │   │           │   │       └── SpotifyMapper.java
    │   │           │   │
    │   │           │   └── out/              # Adaptateurs sortants
    │   │           │       ├── persistence/
    │   │           │       │   ├── JpaAudioRepository.java
    │   │           │       │   ├── entity/
    │   │           │       │   │   └── SpotifyTrackEntity.java
    │   │           │       │   └── mapper/
    │   │           │       │       └── MusicFileMapper.java
    │   │           │       │
    │   │           │       ├── audio/
    │   │           │       │   ├── JAudioTaggerAdapter.java
    │   │           │       │   └── FileSystemScanner.java
    │   │           │       │
    │   │           │       ├── vectorstore/
    │   │           │       │   └── QdrantAdapter.java
    │   │           │       │
    │   │           │       └── ai/
    │   │           │           └── SpringAIAdapter.java
    │   │           │
    │   │           └── config/               # Configuration
    │   │               ├── SpringAIConfig.java
    │   │               ├── QdrantConfig.java
    │   │               ├── SpotifyConfig.java
    │   │               └── SecurityConfig.java
    │   │
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/
    │
    └── test/
        └── java/
            └── com/djtools/musictagger/
                ├── domain/
                └── architecture/         # Tests arch (ArchUnit)
                    └── HexagonalArchTest.java
```

## Principes Architecture Hexagonale Simplifiée

### Domain (Cœur)
- **Indépendant** de toute technologie
- **Pure logique métier**
- **Use Cases** = orchestration dans domain
- **Ports out** = dépendances externes

### Infrastructure
- **Adaptateurs in** = REST, MCP Tools
- **Adaptateurs out** = implémentent ports
- **Technologies** confinées ici
- **Remplaçables** sans toucher domain

## Flux de Données

```
REST Controller → Use Case → Domain Service → Port Out → Adapter Out
     ↓              ↓            ↓               ↓           ↓
  (infra.in)   (domain.usecase) (domain)   (domain.port)  (infra.out)
```

## Dépendances (Règle d'Or)

```
Infrastructure → Domain (use cases + services + model)
                    ↑
              (dépend de rien)
```

## Structure du Projet Frontend

```
music-tagger-frontend/
├── package.json
├── angular.json
├── tsconfig.json
├── electron.config.js
│
├── src/
│   ├── main.ts
│   ├── index.html
│   ├── styles.scss
│   │
│   ├── app/
│   │   ├── app.component.ts
│   │   ├── app.component.html
│   │   ├── app.component.scss
│   │   ├── app.config.ts
│   │   ├── app.routes.ts
│   │   │
│   │   ├── core/
│   │   │   ├── services/
│   │   │   │   ├── agent.service.ts
│   │   │   │   ├── music-file.service.ts
│   │   │   │   ├── plan.service.ts
│   │   │   │   ├── spotify.service.ts
│   │   │   │   └── websocket.service.ts
│   │   │   ├── models/
│   │   │   │   ├── music-file.model.ts
│   │   │   │   ├── tag-operation.model.ts
│   │   │   │   ├── tagging-plan.model.ts
│   │   │   │   └── agent-mode.model.ts
│   │   │   └── guards/
│   │   │
│   │   ├── features/
│   │   │   ├── chat/
│   │   │   │   ├── chat.component.ts
│   │   │   │   ├── chat.component.html
│   │   │   │   ├── chat.component.scss
│   │   │   │   └── message-bubble/
│   │   │   │
│   │   │   ├── file-list/
│   │   │   │   ├── file-list.component.ts
│   │   │   │   ├── file-list.component.html
│   │   │   │   ├── file-list.component.scss
│   │   │   │   └── file-item/
│   │   │   │
│   │   │   ├── plan-review/
│   │   │   │   ├── plan-review.component.ts
│   │   │   │   ├── plan-review.component.html
│   │   │   │   ├── plan-review.component.scss
│   │   │   │   └── operation-card/
│   │   │   │
│   │   │   ├── mode-selector/
│   │   │   │   ├── mode-selector.component.ts
│   │   │   │   ├── mode-selector.component.html
│   │   │   │   └── mode-selector.component.scss
│   │   │   │
│   │   │   └── settings/
│   │   │       ├── settings.component.ts
│   │   │       ├── settings.component.html
│   │   │       └── settings.component.scss
│   │   │
│   │   ├── shared/
│   │   │   ├── components/
│   │   │   │   ├── tag-chip/
│   │   │   │   ├── progress-bar/
│   │   │   │   └── confirmation-dialog/
│   │   │   └── pipes/
│   │   │       ├── file-size.pipe.ts
│   │   │       └── duration.pipe.ts
│   │   │
│   │   └── layout/
│   │       ├── main-layout/
│   │       ├── sidebar/
│   │       └── toolbar/
│   │
│   └── electron/
│       ├── main.ts (Electron main process)
│       ├── preload.ts
│       └── menu.ts
│
└── dist/ (build output)
```

## API REST Endpoints

### Agent Endpoints
- `POST /api/agent/chat` - Envoyer un message à l'agent
- `GET /api/agent/conversation/{id}` - Récupérer une conversation
- `POST /api/agent/mode` - Changer le mode de l'agent

### Music File Endpoints
- `POST /api/music/scan` - Scanner un dossier
- `GET /api/music/file/{path}` - Informations sur un fichier
- `GET /api/music/missing-tags/{path}` - Tags manquants d'un fichier

### Plan Endpoints
- `POST /api/plan/create` - Créer un plan pour un dossier
- `GET /api/plan/{id}` - Récupérer un plan
- `PUT /api/plan/{id}/approve` - Approuver un plan
- `POST /api/plan/{id}/execute` - Exécuter un plan
- `GET /api/plan/history` - Historique des plans

### Tag Endpoints
- `POST /api/tags/preview` - Prévisualiser des modifications
- `POST /api/tags/apply` - Appliquer des tags à un fichier
- `POST /api/tags/batch` - Application batch

### Spotify Endpoints
- `GET /api/spotify/auth` - Authentification OAuth
- `POST /api/spotify/enrich` - Enrichir un fichier avec Spotify
- `GET /api/spotify/search` - Rechercher un track

### WebSocket
- `ws://localhost:8080/ws/agent` - Communication temps réel avec l'agent

## Commandes de Démarrage

### Backend

```bash
# Démarrer les services Docker
docker-compose up -d

# Vérifier que tous les services sont up
docker-compose ps

# Télécharger le modèle Mistral dans Ollama
docker exec -it dj-tagger-ollama ollama pull mistral

# Télécharger le modèle d'embedding
docker exec -it dj-tagger-ollama ollama pull nomic-embed-text

# Build l'application
./gradlew build

# Lancer l'application Spring Boot
./gradlew bootRun

# Ou avec profil dev
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Frontend

```bash
# Installer les dépendances
npm install

# Mode développement (Angular)
npm run start

# Build Electron
npm run electron:build

# Lancer en mode Electron
npm run electron:serve
```

## Variables d'Environnement

Créer un fichier `.env` à la racine du backend :

```env
# Spotify API
SPOTIFY_CLIENT_ID=your_spotify_client_id
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=musicdb
DB_USER=djuser
DB_PASSWORD=djpass

# Ollama
OLLAMA_URL=http://localhost:11434

# Qdrant
QDRANT_HOST=localhost
QDRANT_PORT=6333

# Application
SPRING_PROFILES_ACTIVE=dev
LOG_LEVEL=DEBUG
```

## Plan d'Implémentation par Phases

### 🎯 Phase 1 : Foundation Backend (Semaine 1)

**Objectif** : Backend minimal fonctionnel avec scan de fichiers

**Livrables** :
1. Setup projet Gradle + Java 25 + Spring Boot 4.0.2
2. Docker compose (PostgreSQL uniquement pour commencer)
3. Modèles (Records de base : MusicFileInfo, MissingTagsReport, TagOperation)
4. AudioTagService avec JAudiotagger (lecture tags uniquement)
5. AudioScannerService (scan récursif dossiers)
6. REST Controller basique : POST /api/music/scan

**Tests** :
- Scan d'un dossier retourne liste fichiers avec tags existants
- Détection tags manquants fonctionne

**Point de validation** : Scanner un dossier et voir les tags dans la console/API

---

### 🔌 Phase 2 : Intégration Spotify (Semaine 2)

**Objectif** : Enrichissement métadonnées via Spotify

**Livrables** :
1. Configuration @HttpExchange pour Spotify
2. SpotifyApiClient interface (search, getTrack, getAudioFeatures)
3. Records réponses Spotify complets
4. SpotifyAuthService (OAuth2 token management)
5. SpotifyEnrichmentService
6. SpotifyTrackEntity + Repository (PostgreSQL)
7. REST endpoints : POST /api/spotify/enrich, GET /api/spotify/search

**Tests** :
- Authentification Spotify fonctionne
- Recherche track retourne résultats
- Enrichissement stocke en DB

**Point de validation** : Enrichir un fichier avec Spotify et voir données en DB

---

### 🤖 Phase 3 : Spring AI + Agent Basique (Semaine 3)

**Objectif** : Agent conversationnel avec premières fonctions MCP

**Livrables** :
1. Docker Ollama + pull modèle Mistral
2. Configuration Spring AI 2.0
3. ChatClient setup
4. Premières @Tool functions :
   - scanMusicFile
   - detectMissingTags
   - suggestTagsFromFilename
   - enrichWithSpotify
5. DJAgentService avec structured outputs
6. WebSocket pour chat temps réel
7. REST : POST /api/agent/chat

**Tests** :
- Conversation avec agent fonctionne
- Agent appelle tools correctement
- Structured outputs retournent bon format

**Point de validation** : "Scanne mon dossier /music/test" → agent retourne analyse structurée

---

### 📊 Phase 4 : Mode PLAN (Semaine 4)

**Objectif** : Génération et validation de plans de modifications

**Livrables** :
1. Records : TaggingPlan, AgentQuestion, AgentQuestionResponse
2. @Tool : createPlanForFolder
3. PlanManagementService
4. Logique questions pour cas ambigus
5. Repository pour historique plans
6. REST endpoints : POST /api/plan/create, GET /api/plan/{id}, PUT /api/plan/{id}/approve

**Tests** :
- Génération plan complet pour dossier
- Questions posées pour cas < 70% confiance
- Plan sauvegardé en DB

**Point de validation** : Générer plan pour 50 fichiers avec questions sur ambiguïtés

---

### ✍️ Phase 5 : Application des Tags (Semaine 5)

**Objectif** : Écriture effective des tags dans fichiers

**Livrables** :
1. TagApplicationService (écriture JAudiotagger)
2. @Tool : applyTags, batchApplyTags, previewTagUpdate
3. TaggingHistoryEntity + Repository (traçabilité)
4. Validation pre-apply (fichier writable, backup optionnel)
5. REST : POST /api/tags/apply, POST /api/tags/batch
6. Gestion erreurs + rollback

**Tests** :
- Apply tags modifie réellement fichier
- Historique enregistré
- Batch apply sur 100 fichiers

**Point de validation** : Exécuter plan → tags écrits dans fichiers + vérifiables avec lecteur audio

---

### 🔍 Phase 6 : RAG + Vector Store (Semaine 6)

**Objectif** : Recherche sémantique et suggestions intelligentes

**Livrables** :
1. Docker Qdrant
2. Configuration Spring AI Vector Store
3. EmbeddingService (vectorisation données Spotify)
4. VectorStoreService
5. @Tool : findSimilarTracks, smartSuggestTags
6. Migration données existantes vers Qdrant
7. REST : GET /api/rag/similar

**Tests** :
- Vectorisation données Spotify
- Recherche similarité retourne résultats pertinents
- Suggestions améliorées avec RAG

**Point de validation** : "Trouve des tracks similaires à ma techno" → résultats pertinents

---

### 🎨 Phase 7 : Frontend Angular - Structure (Semaine 7)

**Objectif** : Application Electron + Angular avec UI de base

**Livrables** :
1. Setup Electron + Angular 21 + Material
2. Structure projet (modules, routing)
3. Services frontend (AgentService, MusicFileService)
4. Layout principal (toolbar, sidebar)
5. Composant chat basique
6. Composant file-list
7. Communication WebSocket agent

**Tests** :
- App Electron démarre
- Chat envoie messages au backend
- Liste fichiers affiche résultats scan

**Point de validation** : App desktop affiche chat + liste fichiers

---

### 🎛️ Phase 8 : Frontend - Modes & Plan Review (Semaine 8)

**Objectif** : UI complète pour les 3 modes

**Livrables** :
1. Composant mode-selector
2. Composant plan-review (tableau modifications)
3. Composant operation-card (détail modification)
4. Dialog confirmation
5. Dialog questions agent (AgentQuestion)
6. Progress bar pour batch operations
7. Settings panel

**Tests** :
- Switch entre modes fonctionne
- Plan review affiche toutes modifications
- Questions agent interactives
- Apply batch avec progress

**Point de validation** : Mode PLAN complet → review → apply avec feedback temps réel

---

### 🔐 Phase 9 : Mode MANUAL & APPLY (Semaine 9)

**Objectif** : Compléter les 2 autres modes

**Livrables** :
1. Logique mode MANUAL (fichier par fichier)
2. Logique mode APPLY (auto sans confirmation)
3. @Tool : processNextFile (pour MANUAL)
4. Streaming responses pour APPLY
5. UI stepper pour MANUAL
6. UI live logs pour APPLY

**Tests** :
- Mode MANUAL demande confirmation chaque fichier
- Mode APPLY traite tout automatiquement
- Logs temps réel visibles

**Point de validation** : 3 modes fonctionnent end-to-end

---

### 🎨 Phase 10 : Polish & UX (Semaine 10)

**Objectif** : Amélioration expérience utilisateur

**Livrables** :
1. Thème dark/light
2. Keyboard shortcuts
3. Drag & drop dossiers
4. Preview audio (lecteur intégré)
5. Statistiques collection (dashboard)
6. Export/import settings
7. Notifications système
8. Animations transitions

**Tests** :
- UX fluide
- Shortcuts fonctionnent
- Drag & drop intuitif

**Point de validation** : App agréable à utiliser quotidiennement

---

### 🐛 Phase 11 : Tests & Qualité (Semaine 11)

**Objectif** : Robustesse et fiabilité

**Livrables** :
1. Tests unitaires backend (80% coverage)
2. Tests intégration Spring AI
3. Tests E2E Playwright/Cypress
4. Gestion erreurs complète
5. Logging structuré
6. Monitoring performances
7. Documentation API (OpenAPI)

**Tests** :
- Test suite complète passe
- Pas de memory leaks
- Gestion cas limites

**Point de validation** : Build CI/CD passe avec tous les tests

---

### 📦 Phase 12 : Packaging & Distribution (Semaine 12)

**Objectif** : Application distribuable

**Livrables** :
1. Electron builder configuration
2. Build pour Windows/Mac/Linux
3. Auto-update setup
4. Installer packages
5. Documentation utilisateur
6. README complet
7. CHANGELOG
8. Scripts migration DB

**Tests** :
- Installeurs fonctionnent
- Auto-update marche
- Migration DB smooth

**Point de validation** : App installable et utilisable par beta-testeurs

---

### 📊 Phase 13 : Dashboard & Analyse Collection (Semaine 13)

**Objectif** : Tableau de bord riche pour analyser sa collection musicale et suivre l'activité d'enrichissement.

**Livrables** :

#### 13a — Backend : Nouveaux endpoints stats

Nouveau record domaine : `CollectionProfile`
- Distribution des genres (`Map<String, Long>` — genre → count)
- Histogramme BPM (`Map<String, Long>` — tranches type "120-125" → count)
- Distribution des tonalités (`Map<String, Long>` — clé musicale → count, notation Camelot)
- Moyenne des audio features (energy, danceability, valence — depuis `EnrichedTrackMetadata.audioFeatures`)
- Total tracks scannés, total enrichis, total avec tags complets

Nouveau record domaine : `EnrichmentStats`
- Taux de match Spotify (enrichis / total scannés en pourcentage)
- Types de tags les plus enrichis (`Map<String, Long>` — nom tag → count, trié desc)
- Taux d'erreur (erreurs / total opérations)
- Enrichissement par source ("spotify", "rag", "manual" → count)

Nouveau record domaine : `ActivityTimeline`
- Plans par période (`Map<String, Long>` — date string → count)
- Tags appliqués par période (`Map<String, Long>`)
- Répartition par mode (`Map<OperatingMode, Long>` — PLAN/MANUAL/APPLY → count)
- Durée moyenne d'exécution par mode

Nouveaux endpoints sur `StatsController` :
- `GET /api/stats/collection` → `CollectionProfile`
- `GET /api/stats/enrichment` → `EnrichmentStats`
- `GET /api/stats/activity?period=week|month|all` → `ActivityTimeline`
- Conserver `GET /api/stats` existant pour les KPIs de base

`StatsService` enrichi pour calculer les trois depuis l'historique Redis + données des plans.

#### 13b — Frontend : Page Dashboard (React + Angular)

Remplacer la page Stats basique par un dashboard à onglets :

**Onglet 1 — Ma Collection**
- Donut/pie chart : distribution genres (top 10 + "Autres")
- Bar chart : histogramme BPM (tranches de 5 BPM)
- Visualisation Camelot Wheel : distribution tonalités mappée sur les positions de la roue
- Radar chart : moyenne audio features (energy, danceability, valence, acousticness, instrumentalness)
- KPI cards : total tracks, tracks enrichis, % tags complets

**Onglet 2 — Enrichissement**
- KPI cards : taux match Spotify %, taux erreur %, total enrichis
- Horizontal bar chart : types de tags les plus enrichis
- Pie chart : enrichissement par source (Spotify / RAG / Manuel)

**Onglet 3 — Activité**
- Line/area chart : tags appliqués dans le temps (toggle jour/semaine/mois)
- Stacked bar chart : utilisation des modes (PLAN/MANUAL/APPLY) dans le temps
- KPI cards : plans créés, temps d'exécution moyen
- Table : activité récente (existante, avec pagination)

**Librairie de charts** : Recharts (React) — léger, composable, compatible MUI. Angular : ngx-charts ou Chart.js via ng2-charts.

**Tests** :
- Tests unitaires pour toutes les nouvelles méthodes de StatsService
- Test d'intégration pour chaque nouvel endpoint
- Frontend : composants rendus avec données mockées

**Point de validation** : Dashboard affiche des insights pertinents sur une collection de 100+ fichiers enrichis

---

## 🚀 Phases Optionnelles (Post-MVP)

### Phase 14 : Fonctionnalités Avancées
- Analyse BPM/Key automatique (TarsosDSP)
- Support autres sources (Discogs, MusicBrainz)
- Détection doublons
- Playlist generator
- Backup/restore automatique

### Phase 15 : Collaboration
- Multi-utilisateurs
- Partage collections
- Cloud sync optionnel
- API publique pour intégrations

### Phase 16 : Intelligence
- Apprentissage préférences utilisateur
- Auto-catégorisation par style
- Recommandations basées usage

---

## 📊 Métriques de Succès par Phase

| Phase | Métrique Clé | Objectif |
|-------|-------------|----------|
| 1 | Fichiers scannés/sec | > 100 |
| 2 | Taux succès Spotify | > 85% |
| 3 | Temps réponse agent | < 2s |
| 4 | Questions posées | Seulement si conf < 70% |
| 5 | Tags appliqués sans erreur | > 99% |
| 6 | Précision similarité RAG | > 80% |
| 7-8 | UI responsive | < 100ms |
| 9 | Temps traitement 1000 fichiers | < 10min |
| 11 | Code coverage | > 80% |
| 12 | Taille installer | < 200MB |
| 13 | Dashboard chargé en | < 500ms |

---

## 🔄 Sprints Agile Suggérés

**Sprint 1-2** : Phases 1-2 (Backend + Spotify)  
**Sprint 3-4** : Phases 3-4 (Agent + Plan)  
**Sprint 5-6** : Phases 5-6 (Apply + RAG)  
**Sprint 7-8** : Phases 7-8 (Frontend base)  
**Sprint 9-10** : Phases 9-10 (Modes + UX)  
**Sprint 11-12** : Phases 11-12 (Tests + Release)
**Sprint 13** : Phase 13 (Dashboard & Analytics)

**MVP Ready** : Fin Sprint 9 (9 semaines)
**Production Ready** : Fin Sprint 12 (12 semaines)
**Analytics Ready** : Fin Sprint 13 (13 semaines)

---

## ⚠️ Risques & Mitigation

| Risque | Impact | Mitigation |
|--------|--------|------------|
| API Spotify rate limits | Haut | Cache + batch intelligent |
| Modèle IA lent | Moyen | Streaming responses + feedback |
| Corruption fichiers audio | Critique | Backup automatique avant apply |
| Gros dossiers (10k+ fichiers) | Moyen | Pagination + traitement async |
| Embedding coûteux | Moyen | Cache embeddings + lazy loading |

## Notes Importantes

### Priorités de Développement
1. **Backend d'abord** : L'intelligence est côté serveur
2. **MCP Tools bien définis** : C'est le cœur de l'agent
3. **Records partout** : Immutabilité et clarté du code
4. **RAG progressif** : Commencer simple, améliorer avec le temps

### Bonnes Pratiques
- **Records partout** : Utiliser des records pour tous les DTOs et réponses API
- **Validation stricte** : Validation dans les compact constructors des records
- **@HttpExchange** : Utiliser Spring 6.1+ HTTP Interface pour les appels Spotify (déclaratif, type-safe)
- **Structured Outputs** : Utiliser Spring AI 2.0 Structured Outputs pour les réponses de l'agent
- **Logging détaillé** : Pour debugging l'agent et traçabilité
- **Tests unitaires** : Sur chaque @Tool function
- **Documentation MCP** : Documentation claire des fonctions pour l'agent
- **Code concis** : Toujours extrêmement concis. Sacrifier grammaire au profit de la concision.
- **Noms explicites** : Privilégier méthodes qui parlent d'elles-mêmes plutôt que commentaires

### Performance
- Batch processing pour les gros dossiers
- Cache des résultats Spotify
- Index PostgreSQL sur les champs de recherche
- Limite de timeout pour les appels IA

### Sécurité
**CRITIQUE - Sélection de fichiers** :
- ✅ Seuls les fichiers **explicitement sélectionnés par l'utilisateur via l'interface Electron** peuvent être scannés
- ❌ **AUCUN scan automatique/récursif** de dossiers entiers par le backend
- 🔒 File picker Electron (dialog.showOpenDialog) = seul moyen de sélectionner fichiers
- 📋 Backend reçoit uniquement liste de chemins pré-approuvés par l'utilisateur
- 🛡️ Validation stricte chemins côté backend (anti-path-traversal)

**Autres points** :
- Credentials Spotify en environnement
- Rate limiting sur l'API Spotify
- Sanitization des inputs utilisateur

## Questions Ouvertes / Décisions à Prendre

1. **Analyse audio avancée** : Inclure une lib pour détecter BPM/clé automatiquement ou se fier uniquement à Spotify ?
2. **Rollback** : Implémenter un système de rollback des modifications ?
3. **Multi-utilisateurs** : Support de plusieurs utilisateurs/profils ?
4. **Cloud sync** : Synchronisation optionnelle des métadonnées en cloud ?
5. **Plugins** : Architecture de plugins pour d'autres sources (Discogs, MusicBrainz) ?

## Ressources

- **JAudiotagger** : https://github.com/ijabz/jaudiotagger
- **Spotify API** : https://developer.spotify.com/documentation/web-api
- **Spring AI** : https://docs.spring.io/spring-ai/reference/
- **Qdrant** : https://qdrant.tech/documentation/
- **Angular Material** : https://material.angular.io/

---

**Version** : 1.0.0  
**Auteur** : DJ Music Tagger Team  
**Date** : 2025

Ce document est le point d'entrée principal pour Claude Code. Toutes les décisions d'architecture et les spécifications détaillées sont ici.
