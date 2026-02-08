# Spotify Integration Skill

@HttpExchange + OAuth2 + Records

## Principes

- **@HttpExchange** pour client déclaratif
- **Records** pour toutes réponses API
- **OAuth2** pour authentification
- **Cache** pour rate limiting

## Configuration

### Dependencies
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
}
```

### Spotify Config
```java
@Configuration
public class SpotifyConfig {
    
    @Value("${spotify.client-id}")
    private String clientId;
    
    @Value("${spotify.client-secret}")
    private String clientSecret;
    
    @Bean
    public SpotifyApiClient spotifyApiClient() {
        WebClient webClient = WebClient.builder()
            .baseUrl("https://api.spotify.com/v1")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .filter(spotifyAuthFilter())
            .filter(logRequestFilter())
            .build();
            
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(WebClientAdapter.create(webClient))
            .build();
            
        return factory.createClient(SpotifyApiClient.class);
    }
    
    private ExchangeFilterFunction spotifyAuthFilter() {
        return (request, next) -> {
            String token = getOrRefreshAccessToken();
            
            ClientRequest filtered = ClientRequest.from(request)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
                
            return next.exchange(filtered);
        };
    }
    
    private String getOrRefreshAccessToken() {
        // Implémentation OAuth2 Client Credentials Flow
        // Cache token jusqu'à expiration
        return tokenService.getValidToken(clientId, clientSecret);
    }
    
    private ExchangeFilterFunction logRequestFilter() {
        return (request, next) -> {
            log.debug("Spotify API: {} {}", request.method(), request.url());
            return next.exchange(request);
        };
    }
}
```

### application.yml
```yaml
spotify:
  client-id: ${SPOTIFY_CLIENT_ID}
  client-secret: ${SPOTIFY_CLIENT_SECRET}
  auth-url: https://accounts.spotify.com/api/token
  rate-limit:
    requests-per-second: 10
    cache-ttl-minutes: 60
```

## @HttpExchange Client

### Interface principale
```java
public interface SpotifyApiClient {
    
    @GetExchange("/search")
    SpotifySearchResponse searchTracks(
        @RequestParam("q") String query,
        @RequestParam("type") String type,
        @RequestParam(value = "limit", defaultValue = "10") int limit
    );
    
    @GetExchange("/audio-features/{id}")
    SpotifyAudioFeatures getAudioFeatures(@PathVariable String id);
    
    @GetExchange("/tracks/{id}")
    SpotifyTrack getTrack(@PathVariable String id);
    
    @GetExchange("/tracks")
    SpotifyTracksResponse getTracks(@RequestParam("ids") String trackIds);
    
    @GetExchange("/recommendations")
    SpotifyRecommendationsResponse getRecommendations(
        @RequestParam(value = "seed_tracks", required = false) String seedTracks,
        @RequestParam(value = "seed_artists", required = false) String seedArtists,
        @RequestParam(value = "limit", defaultValue = "20") int limit,
        @RequestParam(value = "target_energy", required = false) Double targetEnergy
    );
    
    @GetExchange("/artists/{id}")
    SpotifyArtist getArtist(@PathVariable String id);
}
```

## Records Spotify

### Search Response
```java
public record SpotifySearchResponse(
    SpotifySearchTracks tracks
) {}

public record SpotifySearchTracks(
    List<SpotifyTrackItem> items,
    int total,
    int limit,
    int offset
) {}

public record SpotifyTrackItem(
    String id,
    String name,
    List<SpotifyArtistItem> artists,
    SpotifyAlbum album,
    int duration_ms,
    int popularity,
    boolean explicit,
    String preview_url
) {
    public String primaryArtist() {
        return artists.isEmpty() ? "Unknown" : artists.get(0).name();
    }
}

public record SpotifyArtistItem(
    String id,
    String name,
    String type
) {}

public record SpotifyAlbum(
    String id,
    String name,
    String release_date,
    List<SpotifyImage> images,
    String album_type
) {
    public int releaseYear() {
        if (release_date == null || release_date.length() < 4) return 0;
        return Integer.parseInt(release_date.substring(0, 4));
    }
}

public record SpotifyImage(
    String url,
    int height,
    int width
) {}
```

### Audio Features
```java
public record SpotifyAudioFeatures(
    String id,
    double danceability,      // 0.0 - 1.0
    double energy,            // 0.0 - 1.0
    int key,                  // 0-11 (C=0)
    double loudness,          // -60 to 0 dB
    int mode,                 // 0=Minor, 1=Major
    double speechiness,       // 0.0 - 1.0
    double acousticness,      // 0.0 - 1.0
    double instrumentalness,  // 0.0 - 1.0
    double liveness,          // 0.0 - 1.0
    double valence,           // 0.0 - 1.0 (mood)
    double tempo,             // BPM
    int duration_ms,
    int time_signature
) {
    private static final String[] KEYS = {
        "C", "C#", "D", "D#", "E", "F", 
        "F#", "G", "G#", "A", "A#", "B"
    };
    
    public String musicalKey() {
        return KEYS[key];
    }
    
    public String musicalMode() {
        return mode == 1 ? "Major" : "Minor";
    }
    
    public String fullKey() {
        return musicalKey() + " " + musicalMode();
    }
    
    public int bpm() {
        return (int) Math.round(tempo);
    }
}
```

### Track complet
```java
public record SpotifyTrack(
    String id,
    String name,
    List<SpotifyArtistItem> artists,
    SpotifyAlbum album,
    int duration_ms,
    boolean explicit,
    String href,
    int popularity,
    String preview_url,
    String type,
    String uri
) {
    public String primaryArtist() {
        return artists.isEmpty() ? "Unknown" : artists.get(0).name();
    }
}
```

### Artiste
```java
public record SpotifyArtist(
    String id,
    String name,
    List<String> genres,
    int popularity,
    List<SpotifyImage> images,
    SpotifyFollowers followers
) {}

public record SpotifyFollowers(
    String href,
    int total
) {}
```

## Service Enrichissement

### Service principal
```java
@Service
@Slf4j
public class SpotifyEnrichmentService {
    
    private final SpotifyApiClient spotifyClient;
    private final SpotifyTrackRepository repository;
    private final SpotifyCacheService cache;
    
    public SpotifyEnrichmentService(
        SpotifyApiClient spotifyClient,
        SpotifyTrackRepository repository,
        SpotifyCacheService cache
    ) {
        this.spotifyClient = spotifyClient;
        this.repository = repository;
        this.cache = cache;
    }
    
    public SpotifyEnrichmentResult enrichTrack(
        String filepath,
        String artist,
        String title
    ) {
        // 1. Check cache
        Optional<SpotifyTrackData> cached = cache.get(artist, title);
        if (cached.isPresent()) {
            log.debug("Cache hit: {} - {}", artist, title);
            return SpotifyEnrichmentResult.success(cached.get());
        }
        
        try {
            // 2. Search Spotify
            String query = buildSearchQuery(artist, title);
            SpotifySearchResponse searchResult = spotifyClient.searchTracks(query, "track", 5);
            
            if (searchResult.tracks().items().isEmpty()) {
                return SpotifyEnrichmentResult.notFound();
            }
            
            // 3. Get best match
            SpotifyTrackItem bestMatch = findBestMatch(
                searchResult.tracks().items(),
                artist,
                title
            );
            
            // 4. Fetch audio features
            SpotifyAudioFeatures audioFeatures = spotifyClient.getAudioFeatures(bestMatch.id());
            
            // 5. Convert to internal model
            SpotifyTrackData enrichedData = convertToInternalModel(
                filepath,
                bestMatch,
                audioFeatures
            );
            
            // 6. Store in DB and cache
            repository.save(SpotifyTrackEntity.fromRecord(enrichedData));
            cache.put(artist, title, enrichedData);
            
            log.info("Enriched: {} - {}", artist, title);
            
            return SpotifyEnrichmentResult.success(enrichedData);
            
        } catch (Exception e) {
            log.error("Enrichment failed: {} - {}", artist, title, e);
            return SpotifyEnrichmentResult.error(e.getMessage());
        }
    }
    
    private String buildSearchQuery(String artist, String title) {
        String cleanArtist = cleanForSearch(artist);
        String cleanTitle = cleanForSearch(title);
        return String.format("artist:%s track:%s", cleanArtist, cleanTitle);
    }
    
    private String cleanForSearch(String text) {
        if (text == null) return "";
        return text.replaceAll("[^a-zA-Z0-9\\s]", "").trim();
    }
    
    private SpotifyTrackItem findBestMatch(
        List<SpotifyTrackItem> items,
        String targetArtist,
        String targetTitle
    ) {
        // Simple: prendre le plus populaire
        // Améliorable: scoring par similarité nom
        return items.stream()
            .max(Comparator.comparingInt(SpotifyTrackItem::popularity))
            .orElse(items.get(0));
    }
    
    private SpotifyTrackData convertToInternalModel(
        String filepath,
        SpotifyTrackItem track,
        SpotifyAudioFeatures features
    ) {
        return new SpotifyTrackData(
            track.id(),
            filepath,
            track.primaryArtist(),
            track.name(),
            track.album().name(),
            List.of(), // Genres via artiste si besoin
            track.album().releaseYear(),
            (double) track.popularity(),
            track.duration_ms(),
            new AudioFeatures(
                features.danceability(),
                features.energy(),
                features.valence(),
                features.acousticness(),
                features.instrumentalness(),
                features.speechiness(),
                features.bpm(),
                features.musicalKey(),
                features.musicalMode(),
                features.time_signature()
            ),
            null, // embedding ajouté après
            LocalDateTime.now()
        );
    }
}
```

## OAuth2 Token Management

### Service tokens
```java
@Service
@Slf4j
public class SpotifyTokenService {
    
    @Value("${spotify.auth-url}")
    private String authUrl;
    
    private String cachedToken;
    private Instant tokenExpiry;
    
    public synchronized String getValidToken(String clientId, String clientSecret) {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        
        return refreshToken(clientId, clientSecret);
    }
    
    private String refreshToken(String clientId, String clientSecret) {
        try {
            WebClient webClient = WebClient.builder()
                .baseUrl(authUrl)
                .build();
                
            String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());
                
            TokenResponse response = webClient.post()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=client_credentials")
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block();
                
            cachedToken = response.access_token();
            tokenExpiry = Instant.now().plusSeconds(response.expires_in() - 60); // 60s buffer
            
            log.info("Token refreshed, expires in: {}s", response.expires_in());
            
            return cachedToken;
            
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new SpotifyAuthException("Failed to get access token", e);
        }
    }
    
    private record TokenResponse(
        String access_token,
        String token_type,
        int expires_in
    ) {}
}
```

## Cache Service

### Simple cache avec Caffeine
```java
@Service
public class SpotifyCacheService {
    
    private final Cache<String, SpotifyTrackData> cache;
    
    public SpotifyCacheService(
        @Value("${spotify.rate-limit.cache-ttl-minutes}") int ttlMinutes
    ) {
        this.cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
            .build();
    }
    
    public Optional<SpotifyTrackData> get(String artist, String title) {
        String key = cacheKey(artist, title);
        return Optional.ofNullable(cache.getIfPresent(key));
    }
    
    public void put(String artist, String title, SpotifyTrackData data) {
        String key = cacheKey(artist, title);
        cache.put(key, data);
    }
    
    private String cacheKey(String artist, String title) {
        return (artist + ":" + title).toLowerCase();
    }
}
```

## Rate Limiting

### Rate limiter
```java
@Component
public class SpotifyRateLimiter {
    
    private final RateLimiter rateLimiter;
    
    public SpotifyRateLimiter(
        @Value("${spotify.rate-limit.requests-per-second}") int rps
    ) {
        this.rateLimiter = RateLimiter.create(rps);
    }
    
    public void acquire() {
        rateLimiter.acquire();
    }
    
    public boolean tryAcquire(Duration timeout) {
        return rateLimiter.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
}

// Usage dans service
public SpotifyEnrichmentResult enrichTrack(...) {
    rateLimiter.acquire(); // Bloque si rate limit atteint
    
    // Puis appel API
    return spotifyClient.searchTracks(...);
}
```

## Batch Operations

### Enrichir multiple tracks
```java
public List<SpotifyEnrichmentResult> enrichBatch(List<MusicFileInfo> files) {
    return files.stream()
        .map(file -> {
            rateLimiter.acquire();
            return enrichTrack(
                file.filepath(),
                file.artist(),
                file.title()
            );
        })
        .toList();
}

// Avec parallélisation limitée
public List<SpotifyEnrichmentResult> enrichBatchParallel(List<MusicFileInfo> files) {
    return files.parallelStream()
        .limit(10) // Max 10 threads parallèles
        .map(file -> {
            rateLimiter.acquire();
            return enrichTrack(
                file.filepath(),
                file.artist(),
                file.title()
            );
        })
        .toList();
}
```

## Error Handling

### Exceptions custom
```java
public class SpotifyApiException extends RuntimeException {
    private final int statusCode;
    
    public SpotifyApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}

public class SpotifyAuthException extends RuntimeException {
    public SpotifyAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class SpotifyRateLimitException extends RuntimeException {
    private final Duration retryAfter;
    
    public SpotifyRateLimitException(Duration retryAfter) {
        super("Rate limit exceeded");
        this.retryAfter = retryAfter;
    }
}
```

## Testing

### Mock SpotifyApiClient
```java
@ExtendWith(MockitoExtension.class)
class SpotifyEnrichmentServiceTest {
    
    @Mock
    private SpotifyApiClient spotifyClient;
    
    @Mock
    private SpotifyTrackRepository repository;
    
    @Mock
    private SpotifyCacheService cache;
    
    @InjectMocks
    private SpotifyEnrichmentService service;
    
    @Test
    void enrichTrack_shouldReturnSuccess_whenTrackFound() {
        // Given
        String artist = "Daft Punk";
        String title = "One More Time";
        
        SpotifySearchResponse searchResponse = new SpotifySearchResponse(
            new SpotifySearchTracks(
                List.of(createMockTrack()),
                1, 10, 0
            )
        );
        
        when(cache.get(artist, title)).thenReturn(Optional.empty());
        when(spotifyClient.searchTracks(anyString(), eq("track"), eq(5)))
            .thenReturn(searchResponse);
        when(spotifyClient.getAudioFeatures(anyString()))
            .thenReturn(createMockFeatures());
            
        // When
        SpotifyEnrichmentResult result = service.enrichTrack(
            "/music/test.mp3",
            artist,
            title
        );
        
        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.data());
    }
}
```

## Checklist

- [ ] @HttpExchange interface définie
- [ ] Records pour toutes réponses
- [ ] OAuth2 token management
- [ ] Cache implémenté
- [ ] Rate limiting actif
- [ ] Error handling robuste
- [ ] Logging approprié
- [ ] Tests avec mocks
