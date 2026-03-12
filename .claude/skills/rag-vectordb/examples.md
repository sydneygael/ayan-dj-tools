# RAG & Vector Database — Exemples Complets

## 1. Vectoriser une Track Complete End-to-End

```java
@Service
@Slf4j
public class TrackVectorizationService {
    private final VectorStore vectorStore;

    public void vectorizeTrack(EnrichedTrackMetadata track) {
        try {
            // 1. Construire texte riche pour embedding
            String embeddingText = String.format(
                "Track: %s by %s. Album: %s. Genres: %s. Year: %d. "
                + "Energy %.2f, Danceability %.2f, Valence %.2f. "
                + "Tempo: %d BPM, Key: %s. Popularity: %d.",
                track.title(), track.artist(), track.album(),
                String.join(", ", track.genres()), track.releaseYear(),
                track.audioFeatures().energy(),
                track.audioFeatures().danceability(),
                track.audioFeatures().valence(),
                track.audioFeatures().bpm(),
                track.audioFeatures().fullKey(),
                track.popularity());

            // 2. Creer metadata pour filtrage ulterieur
            Map<String, Object> metadata = Map.of(
                "spotifyId", track.sourceId(),
                "artist", track.artist(),
                "title", track.title(),
                "genres", String.join(",", track.genres()),
                "releaseYear", track.releaseYear(),
                "tempo", track.audioFeatures().tempo(),
                "energy", track.audioFeatures().energy(),
                "popularity", (double) track.popularity()
            );

            // 3. Stocker dans Qdrant
            Document document = new Document(embeddingText, metadata);
            vectorStore.add(List.of(document));

            log.info("Vectorized: {} - {}", track.artist(), track.title());

        } catch (Exception e) {
            // Error-tolerant : log et continue (pas de throw)
            log.error("Vectorization failed for {}: {}", track.sourceId(), e.getMessage());
        }
    }
}
```

## 2. Recherche par Similarite avec Filtres

```java
public List<SimilarTrackResult> findSimilarTracks(
        String query, int limit, String genre, Integer minYear,
        Double minTempo, Double maxTempo) {

    // 1. Construire filtres
    SearchRequest request = SearchRequest.query(query)
        .withTopK(limit)
        .withSimilarityThreshold(similarityThreshold);

    // 2. Appliquer filtres optionnels
    FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
    List<Filter.Expression> filters = new ArrayList<>();

    if (genre != null && !genre.isBlank()) {
        filters.add(filterBuilder.eq("genres", genre));
    }
    if (minYear != null) {
        filters.add(filterBuilder.gte("releaseYear", minYear));
    }
    if (minTempo != null) {
        filters.add(filterBuilder.gte("tempo", minTempo));
    }
    if (maxTempo != null) {
        filters.add(filterBuilder.lte("tempo", maxTempo));
    }

    if (!filters.isEmpty()) {
        Filter.Expression combined = filters.stream()
            .reduce(filterBuilder::and)
            .orElse(filters.get(0));
        request = request.withFilterExpression(combined);
    }

    // 3. Executer recherche
    return vectorStore.similaritySearch(request).stream()
        .map(doc -> new SimilarTrackResult(
            documentToTrackMetadata(doc),
            doc.getScore()))
        .toList();
}

private EnrichedTrackMetadata documentToTrackMetadata(Document doc) {
    Map<String, Object> meta = doc.getMetadata();
    return new EnrichedTrackMetadata(
        (String) meta.get("spotifyId"),
        (String) meta.get("artist"),
        (String) meta.get("title"),
        null, // album
        meta.containsKey("genres")
            ? List.of(((String) meta.get("genres")).split(","))
            : List.of(),
        List.of(), null, null, null, List.of(),
        meta.containsKey("releaseYear")
            ? ((Number) meta.get("releaseYear")).intValue() : 0,
        meta.containsKey("popularity")
            ? ((Number) meta.get("popularity")).intValue() : 0,
        0, null
    );
}
```

## 3. Migration Batch de Donnees Existantes

```java
@Service
@Slf4j
public class VectorMigrationService {
    private final SpotifyTrackRepository repository;
    private final VectorStoreService vectorStore;

    /**
     * Migre toutes les tracks Spotify existantes vers Qdrant.
     * Traite par batch de 100 pour eviter surcharge memoire.
     */
    public void migrateAllToVectorStore() {
        log.info("Starting vector migration...");
        long total = repository.count();
        int batchSize = 100;
        int processed = 0;

        for (int page = 0; processed < total; page++) {
            Pageable pageable = PageRequest.of(page, batchSize);
            Page<SpotifyTrackEntity> batch = repository.findAll(pageable);
            List<SpotifyTrackData> tracks = batch.getContent().stream()
                .map(SpotifyTrackEntity::toRecord).toList();

            vectorStore.vectorizeBatch(tracks);
            processed += tracks.size();
            log.info("Migrated {}/{} tracks", processed, total);
        }

        log.info("Migration complete: {} tracks vectorized", processed);
    }

    /**
     * Vectorise uniquement les tracks manquantes dans Qdrant.
     */
    public void vectorizeMissing() {
        List<SpotifyTrackEntity> all = repository.findAll();
        int added = 0;
        for (SpotifyTrackEntity entity : all) {
            if (!isInVectorStore(entity.getSpotifyId())) {
                vectorStore.vectorizeSpotifyTrack(entity.toRecord());
                added++;
            }
        }
        log.info("Added {} missing tracks to vector store", added);
    }

    private boolean isInVectorStore(String spotifyId) {
        SearchRequest request = SearchRequest.query(spotifyId)
            .withTopK(1)
            .withFilterExpression(
                new FilterExpressionBuilder().eq("spotifyId", spotifyId).build());
        return !vectorStore.similaritySearch(request).isEmpty();
    }
}
```

## 4. Test d'Integration avec Testcontainers + DeterministicEmbeddingModel

```java
@SpringBootTest(classes = {
    QdrantVectorStoreAdapter.class,
    TrackVectorizationService.class
})
@EnableAutoConfiguration(exclude = {
    OllamaApiAutoConfiguration.class,
    OllamaChatAutoConfiguration.class,
    OllamaEmbeddingAutoConfiguration.class
})
@Testcontainers
class QdrantVectorStoreAdapterIT {

    @Container
    static QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:latest")
        .withExposedPorts(6334);

    @DynamicPropertySource
    static void qdrantProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.vectorstore.qdrant.host", qdrant::getHost);
        registry.add("spring.ai.vectorstore.qdrant.port", qdrant::getFirstMappedPort);
        registry.add("spring.ai.vectorstore.qdrant.collection-name", () -> "test-tracks");
        registry.add("spring.ai.vectorstore.qdrant.initialize-schema", () -> "true");
    }

    // Mock embedding model — deterministe, 768 dimensions
    @TestConfiguration
    static class TestConfig {
        @Bean
        public EmbeddingModel embeddingModel() {
            return new DeterministicEmbeddingModel(768);
        }
    }

    @Autowired private TrackVectorizationService vectorService;

    @Test
    void vectorizeAndSearch_shouldFindSimilarTracks() {
        // Given — vectoriser une track
        EnrichedTrackMetadata track = createTestTrack(
            "spotify-123", "Deadmau5", "Strobe",
            List.of("Progressive House"), 128.0, 0.7);
        vectorService.vectorizeTrack(track);

        // When — recherche par similarite
        List<SimilarTrackResult> results = vectorService
            .findSimilarTracks("Progressive House Deadmau5", 5);

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).track().artist()).isEqualTo("Deadmau5");
        assertThat(results.get(0).similarityScore()).isGreaterThan(0.5);
    }

    @Test
    void searchWithFilters_shouldRespectCriteria() {
        // Given — plusieurs tracks vectorisees
        vectorService.vectorizeTrack(createTestTrack(
            "s1", "Deadmau5", "Strobe", List.of("Progressive House"), 128.0, 0.7));
        vectorService.vectorizeTrack(createTestTrack(
            "s2", "Daft Punk", "Around The World", List.of("House"), 121.0, 0.85));
        vectorService.vectorizeTrack(createTestTrack(
            "s3", "Aphex Twin", "Windowlicker", List.of("IDM"), 137.0, 0.6));

        // When — filtre par genre House
        List<SimilarTrackResult> results = vectorService
            .findSimilarTracks("dance music", 5, "House", null, null, null);

        // Then — seulement House et Progressive House
        assertThat(results).allMatch(r ->
            r.track().genres().stream().anyMatch(g -> g.contains("House")));
    }

    private EnrichedTrackMetadata createTestTrack(String id, String artist,
            String title, List<String> genres, double tempo, double energy) {
        AudioFeatures features = new AudioFeatures(
            energy, 0.8, 0.6, tempo, "Am", 0, 1, 0.0, 0.1, 0.0, -8.0, 4);
        return new EnrichedTrackMetadata(
            id, artist, title, "Album", genres, List.of(),
            null, null, null, List.of(), 2020, 75, 300000, features);
    }
}
```
