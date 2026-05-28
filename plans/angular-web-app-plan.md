# Plan: Angular 21 Web App for Ayan DJ Tools

> Source PRD: `SPEC.md` + backend API surface in `infra`

## Architectural decisions

Durable decisions that apply across all phases:

- **Runtime**: Browser-first Angular 21 SPA (no Electron in this plan).
- **API boundary**: Spring backend remains source of truth (`/api/*` + STOMP `/ws`).
- **Library selection strategy**: Browser sends a root directory path to backend for paginated browse; backend resolves local files.
- **Routes**: `/chat`, `/plan/:id`, `/history`, `/playlist`, `/stats`, `/settings`.
- **Operating modes**: `PLAN`, `MANUAL`, `APPLY` mapped to backend `OperatingMode`.
- **Key models**: `TaggingPlan`, `TagOperation`, `TagPreview`, `BatchApplyResult`, `TaggingHistoryEntry`, `Playlist`, `StatsReport`, `SimilarTrackResult`.
- **Realtime**: STOMP topics for chat streaming and plan progress (`/topic/responses/{conversationId}`, `/topic/plan/{id}/progress`).
- **Cross-cutting**: FR/EN i18n, theme preference persistence, robust network error states.

---

## Phase 1: Angular 21 Foundation and Backend Connectivity

**User stories**: En tant qu'utilisateur, je peux ouvrir l'application web et vérifier rapidement si le backend est disponible.

### What to build

Create the Angular 21 application shell, environment configuration, and typed HTTP/STOMP clients to communicate with the existing backend. Add a global backend status indicator visible from every page.

### Acceptance criteria

- [ ] App boots in browser with Angular 21 and production build pipeline.
- [ ] Health/connectivity status is visible and refreshes automatically.
- [ ] REST and STOMP clients are configured and reusable across features.

---

## Phase 2: App Shell, Navigation, and Settings Baseline

**User stories**: En tant qu'utilisateur, je peux naviguer entre les sections principales et conserver mes préférences.

### What to build

Implement a stable app layout with route navigation for all core views. Add settings for backend URL, websocket toggle, default mode, language, and theme with local persistence.

### Acceptance criteria

- [ ] All target routes are accessible from a persistent navigation shell.
- [ ] Preferences persist across reloads.
- [ ] FR and EN UI strings are switchable without breaking navigation.

---

## Phase 3: Library Browsing and Plan Creation (Browser-Compatible)

**User stories**: En tant qu'utilisateur web, je saisis un dossier, je parcours ma librairie audio paginée, puis je crée un plan de tagging.

### What to build

Add a browser-safe library workflow: user enters a backend-local directory path, browses folders/files via pagination, selects tracks, and creates a plan using selected file paths and mode.

### Acceptance criteria

- [ ] Directory browse works with pagination and folder drill-down.
- [ ] Users can select/unselect files and launch plan creation.
- [ ] Plan creation handles empty selection and invalid path errors clearly.

---

## Phase 4: AI Chat with Streaming Responses

**User stories**: En tant qu'utilisateur, je discute avec Ayan et vois la réponse arriver en temps réel.

### What to build

Implement chat UI connected to backend conversation endpoints, with token streaming over STOMP, conversation continuity, history loading, and interruption support.

### Acceptance criteria

- [ ] Messages are sent and streamed responses render incrementally.
- [ ] Conversation history can be reloaded for a conversation id.
- [ ] Stop/cancel interaction correctly interrupts active streaming.

---

## Phase 5: PLAN Mode Review and Execution

**User stories**: En tant qu'utilisateur, je révise les suggestions, approuve le plan puis exécute les changements.

### What to build

Build plan detail/review view showing operations, tag diffs, per-file status, and summary. Support approve and execute actions with clear status transitions.

### Acceptance criteria

- [ ] Plan details load from `planId` route and show actionable review data.
- [ ] Approve and execute actions follow backend status rules.
- [ ] Execution result summary displays success and failure counts.

---

## Phase 6: MANUAL and APPLY Modes with Realtime Progress

**User stories**: En tant qu'utilisateur, je peux traiter fichier par fichier en manuel ou lancer l'exécution auto et suivre la progression.

### What to build

Implement dedicated mode flows backed by the same plan resource: sequential confirm/reject for MANUAL, asynchronous auto-execution for APPLY, and shared live progress timeline from STOMP.

### Acceptance criteria

- [ ] MANUAL mode supports next operation, approve/reject, and completion state.
- [ ] APPLY mode triggers async execution and shows live updates.
- [ ] Progress UI remains consistent after refresh/reconnect.

---

## Phase 7: History and Tag Diff Exploration

**User stories**: En tant qu'utilisateur, je recherche un plan et consulte l'historique détaillé des tags appliqués.

### What to build

Create history screen with plan lookup and expandable entries showing before/after tag changes and outcome per file.

### Acceptance criteria

- [ ] Plan history can be fetched by `planId`.
- [ ] Each entry exposes clear old/new tag values and status.
- [ ] Empty and not-found states are handled explicitly.

---

## Phase 8: Playlist, Similar Tracks (RAG), and Stats Dashboard

**User stories**: En tant qu'utilisateur, je génère une playlist, recherche des morceaux similaires, et analyse ma collection.

### What to build

Integrate playlist generation filters, semantic similarity search, and a multi-tab stats dashboard (collection, enrichment, activity) based on current backend endpoints.

### Acceptance criteria

- [ ] Playlist generation supports bpm/genre filtering and renders usable results.
- [ ] Similar track query returns and displays ranked matches.
- [ ] Stats tabs load and present consistent metrics across endpoints.

---

## Phase 9: Web Hardening, Test Strategy, and Release Readiness

**User stories**: En tant qu'utilisateur, je dispose d'une application web stable et fiable en usage réel.

### What to build

Harden browser deployment: CORS validation, network resilience, retry/reconnect behavior for STOMP, and focused automated tests (critical flows + regressions). Prepare production build and deployment checklist.

### Acceptance criteria

- [ ] Critical user journeys are covered by automated tests.
- [ ] Realtime and network failure scenarios degrade gracefully.
- [ ] Production build is reproducible with a documented runbook.

