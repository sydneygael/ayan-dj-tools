# RAG & Vector Database Skill

Qdrant + Embeddings + Semantic Search

## Principes

- **Vectoriser** données Spotify enrichies
- **Similarité sémantique** pour suggestions
- **Cache** embeddings éviter recalcul
- **Filtres** pour recherches précises

## Dependencies

```gradle
dependencies {
    implementation 'org.springframework.ai:spring-ai-qdrant-store-spring-boot-starter'
}
```

## Configuration

### Docker Qdrant
```yaml
# docker-compose.yml
qdrant:
  image: qdrant/qdrant:latest
  ports:
    - "6333:6333"
    - "6334:6334"
  volumes:
    - qdrant_data:/qdrant/storage
```

### Spring Config
```java
@Configuration
public class QdrantConfig {
    
    @Bean
    public VectorStore vectorStore(
        QdrantClient qdrantClient,
        EmbeddingModel embeddingModel
    ) {
        return QdrantVectorStore.builder()
            .client(qdrantClient)
            .embeddingModel(embeddingModel)
            .collectionName("spotify_tracks")
            .initializeSchema(true)
            .build();
    }
    
    @Bean
    public EmbeddingModel embeddingModel() {
        return new OllamaEmbeddingModel(
            OllamaOptions.create()
                .withModel("nomic-embed-text")
        );
    }
    
    @Bean
    public QdrantClient qdrantClient(
        @Value("${spring.ai.vectorstore.qdrant.host}") String host,
        @Value("${spring.ai.vectorstore.qdrant.port}") int port
    ) {
        return new QdrantClient(
            QdrantGrpcClient.newBuilder(host, port, false).build()
        );
    }
}
```

### application.yml
```yaml
spring:
  ai:
    vectorstore:
      qdrant:
        host: localhost
        port: 6333
        collection-name: spotify_tracks
        initialize-schema: true
        
dj-tagger:
  rag:
    similarity-threshold: 0.75
    max-similar-tracks: 5
    embedding-dimension: 768
```

## Service Vectorisation

### Vectoriser données Spotify
```java
@Service
@Slf4j
public class VectorStoreService {
    
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final SpotifyTrackRepository repository;
    
    @Value("${dj-tagger.rag.similarity-threshold}")
    private double similarityThreshold;
    
    public VectorStoreService(
        VectorStore vectorStore,
        EmbeddingModel embeddingModel,
        SpotifyTrackRepository repository
    ) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.repository = repository;
    }
    
    public void vectorizeSpotifyTrack(SpotifyTrackData track) {
        try {
            // 1. Créer texte riche pour embedding
            String text = createEmbeddingText(track);
            
            // 2. Créer document avec métadonnées
            Map<String, Object> metadata = Map.of(
                "spotifyId", track.spotifyId(),
                "artist", track.artist(),
                "title", track.title(),
                "genres", String.join(",", track.genres()),
                "releaseYear", track.releaseYear(),
                "tempo", track.audioFeatures().tempo(),
                "energy", track.audioFeatures().energy(),
                "danceability", track.audioFeatures().danceability(),
                "valence", track.audioFeatures().valence()
            );
            
            Document document = new Document(text, metadata);
            document.setId(track.spotifyId());
            
            // 3. Ajouter au vector store
            vectorStore.add(List.of(document));
            
            log.debug("Vectorized: {} - {}", track.artist(), track.title());
            
        } catch (Exception e) {
            log.error("Vectorization failed: {}", track.spotifyId(), e);
        }
    }
    
    private String createEmbeddingText(SpotifyTrackData track) {
        return String.format(
            "Track: %s by %s. Album: %s. Genres: %s. Year: %d. " +
            "Musical style: Energy %.2f, Danceability %.2f, Mood %.2f. " +
            "Tempo: %d BPM, Key: %s %s, Time signature: %d/4. " +
            "Popularity: %.0f. Characteristics: %s",
            track.title(),
            track.artist(),
            track.album(),
            String.join(", ", track.genres()),
            track.releaseYear(),
            track.audioFeatures().energy(),
            track.audioFeatures().danceability(),
            track.audioFeatures().valence(),
            track.audioFeatures().tempo(),
            track.audioFeatures().key(),
            track.audioFeatures().mode(),
            track.audioFeatures().timeSignature(),
            track.popularity(),
            describeCharacteristics(track.audioFeatures())
        );
    }
    
    private String describeCharacteristics(AudioFeatures features) {
        List<String> chars = new ArrayList<>();
        
        if (features.acousticness() > 0.5) chars.add("acoustic");
        if (features.instrumentalness() > 0.5) chars.add("instrumental");
        if (features.speechiness() > 0.33) chars.add("vocal-heavy");
        
        return chars.isEmpty() ? "electronic" : String.join(", ", chars);
    }
}
```

### Batch vectorisation
```java
public void vectorizeBatch(List<SpotifyTrackData> tracks) {
    int batchSize = 100;
    
    for (int i = 0; i < tracks.size(); i += batchSize) {
        int end = Math.min(i + batchSize, tracks.size());
        List<SpotifyTrackData> batch = tracks.subList(i, end);
        
        List<Document> documents = batch.stream()
            .map(this::trackToDocument)
            .toList();
            
        vectorStore.add(documents);
        
        log.info("Vectorized batch {}/{}", end, tracks.size());
    }
}

private Document trackToDocument(SpotifyTrackData track) {
    String text = createEmbeddingText(track);
    Map<String, Object> metadata = createMetadata(track);
    
    Document doc = new Document(text, metadata);
    doc.setId(track.spotifyId());
    
    return doc;
}
```

## Recherche Similarité

### Search simple
```java
public List<SpotifyTrackData> findSimilar(String query, int limit) {
    SearchRequest request = SearchRequest.query(query)
        .withTopK(limit)
        .withSimilarityThreshold(similarityThreshold);
        
    List<Document> results = vectorStore.similaritySearch(request);
    
    return results.stream()
        .map(doc -> {
            String spotifyId = (String) doc.getMetadata().get("spotifyId");
            return repository.findBySpotifyId(spotifyId)
                .map(SpotifyTrackEntity::toRecord)
                .orElse(null);
        })
        .filter(Objects::nonNull)
        .toList();
}
```

### Search avec filtres
```java
public List<SpotifyTrackData> findSimilarWithFilters(
    String query,
    int limit,
    Map<String, Object> filters
) {
    // Construire filter expression Qdrant
    FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
    
    // Exemple: genre = "Techno" AND releaseYear >= 2020
    if (filters.containsKey("genre")) {
        filterBuilder.eq("genres", filters.get("genre"));
    }
    if (filters.containsKey("minYear")) {
        filterBuilder.gte("releaseYear", filters.get("minYear"));
    }
    if (filters.containsKey("minTempo")) {
        filterBuilder.gte("tempo", filters.get("minTempo"));
    }
    if (filters.containsKey("maxTempo")) {
        filterBuilder.lte("tempo", filters.get("maxTempo"));
    }
    
    SearchRequest request = SearchRequest.query(query)
        .withTopK(limit)
        .withSimilarityThreshold(similarityThreshold)
        .withFilterExpression(filterBuilder.build());
        
    return vectorStore.similaritySearch(request).stream()
        .map(this::documentToTrackData)
        .filter(Objects::nonNull)
        .toList();
}
```

### Search par features audio
```java
public record AudioFeatureQuery(
    Integer minTempo,
    Integer maxTempo,
    Double minEnergy,
    Double maxEnergy,
    Double minDanceability,
    String key,
    String mode
) {}

public List<SpotifyTrackData> findByAudioFeatures(
    AudioFeatureQuery query,
    int limit
) {
    FilterExpressionBuilder filter = new FilterExpressionBuilder();
    
    if (query.minTempo() != null) {
        filter.gte("tempo", query.minTempo());
    }
    if (query.maxTempo() != null) {
        filter.lte("tempo", query.maxTempo());
    }
    if (query.minEnergy() != null) {
        filter.gte("energy", query.minEnergy());
    }
    if (query.key() != null) {
        filter.eq("key", query.key());
    }
    
    // Query sémantique basée sur caractéristiques
    String semanticQuery = buildSemanticQuery(query);
    
    SearchRequest request = SearchRequest.query(semanticQuery)
        .withTopK(limit)
        .withFilterExpression(filter.build());
        
    return vectorStore.similaritySearch(request).stream()
        .map(this::documentToTrackData)
        .toList();
}

private String buildSemanticQuery(AudioFeatureQuery query) {
    StringBuilder sb = new StringBuilder("Music track with ");
    
    if (query.minEnergy() != null && query.minEnergy() > 0.7) {
        sb.append("high energy, ");
    } else if (query.minEnergy() != null && query.minEnergy() < 0.3) {
        sb.append("low energy, ");
    }
    
    if (query.minDanceability() != null && query.minDanceability() > 0.7) {
        sb.append("very danceable, ");
    }
    
    if (query.minTempo() != null) {
        sb.append(String.format("tempo around %d BPM, ", query.minTempo()));
    }
    
    if (query.key() != null) {
        sb.append(String.format("in key %s %s", query.key(), query.mode()));
    }
    
    return sb.toString();
}
```

## Suggestions Intelligentes

### Suggérer tags via RAG
```java
@Service
public class RagSuggestionService {
    
    private final VectorStoreService vectorStore;
    private final SpotifyEnrichmentService spotify;
    
    public TagSuggestions smartSuggest(MusicFileInfo file) {
        // 1. Tentative enrichissement Spotify
        SpotifyEnrichmentResult spotifyResult = null;
        if (file.hasArtistAndTitle()) {
            spotifyResult = spotify.enrichTrack(
                file.filepath(),
                file.artist(),
                file.title()
            );
        }
        
        // 2. Si Spotify trouvé, rechercher similaires
        List<SpotifyTrackData> similarTracks = List.of();
        if (spotifyResult != null && spotifyResult.isSuccess()) {
            String query = buildQueryFromSpotifyTrack(spotifyResult.data());
            similarTracks = vectorStore.findSimilar(query, 5);
        }
        
        // 3. Agréger suggestions
        return aggregateSuggestions(
            file,
            spotifyResult,
            similarTracks
        );
    }
    
    private String buildQueryFromSpotifyTrack(SpotifyTrackData track) {
        return String.format(
            "%s %s genre music with %d BPM energy %.2f",
            track.artist(),
            String.join(" ", track.genres()),
            track.audioFeatures().tempo(),
            track.audioFeatures().energy()
        );
    }
    
    private TagSuggestions aggregateSuggestions(
        MusicFileInfo file,
        SpotifyEnrichmentResult spotify,
        List<SpotifyTrackData> similar
    ) {
        TagSuggestions.Builder builder = TagSuggestions.builder()
            .filepath(file.filepath());
            
        // Spotify en priorité
        if (spotify != null && spotify.isSuccess()) {
            SpotifyTrackData data = spotify.data();
            builder
                .suggestedArtist(data.artist())
                .suggestedTitle(data.title())
                .suggestedGenre(inferGenre(data.genres()))
                .suggestedBpm(String.valueOf(data.audioFeatures().tempo()))
                .suggestedKey(data.audioFeatures().key() + " " + data.audioFeatures().mode())
                .spotifyMatch(data)
                .confidence(0.9);
        }
        
        // Compléter avec tracks similaires
        if (!similar.isEmpty()) {
            builder.similarTracks(similar);
            
            // Si pas de genre Spotify, inférer des similaires
            if (builder.build().suggestedGenre() == null) {
                String inferredGenre = inferGenreFromSimilar(similar);
                builder.suggestedGenre(inferredGenre);
            }
        }
        
        return builder.build();
    }
    
    private String inferGenre(List<String> genres) {
        if (genres.isEmpty()) return null;
        // Prendre genre le plus spécifique (généralement le dernier)
        return genres.get(genres.size() - 1);
    }
    
    private String inferGenreFromSimilar(List<SpotifyTrackData> tracks) {
        // Compter occurrences genres
        Map<String, Long> genreCounts = tracks.stream()
            .flatMap(t -> t.genres().stream())
            .collect(Collectors.groupingBy(g -> g, Collectors.counting()));
            
        // Retourner genre le plus fréquent
        return genreCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
}
```

## Migration Données

### Migrer données existantes
```java
@Service
public class VectorMigrationService {
    
    private final SpotifyTrackRepository repository;
    private final VectorStoreService vectorStore;
    
    public void migrateAllToVectorStore() {
        log.info("Starting vector migration...");
        
        long total = repository.count();
        int batchSize = 100;
        int processed = 0;
        
        for (int page = 0; processed < total; page++) {
            Pageable pageable = PageRequest.of(page, batchSize);
            Page<SpotifyTrackEntity> batch = repository.findAll(pageable);
            
            List<SpotifyTrackData> tracks = batch.getContent().stream()
                .map(SpotifyTrackEntity::toRecord)
                .toList();
                
            vectorStore.vectorizeBatch(tracks);
            
            processed += tracks.size();
            log.info("Migrated {}/{} tracks", processed, total);
        }
        
        log.info("Migration complete");
    }
    
    public void vectorizeMissing() {
        // Trouver tracks en DB mais pas dans vector store
        List<SpotifyTrackEntity> all = repository.findAll();
        
        for (SpotifyTrackEntity entity : all) {
            if (!isInVectorStore(entity.getSpotifyId())) {
                vectorStore.vectorizeSpotifyTrack(entity.toRecord());
            }
        }
    }
    
    private boolean isInVectorStore(String spotifyId) {
        try {
            SearchRequest request = SearchRequest
                .query("dummy")
                .withTopK(1)
                .withFilterExpression(
                    new FilterExpressionBuilder()
                        .eq("spotifyId", spotifyId)
                        .build()
                );
                
            return !vectorStore.vectorStore.similaritySearch(request).isEmpty();
            
        } catch (Exception e) {
            return false;
        }
    }
}
```

## Analytics RAG

### Stats collection
```java
public record RagStats(
    long totalVectors,
    long totalQueries,
    double avgSimilarityScore,
    Map<String, Long> genreDistribution
) {}

public RagStats getCollectionStats() {
    // Via Qdrant API
    long vectorCount = qdrantClient.count("spotify_tracks");
    
    // Depuis repository
    List<SpotifyTrackEntity> all = repository.findAll();
    
    Map<String, Long> genreDistribution = all.stream()
        .flatMap(t -> parseGenres(t.getGenres()).stream())
        .collect(Collectors.groupingBy(g -> g, Collectors.counting()));
        
    return new RagStats(
        vectorCount,
        0, // A tracker
        0.0,
        genreDistribution
    );
}
```

## Testing

### Test vectorisation
```java
@SpringBootTest
class VectorStoreServiceTest {
    
    @Autowired
    private VectorStoreService service;
    
    @Test
    void vectorizeSpotifyTrack_shouldStoreInQdrant() {
        // Given
        SpotifyTrackData track = createTestTrack();
        
        // When
        service.vectorizeSpotifyTrack(track);
        
        // Then
        List<SpotifyTrackData> similar = service.findSimilar(
            track.artist() + " " + track.title(),
            1
        );
        
        assertFalse(similar.isEmpty());
        assertEquals(track.spotifyId(), similar.get(0).spotifyId());
    }
}
```

### Test similarité
```java
@Test
void findSimilar_shouldReturnRelevantTracks() {
    // Given
    String query = "energetic techno 128 BPM";
    
    // When
    List<SpotifyTrackData> results = service.findSimilar(query, 5);
    
    // Then
    assertFalse(results.isEmpty());
    assertTrue(results.stream()
        .allMatch(t -> t.audioFeatures().tempo() >= 120 
                    && t.audioFeatures().tempo() <= 135)
    );
}
```

## Checklist

- [ ] Qdrant configuré et running
- [ ] Embedding model fonctionnel
- [ ] Vectorisation données Spotify
- [ ] Recherche similarité précise
- [ ] Filtres sur métadonnées
- [ ] Migration données existantes
- [ ] Cache pour performance
- [ ] Tests avec vraies données
