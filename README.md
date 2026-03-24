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
| Backend Spring Boot | 8080 | HTTP / WebSocket STOMP |
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
