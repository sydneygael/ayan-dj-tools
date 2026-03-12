---
name: spotify-integration
description: Integration Spotify API. @HttpExchange client declaratif, OAuth2 token management, records Spotify, cache Caffeine, rate limiting. Utiliser pour tout code lie a l'enrichissement Spotify.
user-invocable: false
---

# Spotify Integration Skill

@HttpExchange + OAuth2 + Records

> Service enrichissement, batch & tests : voir [enrichment.md](./enrichment.md)
> Reference rapide API : voir [reference.md](./reference.md)

## Principes

- **@HttpExchange** pour client declaratif
- **Records** pour toutes reponses API
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
    @Value("${spotify.client-id}") private String clientId;
    @Value("${spotify.client-secret}") private String clientSecret;

    @Bean
    public SpotifyApiClient spotifyApiClient() {
        WebClient webClient = WebClient.builder()
            .baseUrl("https://api.spotify.com/v1")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .filter(spotifyAuthFilter())
            .build();
        return HttpServiceProxyFactory
            .builderFor(WebClientAdapter.create(webClient)).build()
            .createClient(SpotifyApiClient.class);
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

```java
public interface SpotifyApiClient {

    @GetExchange("/search")
    SpotifySearchResponse searchTracks(
        @RequestParam("q") String query,
        @RequestParam("type") String type,
        @RequestParam(value = "limit", defaultValue = "10") int limit);

    @GetExchange("/audio-features/{id}")
    SpotifyAudioFeatures getAudioFeatures(@PathVariable String id);

    @GetExchange("/tracks/{id}")
    SpotifyTrack getTrack(@PathVariable String id);

    @GetExchange("/recommendations")
    SpotifyRecommendationsResponse getRecommendations(
        @RequestParam(value = "seed_tracks", required = false) String seedTracks,
        @RequestParam(value = "seed_artists", required = false) String seedArtists,
        @RequestParam(value = "limit", defaultValue = "20") int limit,
        @RequestParam(value = "target_energy", required = false) Double targetEnergy);

    @GetExchange("/artists/{id}")
    SpotifyArtist getArtist(@PathVariable String id);
}
```

## Records Spotify

```java
public record SpotifySearchResponse(SpotifySearchTracks tracks) {}

public record SpotifySearchTracks(List<SpotifyTrackItem> items, int total, int limit, int offset) {}

public record SpotifyTrackItem(
    String id, String name, List<SpotifyArtistItem> artists,
    SpotifyAlbum album, int duration_ms, int popularity, boolean explicit, String preview_url
) {
    public String primaryArtist() {
        return artists.isEmpty() ? "Unknown" : artists.get(0).name();
    }
}

public record SpotifyArtistItem(String id, String name, String type) {}

public record SpotifyAlbum(
    String id, String name, String release_date, List<SpotifyImage> images, String album_type
) {
    public int releaseYear() {
        if (release_date == null || release_date.length() < 4) return 0;
        return Integer.parseInt(release_date.substring(0, 4));
    }
}

public record SpotifyImage(String url, int height, int width) {}
```

### Audio Features
```java
public record SpotifyAudioFeatures(
    String id, double danceability, double energy, int key, double loudness,
    int mode, double speechiness, double acousticness, double instrumentalness,
    double liveness, double valence, double tempo, int duration_ms, int time_signature
) {
    private static final String[] KEYS = {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
    public String musicalKey() { return KEYS[key]; }
    public String musicalMode() { return mode == 1 ? "Major" : "Minor"; }
    public String fullKey() { return musicalKey() + " " + musicalMode(); }
    public int bpm() { return (int) Math.round(tempo); }
}
```

### Artiste
```java
public record SpotifyArtist(
    String id, String name, List<String> genres,
    int popularity, List<SpotifyImage> images, SpotifyFollowers followers
) {}

public record SpotifyFollowers(String href, int total) {}
```

## OAuth2 Token Management

```java
@Service
@Slf4j
public class SpotifyTokenService {
    @Value("${spotify.auth-url}") private String authUrl;
    private String cachedToken;
    private Instant tokenExpiry;

    public synchronized String getValidToken(String clientId, String clientSecret) {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) return cachedToken;
        return refreshToken(clientId, clientSecret);
    }

    private String refreshToken(String clientId, String clientSecret) {
        String credentials = Base64.getEncoder()
            .encodeToString((clientId + ":" + clientSecret).getBytes());
        TokenResponse response = WebClient.builder().baseUrl(authUrl).build()
            .post()
            .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("grant_type=client_credentials")
            .retrieve().bodyToMono(TokenResponse.class).block();
        cachedToken = response.access_token();
        tokenExpiry = Instant.now().plusSeconds(response.expires_in() - 60);
        return cachedToken;
    }

    private record TokenResponse(String access_token, String token_type, int expires_in) {}
}
```

## Cache Service

```java
@Service
public class SpotifyCacheService {
    private final Cache<String, SpotifyTrackData> cache;

    public SpotifyCacheService(@Value("${spotify.rate-limit.cache-ttl-minutes}") int ttlMinutes) {
        this.cache = Caffeine.newBuilder()
            .maximumSize(10_000).expireAfterWrite(ttlMinutes, TimeUnit.MINUTES).build();
    }

    public Optional<SpotifyTrackData> get(String artist, String title) {
        return Optional.ofNullable(cache.getIfPresent((artist + ":" + title).toLowerCase()));
    }

    public void put(String artist, String title, SpotifyTrackData data) {
        cache.put((artist + ":" + title).toLowerCase(), data);
    }
}
```

## Rate Limiting

```java
@Component
public class SpotifyRateLimiter {
    private final RateLimiter rateLimiter;

    public SpotifyRateLimiter(@Value("${spotify.rate-limit.requests-per-second}") int rps) {
        this.rateLimiter = RateLimiter.create(rps);
    }

    public void acquire() { rateLimiter.acquire(); }
}
```

## Error Handling

```java
public class SpotifyApiException extends RuntimeException {
    private final int statusCode;
    public SpotifyApiException(String message, int statusCode) {
        super(message); this.statusCode = statusCode;
    }
}

public class SpotifyAuthException extends RuntimeException {
    public SpotifyAuthException(String message, Throwable cause) { super(message, cause); }
}

public class SpotifyRateLimitException extends RuntimeException {
    private final Duration retryAfter;
    public SpotifyRateLimitException(Duration retryAfter) {
        super("Rate limit exceeded"); this.retryAfter = retryAfter;
    }
}
```

## Checklist

- [ ] @HttpExchange interface definie
- [ ] Records pour toutes reponses
- [ ] OAuth2 token management
- [ ] Cache implemente
- [ ] Rate limiting actif
- [ ] Error handling robuste
- [ ] Tests avec mocks
