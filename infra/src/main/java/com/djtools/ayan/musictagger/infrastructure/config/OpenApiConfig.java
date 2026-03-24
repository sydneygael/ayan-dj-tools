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
                                et les enrichit automatiquement via Spotify et l'IA (Ollama/Mistral).

                                **3 modes d'opération :** Plan (batch) · Manuel (fichier par fichier) · Auto (direct)

                                **WebSocket STOMP :** `ws://localhost:8000/ws` — canal `/topic/responses`

                                ---

                                ## Sécurité

                                | Aspect | Détail |
                                |--------|--------|
                                | **Accès fichiers** | Seuls les fichiers sélectionnés via le file picker Electron sont autorisés. Le backend ne fait pas de scan récursif autonome. |
                                | **CORS** | Origines autorisées : `http://localhost:5173` (Vite dev), `file://*` (Electron packagé). `allowCredentials: false`. |
                                | **Spotify** | Credentials exclusivement via `SPOTIFY_CLIENT_ID` / `SPOTIFY_CLIENT_SECRET` (variables d'environnement). Jamais exposés dans les réponses API. |
                                | **Auth HTTP** | Aucune — l'application tourne en local uniquement. Pas de token requis pour les endpoints REST. |
                                | **WebSocket** | STOMP sur SockJS, toutes origines autorisées en dev. |

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
                .addTagsItem(new Tag().name("Stats").description("Statistiques de la collection et de l'activité"));
    }
}
