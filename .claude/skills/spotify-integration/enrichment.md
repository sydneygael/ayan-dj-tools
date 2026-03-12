# Spotify Integration — Enrichment Service & Tests

## Service principal

```java
@Service
@Slf4j
public class SpotifyEnrichmentService {
    private final SpotifyApiClient spotifyClient;
    private final SpotifyTrackRepository repository;
    private final SpotifyCacheService cache;

    public SpotifyEnrichmentResult enrichTrack(String filepath, String artist, String title) {
        Optional<SpotifyTrackData> cached = cache.get(artist, title);
        if (cached.isPresent()) return SpotifyEnrichmentResult.success(cached.get());

        try {
            String query = String.format("artist:%s track:%s",
                artist.replaceAll("[^a-zA-Z0-9\\s]", "").trim(),
                title.replaceAll("[^a-zA-Z0-9\\s]", "").trim());
            SpotifySearchResponse searchResult = spotifyClient.searchTracks(query, "track", 5);

            if (searchResult.tracks().items().isEmpty()) return SpotifyEnrichmentResult.notFound();

            SpotifyTrackItem bestMatch = searchResult.tracks().items().stream()
                .max(Comparator.comparingInt(SpotifyTrackItem::popularity))
                .orElse(searchResult.tracks().items().get(0));

            SpotifyAudioFeatures audioFeatures = spotifyClient.getAudioFeatures(bestMatch.id());
            SpotifyTrackData enrichedData = convertToInternalModel(filepath, bestMatch, audioFeatures);

            repository.save(SpotifyTrackEntity.fromRecord(enrichedData));
            cache.put(artist, title, enrichedData);

            return SpotifyEnrichmentResult.success(enrichedData);
        } catch (Exception e) {
            log.error("Enrichment failed: {} - {}", artist, title, e);
            return SpotifyEnrichmentResult.error(e.getMessage());
        }
    }
}
```

## Batch Operations

```java
public List<SpotifyEnrichmentResult> enrichBatch(List<MusicFileInfo> files) {
    return files.stream()
        .map(file -> {
            rateLimiter.acquire();
            return enrichTrack(file.filepath(), file.artist(), file.title());
        })
        .toList();
}
```

## Testing

```java
@ExtendWith(MockitoExtension.class)
class SpotifyEnrichmentServiceTest {
    @Mock private SpotifyApiClient spotifyClient;
    @Mock private SpotifyTrackRepository repository;
    @Mock private SpotifyCacheService cache;
    @InjectMocks private SpotifyEnrichmentService service;

    @Test
    void enrichTrack_shouldReturnSuccess_whenTrackFound() {
        when(cache.get("Daft Punk", "One More Time")).thenReturn(Optional.empty());
        when(spotifyClient.searchTracks(anyString(), eq("track"), eq(5)))
            .thenReturn(new SpotifySearchResponse(
                new SpotifySearchTracks(List.of(createMockTrack()), 1, 10, 0)));
        when(spotifyClient.getAudioFeatures(anyString())).thenReturn(createMockFeatures());

        SpotifyEnrichmentResult result = service.enrichTrack(
            "/music/test.mp3", "Daft Punk", "One More Time");

        assertTrue(result.isSuccess());
        assertNotNull(result.data());
    }
}
```
