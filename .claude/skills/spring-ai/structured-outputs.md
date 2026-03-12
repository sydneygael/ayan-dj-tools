# Spring AI — Structured Outputs

## Output simple
```java
public TagSuggestions getSuggestions(String filepath) {
    return chatClient.prompt()
        .user(String.format("Analyse le fichier %s et suggere tags manquants", filepath))
        .call()
        .entity(TagSuggestions.class);  // Structured output automatique
}
```

## Output avec contexte
```java
public TaggingPlan createStructuredPlan(String folderPath) {
    return chatClient.prompt()
        .user(String.format("""
            Analyse tous fichiers audio dans: %s
            Pour chaque: 1. Detecte tags manquants 2. Suggere valeurs 3. Indique confiance
            Retourne plan complet.
            """, folderPath))
        .call()
        .entity(TaggingPlan.class);
}
```

## Output batch
```java
public record BatchSuggestionsResponse(
    List<TagSuggestions> suggestions, int totalProcessed,
    int successCount, List<String> errors
) {}

public BatchSuggestionsResponse getBatchSuggestions(List<String> filepaths) {
    return chatClient.prompt()
        .user(String.format("Analyse ces %d fichiers: %s", filepaths.size(), String.join(", ", filepaths)))
        .call()
        .entity(BatchSuggestionsResponse.class);
}
```

## Records avec Validation pour Schema

```java
public record TagSuggestions(
    @JsonProperty(required = true) String filepath,
    @JsonPropertyDescription("Artist name extracted from filename or metadata") String suggestedArtist,
    @JsonPropertyDescription("Track title") String suggestedTitle,
    @Min(60) @Max(200) Integer suggestedBpm,
    @Pattern(regexp = "^[A-G](#|b)? (Major|Minor)$") String suggestedKey,
    @Min(0) @Max(1) double confidence
) {}
```

### JSON Schema genere automatiquement
```json
{
  "type": "object",
  "properties": {
    "filepath": { "type": "string" },
    "suggestedArtist": { "type": "string", "description": "Artist name extracted from filename or metadata" },
    "suggestedBpm": { "type": "integer", "minimum": 60, "maximum": 200 },
    "suggestedKey": { "type": "string", "pattern": "^[A-G](#|b)? (Major|Minor)$" },
    "confidence": { "type": "number", "minimum": 0, "maximum": 1 }
  },
  "required": ["filepath"]
}
```

## Streaming Responses (mode APPLY)

```java
public Flux<TagOperationResult> applyPlanStreaming(UUID planId) {
    TaggingPlan plan = planRepository.findById(planId).orElseThrow();
    return chatClient.prompt()
        .user(String.format("Execute plan %s. Pour chaque operation: applique tags, stream resultat.", planId))
        .stream()
        .content()
        .map(this::parseOperationResult);
}
```

## Frontend Angular - Gestion ConversationID

```typescript
@Injectable({ providedIn: 'root' })
export class ConversationService {
  private conversationId = signal<string | null>(null);
  private http = inject(HttpClient);

  initConversation() {
    const savedId = localStorage.getItem('conversationId');
    if (savedId) {
      this.conversationId.set(savedId);
    } else {
      const newId = crypto.randomUUID();
      this.conversationId.set(newId);
      localStorage.setItem('conversationId', newId);
    }
  }

  sendMessage(message: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>('/api/agent/chat', {
      conversationId: this.conversationId(), message
    });
  }

  clearConversation() {
    const id = this.conversationId();
    if (!id) return;
    this.http.delete(`/api/agent/conversations/${id}`).subscribe(() => {
      localStorage.removeItem('conversationId');
      this.conversationId.set(null);
      this.initConversation();
    });
  }
}
```
