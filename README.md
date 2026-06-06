# Ayan DJ Tools

Application pour DJs — enrichissement automatique des tags audio via un agent IA (Ayan).
Scanne les fichiers audio, détecte les tags manquants, enrichit les métadonnées via Soundcharts,
génère des playlists harmoniques et thématiques, et propose les modifications en mode Plan, Manuel ou Auto.

## Prérequis

| Outil | Version minimale |
|-------|-----------------|
| Java (JDK) | 25 |
| Node.js | 22+ |
| Docker Desktop | 27+ |
| Git | 2.x |

---

## Architecture

```mermaid
graph TB
    subgraph FE["Frontend — Angular 21"]
        direction LR
        Chat["💬 Chat"]
        Plan["📋 Plan Review"]
        PL["🎵 Playlist & RAG"]
        Stats["📊 Stats"]
        Settings["⚙️ Paramètres"]
    end

    subgraph BE["Backend — Spring Boot 4 · infra module"]
        direction TB
        REST["REST Controllers\n/api/**"]
        WS["WebSocket STOMP\n/ws"]

        subgraph SVC["Services"]
            AgentSvc["AyanAgentService\n(LangChain4j)"]
            PlaylistSvc["PlaylistService"]
            VectorSvc["TrackVectorizationService"]
            PlanSvc["PlanManagementService"]
        end

        subgraph TOOLS["@Tool functions (MCP)"]
            AyanTools["AyanMusicTools\nscan · enrich · plan · apply · search"]
            PLTools["PlaylistTools\nloop · harmonic · thematic"]
        end

        subgraph OUT["Adapters out"]
            SC["Soundcharts API\nenrichissement primaire"]
            Spotify["Spotify API\nfallback enrichissement"]
            Qdrant["Qdrant\nvector store RAG"]
            PG["PostgreSQL\ntracks · plans · historique"]
            Redis["Redis\nchat memory · plans"]
            Audio["JAudiotagger\nlecture / écriture tags"]
        end
    end

    subgraph DOM["domain module — Java pur, zéro Spring"]
        UC["Use Cases\nScan · CreatePlan\nExecutePlan · BuildHarmonicPlaylist"]
        Ports["Ports in/out"]
        Models["Models · Value Objects"]
    end

    FE -- "HTTP + SSE" --> REST
    FE <-- "STOMP frames" --> WS
    REST --> SVC
    WS --> AgentSvc
    AgentSvc --> TOOLS
    TOOLS --> SVC
    SVC --> OUT
    SVC --> UC
    UC --> Ports
    Ports -.->|implémentés par| OUT
```

### Séparation domain / infra

Le module `domain` ne connaît ni Spring, ni JAudiotagger, ni aucun framework tiers.
Le module `infra` dépend de `domain` et fournit toutes les implémentations concrètes via `DomainConfig`.

```mermaid
graph LR
    infra -->|dépend de| domain
    domain -. "aucune dépendance" .-> infra
    style domain fill:#1a3a1a,stroke:#4caf50,color:#e8f5e9
    style infra fill:#1a1a3a,stroke:#5c6bc0,color:#e8eaf6
```

---

## Flux d'enrichissement

```mermaid
sequenceDiagram
    actor DJ
    participant Chat as Chat Angular
    participant Agent as Agent IA (Ayan)
    participant SC as Soundcharts
    participant PG as PostgreSQL
    participant Qdrant

    DJ->>Chat: "Enrichis Lost in Music – Maze"
    Chat->>Agent: POST /api/agent/chat/stream (SSE)
    Agent->>Agent: enrichWithSoundcharts("Maze", "Lost in Music")
    Agent->>SC: GET /api/v2/song/search/Lost in Music
    SC-->>Agent: [{uuid, name, creditName…}]
    Agent->>SC: GET /api/v2.25/song/{uuid}
    SC-->>Agent: {genres, audio{bpm,key,energy…}, isrc, labels…}
    Agent->>PG: upsert enriched_track_metadata
    Agent->>Qdrant: store Document (embedding artist+genres+lyrics)
    Agent-->>Chat: SSE chunks (thinking → reply)
    Chat-->>DJ: "Enrichi : BPM=120, tonalité=8A, genres=[Funk, Soul]"
```

---

## Génération de playlists

Les trois types de playlist partagent le même pipeline RAG → filtre → séquençage :

```mermaid
flowchart LR
    Input(["Critères\nBPM · Genre · Énergie\nThème / Ambiance"])
    Query["Requête sémantique\n(RAG)"]
    Qdrant[("Qdrant\nvector store")]
    Pool["Pool de candidats\n(20–200 tracks)"]
    Filter["Filtre BPM\n+ critères"]

    Input --> Query --> Qdrant --> Pool --> Filter

    Filter --> LoopMode["🔁 Loop Mixing\nDanceability ≥ 0.5\n→ top 10"]
    Filter --> HarmMode["🎹 Harmonique\nCamelot Wheel\n±6 BPM"]
    Filter --> ThemMode["🌊 Thématique\nArc narratif\nintro→build→peak→outro"]

    LoopMode --> Out(["Playlist finale\n+ export M3U"])
    HarmMode --> Out
    ThemMode --> Out
```

### Arc narratif (playlist thématique)

Les tracks triés par énergie croissante sont répartis en 4 quartiles puis reordonnés :

```mermaid
graph LR
    A["🔵 Intro\n(énergie basse)"]
    B["🟢 Montée\n(énergie moyenne-basse)"]
    C["🔴 Peak\n(énergie haute)"]
    D["🟣 Outro\n(énergie moyenne-haute)"]

    A --> B --> C --> D

    style A fill:#1565c0,color:#fff,stroke:none
    style B fill:#558b2f,color:#fff,stroke:none
    style C fill:#c62828,color:#fff,stroke:none
    style D fill:#6a1b9a,color:#fff,stroke:none
```

---

## Cycle de vie d'un plan

```mermaid
stateDiagram-v2
    [*] --> DRAFT : createPlan()

    DRAFT --> READY_FOR_REVIEW : scan + enrichissement terminés

    READY_FOR_REVIEW --> APPROVED : approvePlan()
    READY_FOR_REVIEW --> [*] : deletePlan()

    APPROVED --> APPLYING : executePlan()

    APPLYING --> COMPLETED : toutes opérations terminées\n(succès ou erreurs partielles)

    COMPLETED --> [*]
```

Chaque `TagOperation` dans le plan a son propre statut :

```mermaid
stateDiagram-v2
    direction LR
    [*] --> PENDING
    PENDING --> APPROVED : approuver
    PENDING --> REJECTED : rejeter
    APPROVED --> APPLIED : écriture tag OK
    APPROVED --> ERROR : échec écriture
```

---

## Démarrage rapide

### 1. Services Docker (PostgreSQL, Redis, Qdrant, Ollama)

```bash
docker-compose up -d

# Télécharger les modèles IA après le démarrage
docker exec -it dj-tagger-ollama ollama pull llama3.1:8b
docker exec -it dj-tagger-ollama ollama pull nomic-embed-text
```

### 2. Backend Spring Boot

```bash
./gradlew infra:bootRun
```

### 3. Frontend Angular

```bash
cd ayan_dj_tools_web
npm install
npm start          # http://localhost:4200
```

---

## Configuration

### Clés API

Les clés se configurent depuis l'interface → Paramètres (`Ctrl+,`).
Elles peuvent aussi être fournies via variables d'environnement avant de lancer le backend :

| Variable | Service | Rôle |
|----------|---------|------|
| `SOUNDCHARTS_APP_ID` | Soundcharts | Enrichissement métadonnées (source primaire) |
| `SOUNDCHARTS_API_KEY` | Soundcharts | Enrichissement métadonnées (source primaire) |
| `SPOTIFY_CLIENT_ID` | Spotify | Enrichissement (fallback si Soundcharts indisponible) |
| `SPOTIFY_CLIENT_SECRET` | Spotify | Enrichissement (fallback) |
| `TAVILY_API_KEY` | Tavily | Recherche web dans l'agent (`lookupMusicInfo`) |

Sans ces clés, l'agent fonctionne en mode dégradé (scan local uniquement, pas d'enrichissement externe).

---

## Ports utilisés

| Service | Port | Protocole |
|---------|------|-----------|
| Backend Spring Boot | 8000 | HTTP / WebSocket STOMP |
| SwaggerUI / OpenAPI | 8000 | HTTP (`/docs`, `/api-docs`) |
| PostgreSQL | 5432 | TCP |
| Redis | 6379 | TCP |
| Qdrant (REST) | 6333 | HTTP |
| Qdrant (gRPC) | 6334 | gRPC |
| Ollama | 11434 | HTTP |

---

## Écrans de l'application

### Chat — écran d'accueil (`/`)

Interface de conversation avec l'agent IA Ayan en langage naturel.

Capacités principales :
- **Scanner** des fichiers audio et **détecter les tags manquants**
- **Suggérer** artiste/titre à partir du nom de fichier
- **Enrichir** les métadonnées via Soundcharts (BPM, tonalité, genres, paroles, popularité…)
- **Créer un plan** de modifications, puis **prévisualiser** et **appliquer** les tags (backup + rollback)
- **Consulter l'historique** des modifications
- **Rechercher des morceaux par critères** en langage naturel — genre, BPM, énergie, années, ambiance.
  Ex. : *« donne-moi 10 morceaux house énergiques entre 120 et 130 BPM »*
- **Rechercher des morceaux similaires** (RAG sémantique via Qdrant)
- **Générer des playlists** — loop mixing, mix harmonique Camelot ou arc thématique
- **Rechercher des informations musicales** (artiste, album, morceau) via Soundcharts + web

### Plan (`/plan/:id`)

Révision du plan de tagging proposé par l'agent. Trois modes selon la préférence choisie :

| Mode | Comportement |
|------|-------------|
| **Plan** | Liste complète des opérations à approuver/rejeter, puis exécution en lot. |
| **Manuel** | Fichier par fichier, avec progression STOMP en temps réel. |
| **Auto** | Exécution automatique avec log de progression animé. |

### Playlist & RAG (`/playlist`) — `Ctrl+L`

- **Playlist Thématique** — arc narratif basé sur les paroles et l'ambiance analysées.
  Ex. : *liberté danse africa*, *mélancolie pluie*, *été festif soleil*.
- **Génération Playlist (loop mixing)** — filtres BPM min/max + genre, tri par danceability.
- **Mix Harmonique (Camelot)** — séquençage façon *Mixed In Key* avec badges de clé Camelot,
  types de transitions (`PERFECT_MATCH`, `ADJACENT_KEY`, `MODE_CHANGE`, `JUMP`) et stats globales.
- **Recherche Similaire (RAG)** — requête texte libre avec score de similarité.

### Historique (`/history`) — `Ctrl+H`

Recherche par ID de plan. Diff complet des tags (avant / après) avec statut par fichier.

### Statistiques (`/stats`) — `Ctrl+S`

| Onglet | Contenu |
|--------|---------|
| **Collection** | Pistes scannées/enrichies, distribution genres, histogramme BPM, Camelot Wheel. |
| **Enrichissement** | Taux de correspondance Soundcharts, taux d'erreur, tags enrichis par type. |
| **Activité** | Tags appliqués par période, utilisation des modes, plans créés. |

### Paramètres (`/settings`) — `Ctrl+,`

- URL API backend (configurable dynamiquement)
- Clés API (Soundcharts, Spotify, Tavily) — masquées après enregistrement
- Mode par défaut (Plan / Manuel / Auto)
- Basculement thème sombre/clair · Langue (Français / English)
- Export / import configuration JSON

---

## Développement

### Backend

```bash
./gradlew build                  # build complet
./gradlew test                   # tous les tests
./gradlew domain:test            # domaine pur (sans Spring)
./gradlew infra:test             # Spring + Testcontainers
./gradlew infra:bootRun          # lancement dev
./gradlew infra:bootJar          # JAR de distribution → infra/build/libs/
```

### Frontend

```bash
cd ayan_dj_tools_web
npm install
npm start        # dev (localhost:4200, proxy → localhost:8000)
npm run build    # production
npm test         # vitest
```

---

## Raccourcis clavier

| Raccourci | Action |
|-----------|--------|
| `Ctrl+P` | Chat (accueil) |
| `Ctrl+H` | Historique |
| `Ctrl+S` | Statistiques |
| `Ctrl+L` | Playlist & RAG |
| `Ctrl+,` | Paramètres |
| `Ctrl+O` | Ouvrir fichiers |
| `?` | Aide raccourcis |
