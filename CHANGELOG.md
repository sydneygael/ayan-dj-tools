# Changelog

Toutes les modifications notables de ce projet sont documentées ici.

Format : [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/)
Versionnage : [Semantic Versioning](https://semver.org/lang/fr/)

---

## [0.1.0] — 2026-03-24

### Ajouté

#### Phase 1 — Foundation Backend
- Module Gradle `domain` : use case `ScanMusicUseCase`, value objects `Filepath`, `MusicFileInfo`, `MissingTagsReport`
- Module Gradle `infra` : adapter `JAudioTaggerAdapter` (lecture tags MP3/FLAC/WAV/AIFF/M4A/OGG)
- Séparation hexagonale stricte — `domain` sans dépendance Spring

#### Phase 2 — Intégration Spotify
- Client déclaratif `@HttpExchange` pour l'API Spotify
- `SpotifyMusicMetadataAdapter` : enrichissement 14 champs (genres, BPM, clé, popularité…)
- Cache Caffeine 60 min + rate limiter Guava 10 req/s
- OAuth2 client credentials avec refresh automatique

#### Phase 3 — Agent IA Ayan
- Intégration Spring AI 2.0.0-M2 avec Ollama/Mistral
- 10 fonctions `@Tool` : scan, enrich, suggest, apply, plan, RAG…
- WebSocket STOMP (SockJS) pour le chat temps réel
- Historique de conversation Redis (TTL 24h)

#### Phase 4 — Mode Plan
- `TaggingPlan`, `TagOperation`, `PlanStatus`, `OperationStatus`
- `CreatePlanUseCase` : analyse batch, enrichissement Spotify, construction du plan
- `PlanController` REST : create, approve, execute, preview, history, delete
- `RedisPlanRepository` (TTL 48h)

#### Phase 5 — Application des Tags
- `ExecutePlanUseCase` : écriture tags avec backup/rollback automatique
- `TaggingHistoryRepository` Redis (TTL 7 jours)
- `TagController` REST : apply, preview
- Diff avant/après par fichier

#### Phase 6 — RAG + Vector Store
- `QdrantVectorStoreAdapter` : vectorisation des tracks enrichies
- `TrackVectorizationService` : recherche sémantique + suggestions intelligentes
- `RagController` : GET /api/rag/similar
- Auto-vectorisation lors de l'enrichissement Spotify

#### Phase 7 — Structure Frontend
- Application React 19 + Vite + MUI v7 + Zustand + Electron 40
- Layout : toolbar + sidebar + router-outlet
- Thème sombre/clair (persisté localStorage)
- Chat WebSocket STOMP + fallback REST

#### Phase 8 — Modes & Révision de Plan
- `ModeSelector` global (PLAN / MANUEL / AUTO)
- `PlanReviewPage` : résumé + cartes d'opérations + barre de progression
- `HistoryPage` : recherche par planId, tableau extensible
- `SettingsPage` : URL API, mode défaut, export/import JSON

#### Phase 9 — Mode Manuel & Auto
- `ManualModeView` : approbation séquentielle fichier par fichier
- `ApplyModeView` : exécution automatique avec log temps réel
- Hook `usePlanProgress` (STOMP `/topic/plan/{id}/progress`)
- Types corrigés : `PlanStatus`, `OperationStatus`, `BatchApplyResult`

#### Phase 10 — Polish & UX
- Lecteur audio HTML5 dans la sidebar (formats `file://`)
- Drag & drop de fichiers audio
- Raccourcis clavier globaux (Ctrl+P/M/A/H/S/,/O + ? aide)
- Internationalisation FR/EN (react-i18next)

#### Phase 11 — Tests & Qualité
- ~42 tests domaine (JUnit 5, aucune dépendance Spring)
- ~100 tests infra (Spring Boot Test, Testcontainers Redis + Qdrant)
- Tests composants React (Vitest + Testing Library)
- `StatsController` + dashboard stats 3 onglets (Collection / Enrichissement / Activité)

#### Phase 12 — Packaging & Distribution
- Configuration electron-builder : NSIS (Windows), DMG (macOS), AppImage+deb (Linux)
- `electron/main.ts` : lancement JAR, auto-updater, menu natif, graceful shutdown
- `BackendStatusChip` : indicateur vert/rouge du backend dans la toolbar
- Scripts `start-services.sh/.bat` et `build-all.sh/.bat`
- `electron-updater` : mise à jour auto via GitHub Releases
- Endpoint `/actuator/health` exposé via Spring Boot Actuator
- CORS étendu : `file://*` + `localhost:5173` pour Electron + Vite dev

[0.1.0]: https://github.com/your-org/ayan-dj-tools/releases/tag/v0.1.0
