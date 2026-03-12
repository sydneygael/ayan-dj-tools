# Spring AI 2.0.0-M2 — Reference Rapide

## Inner Types ChatClient (ATTENTION aux noms)

| Type | Usage | PAS confondre avec |
|------|-------|--------------------|
| `ChatClient.Builder` | Injection pour construire ChatClient | — |
| `ChatClient.ChatClientRequestSpec` | Retour de `chatClient.prompt()` | ~~PromptSpec~~, ~~UserSpec~~ |
| `ChatClient.CallResponseSpec` | Retour de `.call()` | ~~CallSpec~~ |

```java
// Chain complete
ChatClient.ChatClientRequestSpec req = chatClient.prompt();
req.user("message");
ChatClient.CallResponseSpec resp = req.call();
String content = resp.content();                    // texte brut
MyRecord entity = resp.entity(MyRecord.class);      // structured output
```

## @Tool / @ToolParam Signatures

```java
@Tool(name = "toolName", description = "Description pour l'agent IA")
public ReturnType methodName(
    @ToolParam(description = "Description param") String param1,
    @ToolParam(description = "Description param") int param2
) { ... }
```

- Retour : record, String, List, Map — tout serialisable JSON
- Descriptions : en francais, claires pour l'agent IA
- Enregistrement : `builder.defaultTools(toolsBean)` dans AIConfig

## ChatClient Builder Chain

```java
ChatClient chatClient = builder
    .defaultOptions(ChatOptionsBuilder.builder()
        .withModel("mistral")
        .withTemperature(0.7)
        .build())
    .defaultSystem("Tu es Ayan, assistant DJ...")
    .defaultTools(ayanMusicTools)
    .build();
```

## Redis — Cles et TTL

| Prefixe | TTL | Usage |
|---------|-----|-------|
| `conversation:{id}` | 24h | Historique messages (List) |
| `plan:{id}` | 48h | TaggingPlan (Value) |
| `tagging-history:{planId}` | 7 jours | Historique operations (List) |

## Config Keys

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: mistral
          temperature: 0.7
      embedding:
        options:
          model: nomic-embed-text
  data:
    redis:
      host: localhost
      port: 6379
```

## STOMP Endpoints et Destinations

| Element | Valeur |
|---------|--------|
| Endpoint WebSocket | `/ws` (avec SockJS) |
| Prefix app | `/app` |
| Broker | `/topic` |
| Envoyer message | `/app/chat` (`@MessageMapping("/chat")`) |
| Recevoir reponses | `/topic/responses` (`@SendTo`) |
| CORS autorise | `localhost:4200` |

## 10 @Tool Functions Existantes

| Nom | Description |
|-----|-------------|
| `scanMusicFile` | Analyse fichier audio, retourne metadonnees |
| `detectMissingTags` | Detecte tags manquants |
| `suggestTagsFromFilename` | Suggestions depuis nom fichier |
| `enrichWithSpotify` | Enrichit via Spotify + auto-vectorise |
| `createPlanForFiles` | Cree TaggingPlan pour liste fichiers |
| `applyTagsPlan` | Applique plan de tagging |
| `previewTagUpdate` | Preview changements avant application |
| `getTaggingHistory` | Historique operations tagging |
| `findSimilarTracks` | Recherche tracks similaires (RAG) |
| `smartSuggestTags` | Suggestions combinees Spotify + RAG |

## Gotchas Version-Specifiques

- Spring AI 2.0.0-M2 est milestone → repo Spring Milestones requis dans build.gradle.kts
- `ChatClient.ChatClientRequestSpec` (pas `ChatClientRequest`)
- `Document.score` est final — utiliser `Builder.score()` pour setter
- `EmbeddingModel.embed(Document)` existe, mais PAS `embed(List<Document>)`
- Jackson 3 dans Spring Boot 4 : `tools.jackson.databind.ObjectMapper`
- Exclure Ollama autoconfig dans ITs : `@EnableAutoConfiguration(exclude = {OllamaApiAutoConfiguration.class, ...})`
