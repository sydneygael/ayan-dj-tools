# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Ayan DJ Tools** — Desktop app for DJs to manage and auto-enrich audio file tags using an AI agent (Ayan). Scans user-selected audio files, detects missing tags, enriches metadata via Spotify API, and proposes modifications in 3 modes (Plan/Manual/Apply).

Full specification: `SPEC.md`

## Tech Stack

- **Backend**: Java 25, Spring Boot 4.0.2, Gradle 9.2 (Kotlin DSL)
- **AI**: Spring AI 2.0.0-M2, Ollama/Mistral, Qdrant (vector DB), redis
- **Audio**: JAudiotagger 3.0.1
- **API**: Spotify via `@HttpExchange` declarative client
- **DB**: PostgreSQL + pgvector
- **Frontend**: React 19, Vite, MUI v6, Zustand, Electron 40
- **Language**: French project (comments, agent name, docs) but English code identifiers

## Build & Run Commands

```bash
# Build all modules
./gradlew build

# Run all tests
./gradlew test

# Run tests per module
./gradlew domain:test    # 22 tests (domain only, no Spring)
./gradlew infra:test     # 22 tests (Spring Boot context)

# Run single test class
./gradlew domain:test --tests "com.djtools.ayan.musictagger.domain.model.vo.FilepathTest"
./gradlew infra:test --tests "com.djtools.ayan.musictagger.infrastructure.adapter.out.audio.JAudioTaggerAdapterTest"
./gradlew infra:test --tests "com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.SpotifyMusicMetadataAdapterTest"

# Run Spring Boot
./gradlew infra:bootRun

# Docker services (Ollama, Qdrant, PostgreSQL)
docker-compose up -d

# Pull AI models after Docker is up
docker exec -it dj-tagger-ollama ollama pull mistral
docker exec -it dj-tagger-ollama ollama pull nomic-embed-text

# Frontend
cd music-tagger-ui && npm install && npm run dev
```

## Architecture (Hexagonal + DDD, 2 Gradle modules)

Le projet est séparé en 2 modules Gradle pour enforcer la règle de dépendance :

```
ayan-dj-tools/
├── build.gradle.kts           # Root — plugins apply false, subprojects config
├── settings.gradle.kts        # include("domain", "infra")
│
├── domain/                    # Module java-library — ZÉRO dépendance Spring
│   ├── build.gradle.kts
│   └── src/main/java/com/djtools/ayan/musictagger/domain/
│       ├── exception/         # AudioProcessingException
│       ├── model/             # MusicFileInfo, MissingTagsReport, AudioFeatures, EnrichedTrackMetadata, EnrichmentResult
│       │   └── vo/            # Filepath (+ futurs: BPM, MusicalKey, CamelotKey)
│       ├── port/
│       │   ├── in/            # AudioFileReader, MusicMetadataProvider
│       │   └── out/           # (futurs: AudioRepository, VectorStorePort, AIAgentPort)
│       └── usecase/           # ScanMusicUseCase (plain class, pas de @Service)
│
└── infra/                     # Module Spring Boot — dépend de :domain
    ├── build.gradle.kts
    └── src/main/java/com/djtools/ayan/musictagger/
        ├── MusicTaggerApplication.java
        └── infrastructure/
            ├── adapter/in/
            │   ├── mcp/       # AyanMusicTools (@Tool functions)
            │   ├── rest/      # AgentController, PlanController
            │   └── ws/        # AgentWebSocketController
            ├── adapter/out/
            │   ├── audio/     # JAudioTaggerAdapter, AudioScannerService
            │   ├── persistence/ # RedisPlanRepository
            │   └── spotify/   # SpotifyMusicMetadataAdapter, SpotifyApiClient, SpotifyTokenService
            │       ├── dto/   # SpotifySearchResponse, SpotifyTrackItem, SpotifyAudioFeatures, etc.
            │       └── exception/ # SpotifyApiException, SpotifyAuthException, SpotifyRateLimitException
            ├── service/       # AyanAgentService, PlanManagementService, ConversationHistoryService
            └── config/        # DomainConfig, AIConfig, RedisConfig, WebSocketConfig
```

**Dependency rule**: `infra` → `domain`. Le module domain ne connaît ni Spring, ni JAudiotagger.
- `ScanMusicUseCase` est une classe pure Java (pas de `@Service`)
- `DomainConfig.java` dans infra crée les beans domaine via `@Bean`

**Port `MusicMetadataProvider`**: Le domaine définit une abstraction générique pour l'enrichissement de métadonnées musicales. Une implémentation dans l'infrastructure : `SpotifyMusicMetadataAdapter` (audioFeatures, popularity, genres, album, etc.).

## Key Patterns

- **Records everywhere**: All DTOs, value objects, API responses use Java records
- **@HttpExchange**: Declarative API client for Spotify (no external libs)
- **Spring AI Structured Outputs**: `chatClient.prompt().call().entity(MyRecord.class)` for type-safe AI responses
- **@Tool functions**: MCP tools in `AyanMusicTools.java` — scan, enrich, suggest, apply tags
- **3 operating modes**: PLAN (batch review), MANUAL (one-by-one confirm), APPLY (auto)

## Security Constraints

- **File access**: ONLY files explicitly selected via Electron file picker are allowed. NO recursive scanning by backend.
- Backend receives pre-approved file paths only. Validate against path traversal.
- Spotify credentials via environment variables only.

## Skills Reference

Detailed implementation patterns live in `.claude/skills/` (auto-loaded when relevant):

| Skill | Purpose |
|-------|---------|
| `hexagonal-ddd/` | Architecture rules, domain/infra separation |
| `backend-java/` | Java 25 + Spring Boot 4 + Gradle 9 patterns |
| `spring-ai/` | Spring AI 2.0, @Tool, structured outputs, Camelot Wheel |
| `spotify-integration/` | @HttpExchange client, OAuth2, cache, rate limiting |
| `audio-processing/` | JAudiotagger read/write, validation, backup |
| `rag-vectordb/` | Qdrant vectorization, similarity search |
| `frontend-react/`   | React 19 + Vite + MUI v6 + Zustand + Electron patterns |

## Implementation Phases

Project follows 13 phases (details in `SPEC.md`):
1. Foundation Backend (scan files)
2. Spotify Integration
3. Spring AI + Agent
4. Mode PLAN
5. Tag Application
6. RAG + Vector Store
7. Frontend Structure
8. Frontend Modes & Plan Review
9. Mode MANUAL & APPLY
10. Polish & UX
11. Tests & Quality
12. Packaging & Distribution

**Current state**: Phases 1–13 complete. Phase 11 next: Tests & Quality.

## Code Style

- Concise code. Favor self-documenting method names over comments.
- French for documentation/agent personality, English for code identifiers.
- Use records with compact constructors for validation.
- Prefer `@HttpExchange` interfaces over manual WebClient calls.
