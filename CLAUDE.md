# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Ayan DJ Tools** — Desktop/web app for DJs to manage and auto-enrich audio file tags using an AI agent (Ayan). Scans user-selected audio files, detects missing tags, enriches metadata via Soundcharts + Spotify, and proposes modifications in 3 modes (Plan/Manual/Apply).

Full specification: `SPEC.md`

## Tech Stack

- **Backend**: Java 25, Spring Boot 4.0.6, Gradle 9.2 (Kotlin DSL)
- **AI**: LangChain4j 0.36.0, Ollama (modèle `llama3.1:8b` par défaut, configurable via `OLLAMA_CHAT_MODEL`)
- **Vector DB**: LangChain4j Qdrant integration (LangChain4j embedding store)
- **Audio tags**: JAudiotagger 3.0.1
- **Audio analysis**: TarsosDSP 2.5 (extraction locale BPM/features)
- **Metadata API**: Soundcharts (primary) + Spotify (secondary/fallback) via `@HttpExchange` declarative client
- **DB**: PostgreSQL 16 (`scanned_tracks`, `enriched_track_metadata`) + Redis 7 (conversations, plans, history)
- **Frontend**: Angular 21 web UI (`ayan_dj_tools_web/`), Flutter Desktop 3.22 (`ayan_dj_tools_flutter/`)
- **Docs**: SpringDoc OpenAPI 2.8.9 — Swagger UI `/swagger-ui`, Redoc `/docs/api-reference.html`, overview `/docs`
- **Language**: French project (comments, agent name, docs) but English code identifiers

## Build & Run Commands

```bash
# Build all modules
./gradlew build

# Run all tests
./gradlew test

# Run tests per module
./gradlew domain:test    # domain only, no Spring
./gradlew infra:test     # Spring Boot context + Testcontainers

# Run single test class
./gradlew domain:test --tests "com.djtools.ayan.musictagger.domain.model.vo.FilepathTest"
./gradlew infra:test --tests "com.djtools.ayan.musictagger.infrastructure.adapter.out.audio.JAudioTaggerAdapterTest"

# Run Spring Boot (port 8000)
./gradlew infra:bootRun

# Docker services (Ollama, Qdrant, PostgreSQL, Redis)
docker-compose up -d

# Pull AI models after Docker is up
docker exec -it dj-tagger-ollama ollama pull llama3.1:8b
docker exec -it dj-tagger-ollama ollama pull nomic-embed-text

# Frontend Angular
cd ayan_dj_tools_web && npm install && npm start   # http://localhost:4200

# Frontend Flutter
cd ayan_dj_tools_flutter && flutter pub get && flutter run -d windows
```

## Architecture (Hexagonal + DDD, 2 Gradle modules)

Le projet est séparé en 2 modules Gradle pour enforcer la règle de dépendance :

```
ayan-dj-tools/
├── build.gradle.kts           # Root — plugins apply false, subprojects config
├── settings.gradle.kts        # include("domain", "infra")
│
├── domain/                    # Module java-library — ZÉRO dépendance Spring/framework
│   └── src/main/java/.../domain/
│       ├── exception/         # AudioProcessingException
│       ├── model/             # MusicFileInfo, TaggingPlan, TagOperation, EnrichedTrackMetadata,
│       │                      # AudioFeatures, EnrichmentResult, SimilarTrackResult,
│       │                      # BatchApplyResult, TagWriteResult, TagPreview, TagChange,
│       │                      # TaggingHistoryEntry, Playlist, HarmonicPlaylist,
│       │                      # StatsReport, CollectionProfile, EnrichmentStats, ActivityTimeline,
│       │                      # FileBrowserPage, OperatingMode, OperationStatus, PlanStatus
│       │   └── vo/            # Filepath
│       ├── port/
│       │   ├── in/            # AudioFileReader, MusicMetadataProvider, AudioFeatureExtractor
│       │   └── out/           # AudioFileWriter, VectorStorePort, PlanRepository,
│       │                      # ScannedTrackRepository, TaggingHistoryRepository,
│       │                      # AudioFeaturesCacheRepository, EnrichedMetadataCacheRepository
│       └── usecase/           # ScanMusicUseCase, CreatePlanUseCase, ExecutePlanUseCase,
│                              # BuildHarmonicPlaylistUseCase, SearchSongsUseCase
│
└── infra/                     # Module Spring Boot — dépend de :domain
    └── src/main/java/.../infrastructure/
        ├── adapter/in/
        │   ├── mcp/           # 6 tool files (voir section LangChain4j)
        │   └── rest/          # 11 @RestController (voir section REST API)
        ├── adapter/out/
        │   ├── audio/         # JAudioTaggerAdapter, AudioScannerService
        │   ├── soundcharts/   # SoundchartsApiClient, SoundchartsMusicMetadataAdapter, DTOs
        │   ├── spotify/       # SpotifyApiClient, SpotifyTokenService, SpotifyMusicMetadataAdapter
        │   ├── vectorstore/   # QdrantVectorStoreAdapter
        │   └── persistence/   # RedisChatMemoryRepository (ChatMemoryStore LangChain4j),
        │                      # RedisPlanRepository, RedisTaggingHistoryRepository,
        │                      # PostgresScannedTrackRepository,
        │                      # PostgresEnrichedMetadataCacheRepository
        ├── service/           # AyanAgentService, AyanAssistant (LangChain4j @AiService),
        │                      # IntentType, MusicLookupService,
        │                      # PlanManagementService, ManualModeService, ApplyModeService,
        │                      # PlaylistService, PlaylistExportService,
        │                      # TrackVectorizationService, StatsService, ApiKeysService
        └── config/            # AIConfig, DomainConfig, RedisConfig, WebSocketConfig,
                               # CorsConfig, OpenApiConfig, DocsWebConfig
    └── src/main/resources/
        ├── application.yml
        ├── schema.sql
        └── static/
            ├── architecture.png
            └── docs/
                ├── index.html           # Overview Spring-docs style + diagrammes Mermaid
                └── api-reference.html   # Redoc (theme Spring green)
```

**Dependency rule**: `infra` → `domain`. Le module domain ne connaît ni Spring, ni LangChain4j.
- Les use cases sont des classes Java pures (pas de `@Service`)
- `DomainConfig.java` dans infra crée les beans domaine via `@Bean`

## REST API — 11 Controllers

| Contrôleur | Base path | Tag OpenAPI |
|---|---|---|
| `AgentController` | `/api/agent` | Agent |
| `PlanController` | `/api/plan` | Plan |
| `TagController` | `/api/tags` | Tags |
| `LibraryController` | `/api/library` | Library |
| `FileBrowserController` | `/api/files` | Files |
| `PlaylistController` | `/api/playlist` | Playlist |
| `PlaylistExportController` | `/api/playlist` | Playlist |
| `RagController` | `/api/rag` | RAG |
| `StatsController` | `/api/stats` | Stats |
| `SpotifyCheckController` | `/api/spotify` | Spotify |
| `ApiKeysController` | `/api/settings/keys` | Settings |

Tous les endpoints sont documentés avec `@Operation`, `@ApiResponse`, `@Parameter`, `@Schema`.

## LangChain4j Agent Architecture

### 6 assistants spécialisés

L'agent Ayan utilise **6 assistants LangChain4j** distincts, chacun avec un prompt système et un sous-ensemble de tools. Le routage se fait via un classificateur d'intention (appel rapide, `numCtx=512`) :

| Assistant | Intent | Tool class | Rôle |
|---|---|---|---|
| `fichiersAssistant` | `FICHIERS` | `FileOpsTools` | Scanner, analyser, enrichir fichiers |
| `planAssistant` | `PLANIFICATION` | `PlanTools` | Créer/exécuter plans de tags |
| `rechercheAssistant` | `RECHERCHE` | `SearchTools` | Recherche RAG, filtres collection |
| `playlistAssistant` | `PLAYLIST` | `PlaylistTools` | Génération playlists |
| `decouverteAssistant` | `DECOUVERTE` | `DiscoveryTools` | Infos artiste/album (sources externes) |
| `generalAssistant` | `GENERAL` | *(sans tools)* | Conversation, aide — streaming réel |

### MCP Tool files (`adapter/in/mcp/`)

| Fichier | Tools | Assigné à |
|---|---|---|
| `FileOpsTools.java` | scanMusicFile, detectMissingTags, enrichWithSoundcharts, browseFiles | fichiersAssistant |
| `PlanTools.java` | createPlanForFiles, applyTagsPlan, processNextFile, previewTagUpdate | planAssistant |
| `SearchTools.java` | findSimilarTracks, searchSongs, smartSuggestTags | rechercheAssistant |
| `PlaylistTools.java` | generateLoopMixingPlaylist, generateHarmonicMixedPlaylist, createThematicPlaylist | playlistAssistant |
| `DiscoveryTools.java` | lookupMusicInfo | decouverteAssistant |
| `AyanMusicTools.java` | Tous les 17 tools réunis (référence, non assigné directement) | — |

### Mémoire de conversation (LangChain4j)

- `RedisChatMemoryRepository implements ChatMemoryStore` (LangChain4j) — clé `chat-memory:`, TTL 24h
- `MessageWindowChatMemory.builder().id(convId).maxMessages(20).chatMemoryStore(store).build()`
- Un objet memory créé par `(convId, assistant)` via `chatMemoryProvider`
- `AiServices.builder(AyanAssistant.class).chatMemoryProvider(id -> memory(id, store)).build()`

### Streaming SSE

L'endpoint `POST /api/agent/chat/stream` retourne des **événements JSON** (pas du texte brut) :
```json
{"type":"thinking", "conversationId":"..."}          // heartbeat pendant le traitement
{"type":"chunk",    "token":"...", "conversationId":"..."}  // fragment (generalAssistant uniquement)
{"type":"done",     "reply":"...", "conversationId":"...", "intent":"FICHIERS", "timestamp":"..."}
{"type":"error",    "reply":"...", "conversationId":"..."}
```
Note : seul `generalAssistant` fait du vrai streaming token-by-token. Les autres assistants (avec tools) utilisent une exécution synchrone dans un virtual thread avec heartbeats.

## Documentation API

Disponible sur `http://localhost:8000` une fois le serveur démarré :

| URL | Contenu |
|---|---|
| `/docs` | Overview Spring-docs style avec sidebar, diagrammes Mermaid |
| `/docs/index.html` | Même page (redirect depuis `/docs`) |
| `/docs/api-reference.html` | Redoc — référence API complète, thème Spring green |
| `/swagger-ui` | Swagger UI interactif avec try-it-out |
| `/api-docs` | OpenAPI JSON brut |

**Contrainte** : les annotations `@Schema` ne sont **pas** ajoutées dans le module `domain/` (zéro dépendance externe). Elles sont réservées aux records définis dans `infra/` (request/response des contrôleurs).

## Key Patterns

- **Records everywhere**: All DTOs, value objects, API responses use Java records
- **@HttpExchange**: Declarative API clients for Soundcharts and Spotify (no WebClient boilerplate)
- **LangChain4j AiServices**: `AiServices.builder(AyanAssistant.class).tools(toolClass).chatMemoryProvider(...).build()`
- **Intent routing**: fast LangChain4j classifier (512 ctx, 10 predicted tokens) routes to the right assistant
- **3 operating modes**: PLAN (batch review → approve → execute), MANUAL (file-by-file confirm), APPLY (auto async)
- **Backup/rollback**: JAudioTaggerAdapter creates a backup before every write; auto-restores on failure
- **Parallel enrichment**: `FileBrowserController` runs analyze/enrich with 4-thread pool via `CompletableFuture`
- **No Spring AI**: The project uses LangChain4j 0.36.0, NOT Spring AI. Do not introduce Spring AI dependencies.

## Security Constraints

- **File access**: ONLY files explicitly provided by the client are processed. Paths containing `..` are rejected (HTTP 400). NO autonomous recursive scanning by the backend.
- **CORS**: All origins allowed (`*`), `allowCredentials: false` — local desktop app only.
- **Credentials**: API keys (Soundcharts, Spotify, Tavily) via env vars or `PUT /api/settings/keys`. Never returned in plain text.
- **Auth HTTP**: None — application runs locally only. No token required for REST endpoints.

## Testing Patterns

- Domain tests: pure Java, no Spring context
- Infra IT tests: `@SpringBootTest(classes = {...})` with focused context + `@EnableAutoConfiguration(exclude = {...})`
- Integration tests use Testcontainers: `RedisContainer`, `PostgreSQLContainer`, `QdrantContainer`
- `infra/src/test/resources/application.yml` required for IT tests (config placeholders)
- `MockWebServer` (okhttp3 4.12.0) for HTTP client tests
- `TestAudioFileHelper` generates test MP3 files programmatically (raw MPEG frames + ID3v2.4 tags)
- LangChain4j embedding test model: `langchain4j-embeddings-all-minilm-l6-v2-q` (Testcontainers)

## Skills Reference

Detailed implementation patterns live in `.claude/skills/` (auto-loaded when relevant):

| Skill | Purpose |
|-------|---------|
| `hexagonal-ddd/` | Architecture rules, domain/infra separation |
| `backend-java/` | Java 25 + Spring Boot 4 + Gradle 9 patterns |
| `spring-ai/` | Ignore — ce projet utilise LangChain4j, pas Spring AI |
| `spotify-integration/` | @HttpExchange client, OAuth2, cache, rate limiting |
| `audio-processing/` | JAudiotagger read/write, validation, backup |
| `rag-vectordb/` | Qdrant vectorization, similarity search (via LangChain4j) |
| `angular-developer/` | Angular 21 web UI — signals, linkedSignal, resource, forms, routing, ARIA, Tailwind |

## Implementation Phases

1. ✅ Foundation Backend (scan files, read tags, detect missing tags)
2. ✅ Spotify Integration → remplacé par Soundcharts (source primaire)
3. ✅ Spring AI + Agent → implémenté avec LangChain4j 0.36.0
4. ✅ Mode PLAN (TaggingPlan, TagOperation, CreatePlanUseCase, RedisPlanRepository, PlanController)
5. ✅ Tag Application (write tags, backup/rollback, preview diffs, history)
6. ✅ RAG + Vector Store (Qdrant via LangChain4j, nomic-embed-text, semantic search)
7. ✅ Frontend Structure (Angular 21, layout, chat, file-list, WebSocket STOMP)
8. ✅ Frontend Modes & Plan Review (ModeService, ConfirmDialog, PlanReview, History, Settings)
9. ✅ Mode MANUAL & APPLY (ManualModeView, ApplyModeView, usePlanProgress)
10. ✅ Polish & UX
11. ✅ Documentation API (SpringDoc 2.8.9, @Operation/@ApiResponse/@Schema, Redoc, overview Mermaid)

**Current state**: Phases 1–11 complete.

## Code Style

- Concise code. Favor self-documenting method names over comments.
- French for documentation/agent personality, English for code identifiers.
- Use records with compact constructors for validation.
- Prefer `@HttpExchange` interfaces over manual WebClient calls.
- No `@Schema` annotations in the `domain/` module (zero external dependencies rule).
- Do NOT use Spring AI APIs — the project uses LangChain4j exclusively.
