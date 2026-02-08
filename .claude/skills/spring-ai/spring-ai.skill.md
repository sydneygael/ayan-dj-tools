# Spring AI 2.0.0-M2 Skill

MCP + Structured Outputs + Tool Functions

**Version**: 2.0.0-M2  
**Source**: https://spring.io/blog/2026/01/23/spring-ai-2-0-0-M2-available-now

## Principes

- **@Tool functions** pour chaque action agent
- **Structured outputs** pour toutes réponses agent
- **Records** pour schemas JSON
- **Descriptions claires** pour l'agent IA
- **Redis pour contexte conversationnel** - Historique multi-tours persistent

## Configuration

### Dependencies (build.gradle)
```gradle
ext {
    springAiVersion = '2.0.0-M2'
}

dependencies {
    implementation 'org.springframework.ai:spring-ai-ollama-spring-boot-starter'
    implementation 'com.fasterxml.jackson.core:jackson-annotations'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:${springAiVersion}"
    }
}
```

**Note M2**: Milestone 2 apporte structured outputs natifs + amélioration MCP.

### AI Config
```java
@Configuration
public class AIConfig {
    
    @Bean
    public ChatClient chatClient(
        ChatClient.Builder builder,
        DJMusicTools tools
    ) {
        return builder
            .defaultOptions(
                ChatOptionsBuilder.builder()
                    .withModel("mistral")
                    .withTemperature(0.7)
                    .build()
            )
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
          top-p: 0.9
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
```

## Conversation Memory avec Redis

**Redis stocke l'historique conversationnel** pour que l'agent se souvienne du contexte entre plusieurs tours.

### Docker Compose - Redis
```yaml
# docker-compose.yml
services:
  redis:
    image: redis:7-alpine
    container_name: dj-music-tagger-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

volumes:
  redis-data:
```

### Dependencies
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'io.lettuce:lettuce-core'
}
```

### Redis Configuration
```java
@Configuration
@EnableRedisRepositories
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
        RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Serializers JSON pour objets
        Jackson2JsonRedisSerializer<Object> serializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        
        return template;
    }
    
    @Bean
    public StringRedisTemplate stringRedisTemplate(
        RedisConnectionFactory connectionFactory
    ) {
        return new StringRedisTemplate(connectionFactory);
    }
}
```

### Conversation History Service
```java
@Service
@Slf4j
public class ConversationHistoryService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CONVERSATION_PREFIX = "conversation:";
    private static final Duration TTL = Duration.ofHours(24);
    
    public ConversationHistoryService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    public void saveMessage(String conversationId, ChatMessage message) {
        String key = CONVERSATION_PREFIX + conversationId;
        
        redisTemplate.opsForList().rightPush(key, message);
        redisTemplate.expire(key, TTL);
        
        log.debug("Message saved to conversation {}: {}", conversationId, message.role());
    }
    
    public List<ChatMessage> getHistory(String conversationId) {
        String key = CONVERSATION_PREFIX + conversationId;
        
        List<Object> messages = redisTemplate.opsForList().range(key, 0, -1);
        
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        
        return messages.stream()
            .map(obj -> (ChatMessage) obj)
            .toList();
    }
    
    public void clearHistory(String conversationId) {
        String key = CONVERSATION_PREFIX + conversationId;
        redisTemplate.delete(key);
        log.info("Conversation history cleared: {}", conversationId);
    }
    
    public int getMessageCount(String conversationId) {
        String key = CONVERSATION_PREFIX + conversationId;
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size.intValue() : 0;
    }
}

public record ChatMessage(
    String role,        // "user" | "assistant" | "system"
    String content,
    LocalDateTime timestamp,
    Map<String, Object> metadata
) {}
```

### ChatClient avec Historique
```java
@Service
@Slf4j
public class AyanAgentService {
    
    private final ChatClient chatClient;
    private final ConversationHistoryService historyService;
    
    public AyanAgentService(
        ChatClient chatClient,
        ConversationHistoryService historyService
    ) {
        this.chatClient = chatClient;
        this.historyService = historyService;
    }
    
    public String chat(String conversationId, String userMessage) {
        // 1. Sauvegarder message user
        ChatMessage userMsg = new ChatMessage(
            "user",
            userMessage,
            LocalDateTime.now(),
            Map.of()
        );
        historyService.saveMessage(conversationId, userMsg);
        
        // 2. Récupérer historique
        List<ChatMessage> history = historyService.getHistory(conversationId);
        
        // 3. Construire prompt avec contexte
        String contextualPrompt = buildPromptWithHistory(history, userMessage);
        
        // 4. Appel ChatClient
        String response = chatClient.prompt()
            .user(contextualPrompt)
            .call()
            .content();
        
        // 5. Sauvegarder réponse assistant
        ChatMessage assistantMsg = new ChatMessage(
            "assistant",
            response,
            LocalDateTime.now(),
            Map.of()
        );
        historyService.saveMessage(conversationId, assistantMsg);
        
        return response;
    }
    
    private String buildPromptWithHistory(
        List<ChatMessage> history, 
        String newMessage
    ) {
        if (history.isEmpty()) {
            return newMessage;
        }
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Historique conversation:\n\n");
        
        // Inclure derniers 10 messages pour contexte
        List<ChatMessage> recentHistory = history.stream()
            .skip(Math.max(0, history.size() - 10))
            .toList();
        
        for (ChatMessage msg : recentHistory) {
            prompt.append(String.format("[%s]: %s\n", msg.role(), msg.content()));
        }
        
        prompt.append("\nNouveau message utilisateur:\n");
        prompt.append(newMessage);
        
        return prompt.toString();
    }
}
```

### REST Controller avec Conversation
```java
@RestController
@RequestMapping("/api/agent")
@Slf4j
public class AgentController {
    
    private final AyanAgentService agentService;
    private final ConversationHistoryService historyService;
    
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String conversationId = request.conversationId() != null 
            ? request.conversationId() 
            : UUID.randomUUID().toString();
        
        String response = agentService.chat(conversationId, request.message());
        int messageCount = historyService.getMessageCount(conversationId);
        
        return new ChatResponse(
            conversationId,
            response,
            messageCount,
            LocalDateTime.now()
        );
    }
    
    @DeleteMapping("/conversations/{conversationId}")
    public void clearConversation(@PathVariable String conversationId) {
        historyService.clearHistory(conversationId);
    }
    
    @GetMapping("/conversations/{conversationId}/history")
    public List<ChatMessage> getHistory(@PathVariable String conversationId) {
        return historyService.getHistory(conversationId);
    }
}

public record ChatRequest(
    String conversationId,
    String message
) {}

public record ChatResponse(
    String conversationId,
    String message,
    int messageCount,
    LocalDateTime timestamp
) {}
```

### WebSocket pour Temps Réel (avec Historique)
```java
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
@Slf4j
public class AgentWebSocketController {
    
    private final AyanAgentService agentService;
    private final SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/chat")
    @SendTo("/topic/responses")
    public ChatResponse handleChatMessage(ChatRequest request) {
        String conversationId = request.conversationId() != null
            ? request.conversationId()
            : UUID.randomUUID().toString();
        
        // Traitement avec historique
        String response = agentService.chat(conversationId, request.message());
        
        return new ChatResponse(
            conversationId,
            response,
            0,
            LocalDateTime.now()
        );
    }
}
```

### Gestion TTL et Cleanup
```java
@Service
@Slf4j
public class ConversationCleanupService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Scheduled(fixedRate = 3600000) // Chaque heure
    public void cleanupExpiredConversations() {
        // Redis gère automatiquement les TTL
        log.info("Cleanup check - Redis handles TTL automatically");
    }
    
    // Méthode manuelle pour forcer cleanup
    public void forceCleanupOldConversations(Duration olderThan) {
        Set<String> keys = redisTemplate.keys("conversation:*");
        
        if (keys == null || keys.isEmpty()) {
            return;
        }
        
        int deleted = 0;
        for (String key : keys) {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            
            if (ttl != null && ttl < 0) {
                redisTemplate.delete(key);
                deleted++;
            }
        }
        
        log.info("Force cleanup: {} conversations deleted", deleted);
    }
}
```

### Frontend Angular - Gestion ConversationID
```typescript
// conversation.service.ts
@Injectable({ providedIn: 'root' })
export class ConversationService {
  private conversationId = signal<string | null>(null);
  private http = inject(HttpClient);
  
  initConversation() {
    // Générer nouvel ID ou récupérer depuis localStorage
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
      conversationId: this.conversationId(),
      message
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
  
  getHistory(): Observable<ChatMessage[]> {
    const id = this.conversationId();
    if (!id) return of([]);
    
    return this.http.get<ChatMessage[]>(
      `/api/agent/conversations/${id}/history`
    );
  }
}
```

## @Tool Functions Pattern

### Fonction simple
```java
@Component
public class AyanMusicTools {
    
    private final AudioTagService audioService;
    
    public AyanMusicTools(AudioTagService audioService) {
        this.audioService = audioService;
    }
    
    @Tool(
        name = "scanMusicFile",
        description = "Analyse fichier audio et retourne métadonnées actuelles"
    )
    public MusicFileInfo scanFile(
        @ToolParam(description = "Chemin absolu fichier audio") 
        String filepath
    ) {
        return audioService.readTags(filepath);
    }
}
```

### Fonction avec logique métier
```java
@Tool(
    name = "detectMissingTags",
    description = "Détecte quels tags manquants (artist, title, bpm, key, genre)"
)
public MissingTagsReport detectMissingTags(
    @ToolParam(description = "Chemin fichier") 
    String filepath
) {
    MusicFileInfo info = audioService.readTags(filepath);
    List<String> missing = new ArrayList<>();
    
    if (isBlank(info.artist())) missing.add("ARTIST");
    if (isBlank(info.title())) missing.add("TITLE");
    if (info.bpm() == null) missing.add("BPM");
    if (isBlank(info.key())) missing.add("KEY");
    if (isBlank(info.genre())) missing.add("GENRE");
    
    return new MissingTagsReport(filepath, List.copyOf(missing));
}

private boolean isBlank(String str) {
    return str == null || str.isBlank();
}
```

### Fonction avec appels externes
```java
@Tool(
    name = "enrichWithSpotify",
    description = "Recherche et enrichit métadonnées via Spotify API"
)
public SpotifyEnrichmentResult enrichWithSpotify(
    @ToolParam(description = "Chemin fichier") String filepath,
    @ToolParam(description = "Artiste approximatif") String artist,
    @ToolParam(description = "Titre approximatif") String title
) {
    try {
        // Recherche Spotify
        String query = String.format("artist:%s track:%s", artist, title);
        SpotifySearchResponse result = spotifyClient.searchTracks(query, "track", 5);
        
        if (result.tracks().items().isEmpty()) {
            return SpotifyEnrichmentResult.notFound();
        }
        
        SpotifyTrackItem match = result.tracks().items().get(0);
        SpotifyAudioFeatures features = spotifyClient.getAudioFeatures(match.id());
        
        SpotifyTrackData data = convertToInternalModel(filepath, match, features);
        repository.save(SpotifyTrackEntity.fromRecord(data));
        
        return SpotifyEnrichmentResult.success(data);
        
    } catch (Exception e) {
        log.error("Spotify enrichment failed: {}", filepath, e);
        return SpotifyEnrichmentResult.error(e.getMessage());
    }
}
```

### Fonction complexe avec orchestration
```java
@Tool(
    name = "createPlanForFolder",
    description = "Analyse dossier complet et génère plan modifications"
)
public TaggingPlan createPlan(
    @ToolParam(description = "Chemin dossier à analyser") 
    String folderPath
) {
    List<MusicFileInfo> files = audioService.scanFolder(folderPath);
    List<TagOperation> operations = new ArrayList<>();
    
    for (MusicFileInfo file : files) {
        MissingTagsReport missing = detectMissingTags(file.filepath());
        
        if (missing.hasMissingTags()) {
            TagSuggestions suggestions = suggestTags(file);
            
            operations.add(new TagOperation(
                file.filepath(),
                fileToTagMap(file),
                suggestionsToTagMap(suggestions),
                TagOperation.OperationStatus.PENDING,
                "Ready for review"
            ));
        }
    }
    
    return new TaggingPlan(
        folderPath,
        List.copyOf(operations),
        LocalDateTime.now(),
        TaggingPlan.PlanStatus.READY_FOR_REVIEW,
        files.size(),
        operations.size()
    );
}
```

### Fonction DJ avancée - Harmonic Mixing Playlist
```java
@Tool(
    name = "generateHarmonicMixedPlaylist",
    description = "Génère playlist 50 tracks mixed in key (harmonic mixing) avec même genre/sous-genre. Utilise Camelot Wheel pour transitions harmoniques."
)
public HarmonicPlaylist generateHarmonicMixedPlaylist(
    @ToolParam(description = "Genre principal (ex: Techno, House)") 
    String mainGenre,
    
    @ToolParam(description = "Sous-genre optionnel (ex: Melodic Techno, Deep House)") 
    String subGenre,
    
    @ToolParam(description = "BPM minimum") 
    Integer minBpm,
    
    @ToolParam(description = "BPM maximum") 
    Integer maxBpm,
    
    @ToolParam(description = "Energy level 0.0-1.0") 
    Double targetEnergy
) {
    // 1. Rechercher tracks dans collection via RAG
    String searchQuery = buildGenreQuery(mainGenre, subGenre, targetEnergy);
    
    List<SpotifyTrackData> candidates = vectorStore.findSimilarWithFilters(
        searchQuery,
        200, // Pool large pour sélection
        Map.of(
            "genre", mainGenre,
            "minTempo", minBpm,
            "maxTempo", maxBpm,
            "minEnergy", targetEnergy - 0.1,
            "maxEnergy", targetEnergy + 0.1
        )
    );
    
    if (candidates.size() < 50) {
        throw new IllegalStateException(
            String.format("Pas assez tracks: %d trouvés, 50 requis", candidates.size())
        );
    }
    
    // 2. Construire playlist avec harmonic mixing
    List<PlaylistTrack> playlist = buildHarmonicSequence(
        candidates,
        50,
        targetEnergy
    );
    
    // 3. Calculer stats
    PlaylistStats stats = calculateStats(playlist);
    
    return new HarmonicPlaylist(
        "Harmonic Mix - " + mainGenre,
        playlist,
        stats,
        LocalDateTime.now()
    );
}

// Camelot Wheel pour harmonic mixing
private static final Map<String, String> CAMELOT_WHEEL = Map.ofEntries(
    entry("C Major", "8B"), entry("A Minor", "8A"),
    entry("G Major", "9B"), entry("E Minor", "9A"),
    entry("D Major", "10B"), entry("B Minor", "10A"),
    entry("A Major", "11B"), entry("F# Minor", "11A"),
    entry("E Major", "12B"), entry("C# Minor", "12A"),
    entry("B Major", "1B"), entry("G# Minor", "1A"),
    entry("F# Major", "2B"), entry("D# Minor", "2A"),
    entry("Db Major", "3B"), entry("Bb Minor", "3A"),
    entry("Ab Major", "4B"), entry("F Minor", "4A"),
    entry("Eb Major", "5B"), entry("C Minor", "5A"),
    entry("Bb Major", "6B"), entry("G Minor", "6A"),
    entry("F Major", "7B"), entry("D Minor", "7A")
);

private List<PlaylistTrack> buildHarmonicSequence(
    List<SpotifyTrackData> candidates,
    int targetCount,
    double targetEnergy
) {
    // Grouper par clé Camelot
    Map<String, List<SpotifyTrackData>> byKey = candidates.stream()
        .filter(t -> getCamelotKey(t) != null)
        .collect(Collectors.groupingBy(this::getCamelotKey));
    
    List<PlaylistTrack> playlist = new ArrayList<>();
    
    // Commencer avec track random haute energy si ciblé
    String startKey = selectStartingKey(byKey, targetEnergy);
    SpotifyTrackData current = selectBestTrack(byKey.get(startKey), targetEnergy);
    
    playlist.add(new PlaylistTrack(
        current,
        1,
        startKey,
        null, // Pas de transition pour le premier
        0.0
    ));
    
    String currentKey = startKey;
    
    // Construire séquence harmonique
    for (int i = 1; i < targetCount; i++) {
        // Trouver clés compatibles (Camelot Wheel rules)
        List<String> compatibleKeys = getCompatibleKeys(currentKey);
        
        // Sélectionner prochaine clé avec variété
        String nextKey = selectNextKey(compatibleKeys, byKey, playlist);
        
        if (nextKey == null || byKey.get(nextKey).isEmpty()) {
            log.warn("Pas de tracks compatibles pour clé {}, utilise même clé", currentKey);
            nextKey = currentKey;
        }
        
        // Sélectionner meilleur track dans cette clé
        List<SpotifyTrackData> available = byKey.get(nextKey).stream()
            .filter(t -> !isAlreadyInPlaylist(t, playlist))
            .toList();
            
        if (available.isEmpty()) {
            log.warn("Clé {} épuisée, cherche alternative", nextKey);
            continue;
        }
        
        SpotifyTrackData next = selectBestTrack(available, targetEnergy);
        
        // Calculer transition quality
        String transitionType = getTransitionType(currentKey, nextKey);
        double transitionQuality = calculateTransitionQuality(
            playlist.get(i - 1).track(),
            next
        );
        
        playlist.add(new PlaylistTrack(
            next,
            i + 1,
            nextKey,
            transitionType,
            transitionQuality
        ));
        
        currentKey = nextKey;
    }
    
    return playlist;
}

private String getCamelotKey(SpotifyTrackData track) {
    String key = track.audioFeatures().key();
    String mode = track.audioFeatures().mode();
    String musicalKey = key + " " + mode;
    
    return CAMELOT_WHEEL.get(musicalKey);
}

// Règles Camelot Wheel
private List<String> getCompatibleKeys(String camelotKey) {
    // Perfect match: même clé
    // +1/-1: clé adjacente (energy shift)
    // A<->B: mode change (major<->minor)
    
    String number = camelotKey.substring(0, camelotKey.length() - 1);
    String letter = camelotKey.substring(camelotKey.length() - 1);
    int num = Integer.parseInt(number);
    
    List<String> compatible = new ArrayList<>();
    compatible.add(camelotKey); // Même clé (safest)
    
    // +1 (energy up)
    compatible.add((num % 12 + 1) + letter);
    
    // -1 (energy down)
    compatible.add((num == 1 ? 12 : num - 1) + letter);
    
    // Mode swap (major<->minor)
    String otherLetter = letter.equals("A") ? "B" : "A";
    compatible.add(number + otherLetter);
    
    return compatible;
}

private String selectNextKey(
    List<String> compatibleKeys,
    Map<String, List<SpotifyTrackData>> byKey,
    List<PlaylistTrack> playlist
) {
    // Éviter répétitions: ne pas utiliser même clé 3x de suite
    List<String> recentKeys = playlist.stream()
        .skip(Math.max(0, playlist.size() - 2))
        .map(PlaylistTrack::camelotKey)
        .toList();
    
    return compatibleKeys.stream()
        .filter(key -> byKey.containsKey(key) && !byKey.get(key).isEmpty())
        .filter(key -> !recentKeys.contains(key) || compatibleKeys.size() == 1)
        .findFirst()
        .orElse(compatibleKeys.get(0));
}

private SpotifyTrackData selectBestTrack(
    List<SpotifyTrackData> tracks,
    double targetEnergy
) {
    return tracks.stream()
        .min(Comparator.comparingDouble(t -> 
            Math.abs(t.audioFeatures().energy() - targetEnergy)
        ))
        .orElse(tracks.get(0));
}

private String getTransitionType(String fromKey, String toKey) {
    if (fromKey.equals(toKey)) return "PERFECT_MATCH";
    
    String fromNum = fromKey.substring(0, fromKey.length() - 1);
    String toNum = toKey.substring(0, toKey.length() - 1);
    String fromLetter = fromKey.substring(fromKey.length() - 1);
    String toLetter = toKey.substring(toKey.length() - 1);
    
    if (!fromLetter.equals(toLetter)) return "MODE_CHANGE";
    
    int diff = Math.abs(Integer.parseInt(toNum) - Integer.parseInt(fromNum));
    if (diff == 1 || diff == 11) return "ADJACENT_KEY";
    
    return "JUMP";
}

private double calculateTransitionQuality(
    SpotifyTrackData from,
    SpotifyTrackData to
) {
    // BPM compatibility (max ±6 BPM)
    double bpmDiff = Math.abs(from.audioFeatures().tempo() - to.audioFeatures().tempo());
    double bpmScore = bpmDiff <= 6 ? 1.0 : Math.max(0, 1.0 - ((bpmDiff - 6) / 10.0));
    
    // Energy flow
    double energyDiff = Math.abs(from.audioFeatures().energy() - to.audioFeatures().energy());
    double energyScore = Math.max(0, 1.0 - energyDiff);
    
    // Key compatibility already handled by Camelot
    double keyScore = 1.0;
    
    return (bpmScore * 0.4 + energyScore * 0.3 + keyScore * 0.3);
}

private PlaylistStats calculateStats(List<PlaylistTrack> playlist) {
    double avgBpm = playlist.stream()
        .mapToDouble(p -> p.track().audioFeatures().tempo())
        .average()
        .orElse(0);
        
    double avgEnergy = playlist.stream()
        .mapToDouble(p -> p.track().audioFeatures().energy())
        .average()
        .orElse(0);
        
    double avgTransitionQuality = playlist.stream()
        .filter(p -> p.transitionQuality() > 0)
        .mapToDouble(PlaylistTrack::transitionQuality)
        .average()
        .orElse(0);
        
    Map<String, Long> keyDistribution = playlist.stream()
        .collect(Collectors.groupingBy(
            PlaylistTrack::camelotKey,
            Collectors.counting()
        ));
        
    long perfectTransitions = playlist.stream()
        .filter(p -> "PERFECT_MATCH".equals(p.transitionType()))
        .count();
        
    return new PlaylistStats(
        playlist.size(),
        avgBpm,
        avgEnergy,
        avgTransitionQuality,
        keyDistribution,
        perfectTransitions
    );
}

// Records
public record HarmonicPlaylist(
    String name,
    List<PlaylistTrack> tracks,
    PlaylistStats stats,
    LocalDateTime createdAt
) {}

public record PlaylistTrack(
    SpotifyTrackData track,
    int position,
    String camelotKey,
    String transitionType,  // PERFECT_MATCH, ADJACENT_KEY, MODE_CHANGE, JUMP
    double transitionQuality // 0.0 - 1.0
) {}

public record PlaylistStats(
    int totalTracks,
    double avgBpm,
    double avgEnergy,
    double avgTransitionQuality,
    Map<String, Long> keyDistribution,
    long perfectTransitions
) {
    public double harmonicCompatibility() {
        return avgTransitionQuality * 100;
    }
}
```

## Structured Outputs Pattern

### Output simple
```java
@Service
public class AyanAgentService {
    
    private final ChatClient chatClient;
    
    public AyanAgentService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
    
    public TagSuggestions getSuggestions(String filepath) {
        String prompt = String.format(
            "Analyse le fichier %s et suggère tags manquants",
            filepath
        );
        
        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(TagSuggestions.class);  // Structured output automatique
    }
}
```

### Output avec contexte
```java
public TaggingPlan createStructuredPlan(String folderPath) {
    String prompt = String.format("""
        Analyse tous fichiers audio dans: %s
        
        Pour chaque:
        1. Détecte tags manquants
        2. Suggère valeurs appropriées
        3. Indique confiance
        
        Retourne plan complet.
        """, folderPath);
        
    return chatClient.prompt()
        .user(prompt)
        .call()
        .entity(TaggingPlan.class);
}
```

### Output batch
```java
public record BatchSuggestionsResponse(
    List<TagSuggestions> suggestions,
    int totalProcessed,
    int successCount,
    List<String> errors
) {}

public BatchSuggestionsResponse getBatchSuggestions(List<String> filepaths) {
    String prompt = String.format("""
        Analyse ces %d fichiers audio et suggère tags.
        
        Fichiers: %s
        
        Retourne liste complète suggestions avec stats.
        """, 
        filepaths.size(),
        String.join(", ", filepaths)
    );
    
    return chatClient.prompt()
        .user(prompt)
        .call()
        .entity(BatchSuggestionsResponse.class);
}
```

### Output avec tools combinés
```java
public record SmartEnrichmentResult(
    String filepath,
    boolean spotifyFound,
    TagSuggestions suggestions,
    List<SpotifyTrackData> similarTracks,
    String reasoning,
    double confidence
) {}

public SmartEnrichmentResult smartEnrich(String filepath) {
    String prompt = String.format("""
        Pour fichier: %s
        
        1. Utilise smartSuggestTags pour suggestions
        2. Si possible, enrichis avec Spotify
        3. Trouve tracks similaires en base
        4. Explique raisonnement
        5. Donne score confiance global
        
        Retourne résultat structuré complet.
        """, filepath);
        
    // Agent appelle tools nécessaires ET retourne structured output
    return chatClient.prompt()
        .user(prompt)
        .call()
        .entity(SmartEnrichmentResult.class);
}
```

## Records avec Validation pour Schema

### Annotations Jackson
```java
public record TagSuggestions(
    @JsonProperty(required = true)
    String filepath,
    
    @JsonPropertyDescription("Artist name extracted from filename or metadata")
    String suggestedArtist,
    
    @JsonPropertyDescription("Track title")
    String suggestedTitle,
    
    @Min(60) @Max(200)
    Integer suggestedBpm,
    
    @Pattern(regexp = "^[A-G](#|b)? (Major|Minor)$")
    String suggestedKey,
    
    @Min(0) @Max(1)
    double confidence
) {}
```

### JSON Schema généré automatiquement
```json
{
  "type": "object",
  "properties": {
    "filepath": { 
      "type": "string" 
    },
    "suggestedArtist": { 
      "type": "string",
      "description": "Artist name extracted from filename or metadata"
    },
    "suggestedBpm": {
      "type": "integer",
      "minimum": 60,
      "maximum": 200
    },
    "suggestedKey": {
      "type": "string",
      "pattern": "^[A-G](#|b)? (Major|Minor)$"
    },
    "confidence": {
      "type": "number",
      "minimum": 0,
      "maximum": 1
    }
  },
  "required": ["filepath"]
}
```

## Gestion Questions Agent (Mode PLAN)

### Record question
```java
public record AgentQuestion(
    String questionId,
    String filepath,
    QuestionType type,
    String question,
    List<String> options,
    String context,
    double currentConfidence
) {
    public enum QuestionType {
        MULTIPLE_CHOICE,
        PREFERENCE,
        CONFIRMATION
    }
}
```

### Tool pour poser question
```java
@Tool(
    name = "askUserForClarification",
    description = "Pose question utilisateur quand confiance < 70% ou multiple options"
)
public AgentQuestionResponse askUser(
    @ToolParam(description = "Question à poser") String question,
    @ToolParam(description = "Options possibles") List<String> options,
    @ToolParam(description = "Contexte additionnel") String context
) {
    String questionId = UUID.randomUUID().toString();
    
    AgentQuestion agentQuestion = new AgentQuestion(
        questionId,
        null,
        AgentQuestion.QuestionType.MULTIPLE_CHOICE,
        question,
        options,
        context,
        0.5
    );
    
    // Publier question via WebSocket pour UI
    questionPublisher.publish(agentQuestion);
    
    // Attendre réponse utilisateur (via WebSocket ou polling)
    return waitForUserResponse(questionId);
}
```

### Service avec gestion questions
```java
@Service
public class PlanService {
    
    private final ChatClient chatClient;
    private final QuestionService questionService;
    
    public TaggingPlan createPlanWithQuestions(String folderPath) {
        String prompt = String.format("""
            Analyse dossier: %s
            
            Si confiance < 70%% ou multiple options Spotify:
            - Utilise askUserForClarification
            - Attends réponse avant continuer
            
            Génère plan complet une fois clarifications obtenues.
            """, folderPath);
            
        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(TaggingPlan.class);
    }
}
```

## Streaming Responses

### Pour mode APPLY (feedback temps réel)
```java
public Flux<TagOperationResult> applyPlanStreaming(UUID planId) {
    TaggingPlan plan = planRepository.findById(planId)
        .orElseThrow();
        
    String prompt = String.format("""
        Exécute plan %s.
        
        Pour chaque opération:
        1. Applique tags
        2. Stream résultat immédiatement
        
        Continue jusqu'à fin.
        """, planId);
        
    return chatClient.prompt()
        .user(prompt)
        .stream()
        .content()
        .map(this::parseOperationResult);
}
```

## Error Handling

### Try-catch dans tools
```java
@Tool(name = "applyTags", description = "Applique tags à fichier")
public TagApplicationResult applyTags(
    String filepath,
    Map<String, String> tags
) {
    try {
        validateFilePath(filepath);
        validateTags(tags);
        
        audioService.writeTags(filepath, tags);
        historyService.recordSuccess(filepath, tags);
        
        return TagApplicationResult.success(filepath);
        
    } catch (IllegalArgumentException e) {
        log.warn("Validation failed: {}", filepath, e);
        return TagApplicationResult.validationError(e.getMessage());
        
    } catch (IOException e) {
        log.error("IO error applying tags: {}", filepath, e);
        return TagApplicationResult.ioError(e.getMessage());
        
    } catch (Exception e) {
        log.error("Unexpected error: {}", filepath, e);
        return TagApplicationResult.unknownError(e.getMessage());
    }
}

public record TagApplicationResult(
    String filepath,
    ResultStatus status,
    String errorMessage
) {
    public enum ResultStatus { SUCCESS, VALIDATION_ERROR, IO_ERROR, UNKNOWN_ERROR }
    
    public static TagApplicationResult success(String filepath) {
        return new TagApplicationResult(filepath, ResultStatus.SUCCESS, null);
    }
    
    public static TagApplicationResult validationError(String message) {
        return new TagApplicationResult(null, ResultStatus.VALIDATION_ERROR, message);
    }
}
```

## Testing Tools

### Mock ChatClient
```java
@ExtendWith(MockitoExtension.class)
class DJAgentServiceTest {
    
    @Mock
    private ChatClient chatClient;
    
    @Mock
    private ChatClient.ChatClientRequest.CallResponseSpec callResponseSpec;
    
    @InjectMocks
    private DJAgentService service;
    
    @Test
    void getSuggestions_shouldReturnStructuredOutput() {
        // Given
        String filepath = "/music/test.mp3";
        TagSuggestions expected = TagSuggestions.builder()
            .filepath(filepath)
            .suggestedArtist("Artist")
            .build();
            
        when(chatClient.prompt()).thenReturn(mock(ChatClient.ChatClientRequestSpec.class));
        when(callResponseSpec.entity(TagSuggestions.class)).thenReturn(expected);
        
        // When
        TagSuggestions result = service.getSuggestions(filepath);
        
        // Then
        assertEquals(expected, result);
    }
}
```

### Test intégration avec Ollama
```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.ai.ollama.base-url=http://localhost:11434"
})
class DJMusicToolsIntegrationTest {
    
    @Autowired
    private DJMusicTools tools;
    
    @Test
    void scanMusicFile_shouldReturnValidInfo() {
        // Given
        String filepath = "src/test/resources/test.mp3";
        
        // When
        MusicFileInfo result = tools.scanFile(filepath);
        
        // Then
        assertNotNull(result);
        assertEquals(filepath, result.filepath());
    }
}
```

## Checklist

- [ ] @Tool pour chaque action agent
- [ ] Descriptions claires et concises
- [ ] @ToolParam pour tous paramètres
- [ ] Structured outputs pour réponses
- [ ] Records avec validation Jackson
- [ ] **Redis configuré pour contexte conversationnel**
- [ ] **ConversationID géré frontend/backend**
- [ ] **Historique persisté avec TTL 24h**
- [ ] Error handling dans tools
- [ ] Logging approprié
- [ ] Tests tools individuellement
- [ ] Questions agent si confiance < 70%
