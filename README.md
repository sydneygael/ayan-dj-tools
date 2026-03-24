# Ayan DJ Tools

Application desktop pour DJs — enrichissement automatique des tags audio via IA (agent Ayan).

## Prérequis

| Outil | Version minimale |
|-------|-----------------|
| Java (JDK) | 21+ |
| Node.js | 20+ |
| Docker Desktop | 27+ |
| Git | 2.x |

> **Note beta** : L'installeur n'est pas encore signé. Sur Windows, cliquez « Plus d'informations » → « Exécuter quand même » pour contourner SmartScreen. Sur macOS, autorisez l'app dans Réglages → Confidentialité & Sécurité.

---

## Installation rapide (beta-testeurs)

1. Téléchargez le dernier installeur depuis [GitHub Releases](https://github.com/your-org/ayan-dj-tools/releases)
2. Lancez les services Docker :
   ```bash
   ./scripts/start-services.sh   # Linux/macOS
   scripts\start-services.bat    # Windows
   ```
3. Lancez l'application installée — le backend démarre automatiquement

---

## Configuration Spotify

Les credentials Spotify sont optionnels mais activent l'enrichissement automatique des tags.

1. Créez une app sur [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Définissez les variables d'environnement avant de lancer le backend :
   ```bash
   export SPOTIFY_CLIENT_ID=votre_client_id
   export SPOTIFY_CLIENT_SECRET=votre_client_secret
   ```
   Ou en Windows :
   ```bat
   set SPOTIFY_CLIENT_ID=votre_client_id
   set SPOTIFY_CLIENT_SECRET=votre_client_secret
   ```

---

## Ports utilisés

| Service | Port | Protocole |
|---------|------|-----------|
| Backend Spring Boot | 8000 | HTTP / WebSocket STOMP |
| SwaggerUI / OpenAPI | 8000 | HTTP (`/docs`, `/api-docs`) |
| Redis | 6379 | TCP |
| Qdrant (REST) | 6333 | HTTP |
| Qdrant (gRPC) | 6334 | gRPC |
| Ollama | 11434 | HTTP |

---

## Architecture

```
ayan-dj-tools/
├── domain/          # Java pur — use cases, ports, value objects (pas de Spring)
├── infra/           # Spring Boot 4 — adapters REST/WS, Spotify, Qdrant, Redis
├── music-tagger-ui/ # React 19 + Electron 40 + MUI v7 + Zustand
└── scripts/         # Scripts de démarrage et build
```

L'architecture est hexagonale (ports & adapters) avec DDD. Le module `domain` n'a aucune dépendance Spring.

---

## Écrans de l'application

### Chat — écran d'accueil (`/`)

Interface de conversation avec l'agent IA Ayan. Envoyez des messages en langage naturel pour déclencher
des scans, des enrichissements ou des suggestions de tags.

- Bulles de dialogue avec distinction Vous / Ayan
- Connexion WebSocket STOMP (fallback REST si WebSocket indisponible)
- Indicateur de connexion WS dans la toolbar

### Révision de plan (`/plan/:id`)

Affiché après la création d'un plan. La vue s'adapte au mode sélectionné :

| Mode | Vue | Comportement |
|------|-----|-------------|
| **Plan** | `PlanReviewPage` | Liste de toutes les opérations avec diff tag actuel / suggéré. Approuver/rejeter par opération, puis exécuter le plan en lot. |
| **Manuel** | `ManualModeView` | Présente les fichiers un par un. Approuvez ou rejetez chaque modification avant de passer au suivant. |
| **Auto** | `ApplyModeView` | Exécution automatique avec journal des opérations en temps réel via WebSocket. |

### Historique (`/history`) — `Ctrl+H`

Tableau de toutes les modifications de tags appliquées. Recherche par `planId`, vue détaillée des changements
par fichier (tag, valeur avant, valeur après, date).

### Statistiques (`/stats`) — `Ctrl+S`

Dashboard en 3 onglets :

| Onglet | Contenu |
|--------|---------|
| **Collection** | Nombre de pistes scannées/enrichies, distribution des genres, histogramme BPM, Camelot Wheel des tonalités, caractéristiques audio moyennes. |
| **Enrichissement** | Taux de correspondance Spotify, taux d'erreur, tags enrichis par type, enrichissement par source. |
| **Activité** | Tags appliqués par période (semaine / mois / tout), utilisation des modes, nombre de plans créés, durée moyenne. |

### Paramètres (`/settings`) — `Ctrl+,`

- URL API backend
- Activation / désactivation WebSocket
- Mode par défaut (Plan / Manuel / Auto)
- Basculement thème sombre/clair
- Langue (Français / English)
- Export / import de la configuration en JSON

### Sidebar (persistante)

Présente sur tous les écrans :

- **Liste des fichiers** — fichiers audio sélectionnés avec indicateur de tags manquants
- **Drag & drop** — glisser des fichiers audio directement dans la sidebar
- **Lecteur audio** — pré-écoute du fichier sélectionné (format `file://`, HTML5)
- **Bouton « Créer un plan »** — visible dès qu'au moins un fichier est sélectionné

---

## Modes d'opération

| Mode | Comportement |
|------|-------------|
| **Plan** | L'agent analyse tous les fichiers, propose un plan. Vous révisez et approuvez avant application. |
| **Manuel** | Fichier par fichier — approuvez ou rejetez chaque modification. |
| **Auto** | Application automatique sans confirmation. |

---

## Développement

### Backend

```bash
# Démarrer les services Docker
docker-compose up -d

# Lancer le backend
./gradlew infra:bootRun

# Tests
./gradlew test
```

### Frontend

```bash
cd music-tagger-ui
npm install

# Mode navigateur (sans Electron)
npm run dev

# Mode Electron (dev)
npm run electron:dev
```

---

## Build de distribution

```bash
# Build complet (JAR + installeur détecte la plateforme)
./scripts/build-all.sh

# Cibler une plateforme spécifique
./scripts/build-all.sh --win    # Windows NSIS
./scripts/build-all.sh --mac    # macOS DMG
./scripts/build-all.sh --linux  # AppImage + deb

# L'installeur est généré dans :
music-tagger-ui/release/
```

---

## Raccourcis clavier

| Raccourci | Action |
|-----------|--------|
| `Ctrl+O` | Ouvrir des fichiers audio |
| `Ctrl+P` | Mode Plan |
| `Ctrl+M` | Mode Manuel |
| `Ctrl+A` | Mode Auto |
| `Ctrl+H` | Historique |
| `Ctrl+S` | Statistiques |
| `Ctrl+,` | Paramètres |
| `?` | Aide raccourcis |
