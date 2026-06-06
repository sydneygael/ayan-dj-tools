package com.djtools.ayan.musictagger.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ayan DJ Tools API")
                        .version("0.1.0")
                        .description("""
                                ## Agent IA d'enrichissement de tags audio pour DJs

                                Ayan scanne vos fichiers audio, détecte les tags manquants,
                                et les enrichit automatiquement via Spotify et l'IA locale (Ollama/Mistral).

                                **3 modes d'opération :** Plan (batch) · Manuel (fichier par fichier) · Auto (direct)

                                **Streaming HTTP (SSE) :** `POST /api/agent/chat/stream`

                                ---

                                ## Modèle IA — Mistral via Ollama (SLM local, pas un LLM cloud)

                                L'agent Ayan utilise **Mistral 7B** exécuté localement via **Ollama**, \
                                et non un LLM cloud (GPT-4, Claude, Gemini, etc.). \
                                C'est un **SLM (Small Language Model)** qui tourne entièrement sur votre machine.

                                ### Pourquoi ce choix ?

                                | Critère | LLM cloud (GPT-4, Claude…) | Mistral local (Ollama) |
                                |---------|---------------------------|------------------------|
                                | **Confidentialité** | Données envoyées à des serveurs tiers | Aucune donnée ne quitte la machine |
                                | **Offline** | Nécessite Internet | Fonctionne sans connexion |
                                | **Coût** | Facturation à l'usage (tokens) | Zéro coût d'inférence |
                                | **Latence** | Dépend du réseau | Locale, prédictible |
                                | **Données sensibles** | Risque d'exposition de chemins/tags perso | Traitement 100 % local |

                                > Pour un outil desktop de gestion de bibliothèque musicale personnelle, \
                                > la confidentialité et le fonctionnement hors-ligne priment sur la puissance brute du modèle. \
                                > Les tâches de l'agent (lire des tags, appeler des outils, formater des réponses) \
                                > ne requièrent pas un modèle frontier.

                                ### Stack IA complète

                                | Composant | Modèle | Rôle |
                                |-----------|--------|------|
                                | **Agent conversationnel** | `mistral` (Ollama) | Dialogue, orchestration des @Tool, suggestions |
                                | **Embeddings RAG** | `nomic-embed-text` (Ollama) | Vectorisation des tracks pour Qdrant |
                                | **Vector Store** | Qdrant | Recherche sémantique, playlist, suggestions similaires |
                                | **Enrichissement** | Spotify API | Métadonnées officielles (BPM, tonalité, genres, popularité) |

                                Configuration : `spring.ai.ollama.chat.options.model=mistral` \
                                / `spring.ai.ollama.embedding.options.model=nomic-embed-text`

                                Pour utiliser un autre modèle Ollama (ex: `llama3.2`, `phi4`, `gemma3`), \
                                modifier `application.yml` et lancer `docker exec -it dj-tagger-ollama ollama pull <model>`.

                                ---

                                ## Sécurité

                                | Aspect | Détail |
                                |--------|--------|
                                | **Accès fichiers** | Seuls les fichiers sélectionnés via le file picker Electron sont autorisés. Le backend ne fait pas de scan récursif autonome. |
                                | **CORS** | Origines autorisées : `http://localhost:5173` (Vite dev), `file://*` (Electron packagé). `allowCredentials: false`. |
                                | **Spotify** | Credentials exclusivement via `SPOTIFY_CLIENT_ID` / `SPOTIFY_CLIENT_SECRET` (variables d'environnement). Jamais exposés dans les réponses API. |
                                | **Auth HTTP** | Aucune — l'application tourne en local uniquement. Pas de token requis pour les endpoints REST. |
                                | **Streaming** | SSE HTTP via `/api/agent/chat/stream` (aucun WebSocket requis). |

                                ---

                                ## Architecture hexagonale

                                <img src="/architecture.png" style="max-width:100%;border-radius:8px;margin-top:8px"/>
                                """)
                        .contact(new Contact()
                                .name("Ayan DJ Tools")
                                .url("https://github.com/your-org/ayan-dj-tools"))
                )
                .components(new Components()
                        .addSecuritySchemes("spotify-env", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Spotify-Token")
                                .description(
                                        "Credentials Spotify injectés côté serveur via variables d'environnement " +
                                        "`SPOTIFY_CLIENT_ID` et `SPOTIFY_CLIENT_SECRET`. " +
                                        "Le client HTTP ne fournit pas de token — l'authentification est gérée " +
                                        "par `SpotifyTokenService` (OAuth2 client credentials, TTL 3600s)."
                                )
                        )
                )
                .addTagsItem(new Tag().name("Agent").description("Chat IA Ayan — enrichissement conversationnel"))
                .addTagsItem(new Tag().name("Plan").description("Gestion des plans de tagging (PLAN / MANUEL / AUTO)"))
                .addTagsItem(new Tag().name("Tags").description("Application directe de tags sur fichiers audio"))
                .addTagsItem(new Tag().name("RAG").description("Recherche sémantique par similarité (Vector Store Qdrant)"))
                .addTagsItem(new Tag().name("Stats").description("Statistiques de la collection et de l'activité"))
                .addTagsItem(new Tag().name("Library").description("Bibliothèque des fichiers audio scannés (persistance PostgreSQL)"))
                .addTagsItem(new Tag().name("Files").description("Navigation système de fichiers, analyse et enrichissement de fichiers audio"))
                .addTagsItem(new Tag().name("Playlist").description("Génération de playlists (loop-mixing, harmonique Camelot, thématique) et export M3U"))
                .addTagsItem(new Tag().name("Settings").description("Configuration des clés API tierces (Soundcharts, Spotify, Tavily)"))
                .addTagsItem(new Tag().name("Spotify").description("Diagnostic de connectivité et quota de l'API Spotify"));
    }
}
