# Ayan DJ Tools

Application desktop pour DJs — enrichissement automatique des tags audio via IA (agent Ayan).

## Prérequis

| Outil | Version minimale |
|-------|-----------------|
| Java (JDK) | 21+ |
| Node.js | 22+ (frontend Angular) |
| Flutter SDK | 3.22+ (frontend Desktop) |
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
├── domain/                  # Java pur — use cases, ports, value objects (pas de Spring)
├── infra/                   # Spring Boot 4 — adapters REST/WS, Spotify, Qdrant, Redis
├── ayan_dj_tools_flutter/   # Flutter Desktop — Windows, macOS, Linux
└── ayan_dj_tools_web/       # Angular 21 — interface web (signals, Material 3, STOMP)
```

L'architecture est hexagonale (ports & adapters) avec DDD. Le module `domain` n'a aucune dépendance Spring.
Le frontend Flutter Desktop communique avec le backend via HTTP (Dio) et WebSocket STOMP (`stomp_dart_client`).
Le frontend Angular communique via HTTP (HttpClient) et WebSocket STOMP (`@stomp/stompjs`).

---

## Écrans de l'application

### Chat — écran d'accueil (`/`)

Interface de conversation avec l'agent IA Ayan. Envoyez des messages en langage naturel pour déclencher
des scans, des enrichissements ou des suggestions de tags.

### Plan (`/plan/:id`)

Révision du plan de tagging proposé par l'agent. Trois modes d'affichage selon le mode d'opération :

- **Plan** — liste complète des opérations à approuver/rejeter, puis exécution en lot
- **Manuel** — fichier par fichier, avec progression STOMP en temps réel
- **Auto** — exécution automatique avec log de progression animé

### Historique (`/history`) — `Ctrl+H`

Recherche par ID de plan. Affiche la liste des fichiers modifiés avec le diff complet des tags
(valeur avant / valeur après, avec indicateur succès/erreur par fichier).

### Playlist (`/playlist`) — `Ctrl+L`

Génération de playlist à partir de la bibliothèque enrichie. Filtres : BPM min/max, genre.
Chaque piste affiche artiste, titre, album, genres, BPM, tonalité et durée.

### Statistiques (`/stats`) — `Ctrl+S`

Dashboard en 3 onglets :

| Onglet | Contenu |
|--------|---------|
| **Collection** | Nombre de pistes scannées/enrichies, distribution des genres (donut), histogramme BPM, Camelot Wheel des tonalités. |
| **Enrichissement** | Taux de correspondance Spotify, taux d'erreur, tags enrichis par type. |
| **Activité** | Tags appliqués par période (semaine / mois / tout), utilisation des modes, nombre de plans créés. |

### Paramètres (`/settings`) — `Ctrl+,`

- URL API backend
- Activation / désactivation WebSocket
- Mode par défaut (Plan / Manuel / Auto)
- Basculement thème sombre/clair
- Langue (Français / English)
- Export / import de la configuration en JSON

### Sidebar (persistante)

Présente sur tous les écrans :

- **Liste des fichiers** — fichiers audio sélectionnés (MP3, FLAC, WAV, AIFF, M4A, OGG)
- **Drag & drop** — glisser des fichiers audio directement dans la sidebar
- **Lecteur audio** — pré-écoute du fichier sélectionné (lecture native via `audioplayers`)
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

### Frontend Angular Web

```bash
cd ayan_dj_tools_web

# Installer les dépendances
npm install

# Lancer en mode dev (localhost:4200)
npm start

# Build de production
npm run build
```

### Frontend Flutter

```bash
cd ayan_dj_tools_flutter

# Installer les dépendances
flutter pub get

# Lancer en mode dev (Windows)
flutter run -d windows

# Lancer en mode dev (macOS / Linux)
flutter run -d macos
flutter run -d linux
```

> **Windows** : le mode développeur doit être activé (Paramètres → Développeurs → Mode développeur).
> **Build Windows** : Visual Studio avec la charge de travail « Développement Desktop en C++ » est requis
> (CMake + Windows 10 SDK).

---

## Build de distribution

### Backend

```bash
./gradlew infra:bootJar
# Génère : infra/build/libs/infra-*.jar
```

### Frontend Flutter

```bash
cd ayan_dj_tools_flutter

# Windows
flutter build windows --release
# Sortie : build/windows/x64/runner/Release/ayan_dj_tools.exe

# macOS
flutter build macos --release

# Linux
flutter build linux --release
```

---

## Raccourcis clavier

| Raccourci | Action |
|-----------|--------|
| `Ctrl+P` | Chat (accueil) |
| `Ctrl+H` | Historique |
| `Ctrl+S` | Statistiques |
| `Ctrl+L` | Playlist |
| `Ctrl+,` | Paramètres |
| Icône `⌨` | Aide raccourcis |
