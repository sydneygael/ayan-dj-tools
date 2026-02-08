# DJ Music Tagger - Main Skill

**Agent IA** : **Ayan** (tambour sacré yoruba - rythme et transmission)

## Vue d'ensemble

Application desktop pour DJ permettant l'enrichissement automatique des tags audio via Ayan, l'agent IA.

## Stack
- **Backend**: Java 25 + Spring Boot 4.0.2 + Spring AI 2.0.0-M2
- **Frontend**: Electron + Angular 21 + Material
- **IA**: Mistral via Ollama + MCP + Structured Outputs
- **Données**: PostgreSQL + Qdrant (vector store) + Spotify API
- **Build**: Gradle 9.2.1

**Référence Spring AI**: https://spring.io/blog/2026/01/23/spring-ai-2-0-0-M2-available-now

## Principes Fondamentaux

### Code Style
- **Concision extrême**: Sacrifier grammaire pour concision
- **Noms explicites**: Méthodes qui parlent d'elles-mêmes, pas de commentaires
- **Records partout**: Immutabilité, validation dans compact constructors
- **Type-safety**: Structured outputs, pas de parsing JSON manuel

### Architecture
- **Hexagonal + DDD**: Domain pur (model + service + usecase), ports/adapters, 2 couches
- **Backend-first**: Intelligence côté serveur
- **MCP-driven**: Agent contrôle workflow via @Tool functions
- **Stateless API**: Communication REST + WebSocket
- **RAG-enhanced**: Apprentissage continu via vector store

## Skills Spécialisés

Référencer ces skills selon le contexte:

### 📦 Backend
- `hexagonal-ddd.skill.md` - Architecture hexagonale + DDD simple
- `backend-java.skill.md` - Java 25, Spring Boot 4.0.2, patterns, validation
- `spring-ai.skill.md` - Spring AI 2.0, MCP, structured outputs, tools
- `spotify-integration.skill.md` - @HttpExchange, OAuth2, records Spotify
- `audio-processing.skill.md` - JAudiotagger, scan fichiers, tags manipulation
- `rag-vectordb.skill.md` - Qdrant, embeddings, similarité

### 🎨 Frontend
- `frontend-angular.skill.md` - Angular 21, Material, Electron, WebSocket

## Workflow par Type de Tâche

### Créer un service backend
1. Lire `backend-java.skill.md` pour patterns
2. Si appels externes → `spotify-integration.skill.md`
3. Si agent IA → `spring-ai.skill.md`
4. Si audio → `audio-processing.skill.md`

### Ajouter une fonction MCP
1. Lire `spring-ai.skill.md` section @Tool
2. Définir record pour structured output
3. Implémenter logique concise
4. Tester avec agent

### Créer un composant Angular
1. Lire `frontend-angular.skill.md`
2. Standalone component
3. Material components
4. Service pour backend calls

### Implémenter RAG
1. Lire `rag-vectordb.skill.md`
2. Vectoriser données Spotify
3. Similarité search
4. Intégrer dans @Tool

## Références Rapides

### Records Clés
- `MusicFileInfo` - Fichier audio local
- `SpotifyTrackData` - Données enrichies Spotify
- `TagOperation` - Modification à appliquer
- `TaggingPlan` - Plan complet mode PLAN
- `AgentQuestion` - Question agent pour clarification

### @Tool Functions
- `scanMusicFile` - Analyse fichier
- `enrichWithSpotify` - Enrichissement API
- `createPlanForFolder` - Génération plan
- `applyTags` - Application tags
- `findSimilarTracks` - Recherche RAG
- `generateHarmonicMixedPlaylist` - Playlist harmonic mix

### Classes Principales
- **AyanMusicTools** - Tous les @Tool pour l'agent
- **AyanAgentService** - Service conversationnel
- **SpotifyApiClient** - Client Spotify @HttpExchange

### API Endpoints
- `POST /api/music/scan` - Scanner dossier
- `POST /api/agent/chat` - Conversation agent
- `POST /api/plan/create` - Créer plan
- `POST /api/tags/apply` - Appliquer tags
- `WS /ws/agent` - WebSocket temps réel

## Points d'Attention

### Sécurité
- Valider chemins fichiers (pas d'accès hors dossiers autorisés)
- Credentials Spotify en env vars
- Rate limiting API Spotify

### Performance
- Batch processing pour gros dossiers
- Cache résultats Spotify
- Streaming responses agent
- Index DB sur champs recherche

### Qualité
- Tests unitaires chaque @Tool
- Validation stricte records
- Logging détaillé pour debug agent
- Gestion erreurs graceful

## Commandes Utiles

```bash
# Backend
./gradlew bootRun
docker-compose up -d

# Frontend
npm run electron:serve

# Tests
./gradlew test
npm run test
```

## Référencer depuis Claude Code

Pour utiliser un skill spécifique:
```
# Dans votre prompt à Claude Code
"Crée le service d'enrichissement Spotify en suivant spotify-integration.skill.md"
"Implémente la fonction MCP createPlanForFolder selon spring-ai.skill.md"
"Crée le composant chat Angular selon frontend-angular.skill.md"
```

---

**Version**: 1.0.0  
**Projet**: DJ Music Tagger  
**Auteur**: DJ Music Tagger Team
