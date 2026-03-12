---
name: spring-ai
description: Spring AI 2.0.0-M2 patterns. @Tool functions, structured outputs, ChatClient, Redis conversation history, WebSocket STOMP, Camelot Wheel harmonic mixing. Utiliser pour tout code lie a l'agent IA Ayan.
user-invocable: false
---

# Spring AI 2.0.0-M2 Skill

MCP + Structured Outputs + Tool Functions

**Version**: 2.0.0-M2

> Harmonic mixing & Camelot Wheel : voir [harmonic-mixing.md](./harmonic-mixing.md)
> Structured outputs patterns : voir [structured-outputs.md](./structured-outputs.md)
> Reference rapide API : voir [reference.md](./reference.md)
> Exemples complets : voir [examples.md](./examples.md)

## Principes

- **@Tool functions** pour chaque action agent
- **Structured outputs** pour toutes reponses agent
- **Records** pour schemas JSON
- **Descriptions claires** pour l'agent IA
- **Redis pour contexte conversationnel** - Historique multi-tours persistent

## Configuration

### Dependencies (build.gradle)
```gradle
ext { springAiVersion = '2.0.0-M2' }

dependencies {
    implementation 'org.springframework.ai:spring-ai-ollama-spring-boot-starter'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'io.lettuce:lettuce-core'
}

dependencyManagement {
    imports { mavenBom "org.springframework.ai:spring-ai-bom:${springAiVersion}" }
}
```

### AI Config
```java
@Configuration
public class AIConfig {
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, DJMusicTools tools) {
        return builder
            .defaultOptions(ChatOptionsBuilder.builder()
                .withModel("mistral").withTemperature(0.7).build())
            .defaultTools(tools)
            .build();
    }
}
```

### application.yml
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: mistral
          temperature: 0.7
  data:
    redis:
      host: localhost
      port: 6379
```

## Conversation Memory avec Redis

```java
@Configuration
@EnableRedisRepositories
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        return template;
    }
}

@Service
@Slf4j
public class ConversationHistoryService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String PREFIX = "conversation:";
    private static final Duration TTL = Duration.ofHours(24);

    public void saveMessage(String conversationId, ChatMessage message) {
        String key = PREFIX + conversationId;
        redisTemplate.opsForList().rightPush(key, message);
        redisTemplate.expire(key, TTL);
    }

    public List<ChatMessage> getHistory(String conversationId) {
        List<Object> messages = redisTemplate.opsForList().range(PREFIX + conversationId, 0, -1);
        if (messages == null || messages.isEmpty()) return List.of();
        return messages.stream().map(obj -> (ChatMessage) obj).toList();
    }

    public void clearHistory(String conversationId) {
        redisTemplate.delete(PREFIX + conversationId);
    }
}

public record ChatMessage(String role, String content, LocalDateTime timestamp, Map<String, Object> metadata) {}
```

### ChatClient avec Historique
```java
@Service
@Slf4j
public class AyanAgentService {
    private final ChatClient chatClient;
    private final ConversationHistoryService historyService;

    public String chat(String conversationId, String userMessage) {
        historyService.saveMessage(conversationId,
            new ChatMessage("user", userMessage, LocalDateTime.now(), Map.of()));

        List<ChatMessage> history = historyService.getHistory(conversationId);
        String contextualPrompt = buildPromptWithHistory(history, userMessage);

        String response = chatClient.prompt().user(contextualPrompt).call().content();

        historyService.saveMessage(conversationId,
            new ChatMessage("assistant", response, LocalDateTime.now(), Map.of()));
        return response;
    }

    private String buildPromptWithHistory(List<ChatMessage> history, String newMessage) {
        if (history.isEmpty()) return newMessage;
        StringBuilder prompt = new StringBuilder("Historique conversation:\n\n");
        history.stream().skip(Math.max(0, history.size() - 10))
            .forEach(msg -> prompt.append(String.format("[%s]: %s\n", msg.role(), msg.content())));
        prompt.append("\nNouveau message utilisateur:\n").append(newMessage);
        return prompt.toString();
    }
}
```

### REST Controller + WebSocket
```java
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AyanAgentService agentService;

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String conversationId = request.conversationId() != null
            ? request.conversationId() : UUID.randomUUID().toString();
        String response = agentService.chat(conversationId, request.message());
        return new ChatResponse(conversationId, response, 0, LocalDateTime.now());
    }

    @DeleteMapping("/conversations/{id}")
    public void clearConversation(@PathVariable String id) {
        historyService.clearHistory(id);
    }
}

public record ChatRequest(String conversationId, String message) {}
public record ChatResponse(String conversationId, String message, int messageCount, LocalDateTime timestamp) {}

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }
}

@Controller
public class AgentWebSocketController {
    @MessageMapping("/chat")
    @SendTo("/topic/responses")
    public ChatResponse handleChatMessage(ChatRequest request) {
        String cid = request.conversationId() != null ? request.conversationId() : UUID.randomUUID().toString();
        return new ChatResponse(cid, agentService.chat(cid, request.message()), 0, LocalDateTime.now());
    }
}
```

## @Tool Functions Pattern

```java
@Component
public class AyanMusicTools {
    private final AudioTagService audioService;

    @Tool(name = "scanMusicFile", description = "Analyse fichier audio et retourne metadonnees actuelles")
    public MusicFileInfo scanFile(@ToolParam(description = "Chemin absolu fichier audio") String filepath) {
        return audioService.readTags(filepath);
    }

    @Tool(name = "detectMissingTags", description = "Detecte quels tags manquants")
    public MissingTagsReport detectMissingTags(@ToolParam(description = "Chemin fichier") String filepath) {
        MusicFileInfo info = audioService.readTags(filepath);
        List<String> missing = new ArrayList<>();
        if (isBlank(info.artist())) missing.add("ARTIST");
        if (isBlank(info.title())) missing.add("TITLE");
        if (info.bpm() == null) missing.add("BPM");
        return new MissingTagsReport(filepath, List.copyOf(missing));
    }

    @Tool(name = "enrichWithSpotify", description = "Recherche et enrichit metadonnees via Spotify API")
    public SpotifyEnrichmentResult enrichWithSpotify(
        @ToolParam(description = "Chemin fichier") String filepath,
        @ToolParam(description = "Artiste") String artist,
        @ToolParam(description = "Titre") String title
    ) {
        try {
            String query = String.format("artist:%s track:%s", artist, title);
            SpotifySearchResponse result = spotifyClient.searchTracks(query, "track", 5);
            if (result.tracks().items().isEmpty()) return SpotifyEnrichmentResult.notFound();
            SpotifyTrackItem match = result.tracks().items().get(0);
            SpotifyAudioFeatures features = spotifyClient.getAudioFeatures(match.id());
            return SpotifyEnrichmentResult.success(convertToInternalModel(filepath, match, features));
        } catch (Exception e) {
            return SpotifyEnrichmentResult.error(e.getMessage());
        }
    }
}
```

## Gestion Questions Agent (Mode PLAN)

```java
public record AgentQuestion(
    String questionId, String filepath, QuestionType type,
    String question, List<String> options, String context, double currentConfidence
) {
    public enum QuestionType { MULTIPLE_CHOICE, PREFERENCE, CONFIRMATION }
}

@Tool(name = "askUserForClarification", description = "Pose question quand confiance < 70%")
public AgentQuestionResponse askUser(
    @ToolParam(description = "Question") String question,
    @ToolParam(description = "Options") List<String> options,
    @ToolParam(description = "Contexte") String context
) {
    AgentQuestion q = new AgentQuestion(UUID.randomUUID().toString(), null,
        AgentQuestion.QuestionType.MULTIPLE_CHOICE, question, options, context, 0.5);
    questionPublisher.publish(q);
    return waitForUserResponse(q.questionId());
}
```

## Error Handling

```java
@Tool(name = "applyTags", description = "Applique tags a fichier")
public TagApplicationResult applyTags(String filepath, Map<String, String> tags) {
    try {
        validateFilePath(filepath);
        audioService.writeTags(filepath, tags);
        return TagApplicationResult.success(filepath);
    } catch (IllegalArgumentException e) {
        return TagApplicationResult.validationError(e.getMessage());
    } catch (IOException e) {
        return TagApplicationResult.ioError(e.getMessage());
    }
}

public record TagApplicationResult(String filepath, ResultStatus status, String errorMessage) {
    public enum ResultStatus { SUCCESS, VALIDATION_ERROR, IO_ERROR, UNKNOWN_ERROR }
    public static TagApplicationResult success(String fp) { return new TagApplicationResult(fp, ResultStatus.SUCCESS, null); }
    public static TagApplicationResult validationError(String msg) { return new TagApplicationResult(null, ResultStatus.VALIDATION_ERROR, msg); }
    public static TagApplicationResult ioError(String msg) { return new TagApplicationResult(null, ResultStatus.IO_ERROR, msg); }
}
```

## Testing

```java
@ExtendWith(MockitoExtension.class)
class DJAgentServiceTest {
    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequest.CallResponseSpec callResponseSpec;
    @InjectMocks private DJAgentService service;

    @Test
    void getSuggestions_shouldReturnStructuredOutput() {
        TagSuggestions expected = TagSuggestions.builder().filepath("/music/test.mp3").build();
        when(chatClient.prompt()).thenReturn(mock(ChatClient.ChatClientRequestSpec.class));
        when(callResponseSpec.entity(TagSuggestions.class)).thenReturn(expected);
        assertEquals(expected, service.getSuggestions("/music/test.mp3"));
    }
}
```

## Checklist

- [ ] @Tool pour chaque action agent
- [ ] Descriptions claires et concises
- [ ] @ToolParam pour tous parametres
- [ ] Structured outputs pour reponses
- [ ] Records avec validation Jackson
- [ ] Redis configure pour contexte conversationnel
- [ ] ConversationID gere frontend/backend
- [ ] Historique persiste avec TTL 24h
- [ ] Error handling dans tools
- [ ] Tests tools individuellement
- [ ] Questions agent si confiance < 70%
