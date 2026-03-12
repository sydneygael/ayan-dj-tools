---
name: rag-vectordb
description: RAG avec Qdrant vector store + Spring AI embeddings. Vectorisation tracks Spotify, recherche par similarite semantique, filtres metadata, suggestions intelligentes. Utiliser pour tout code lie au vector store ou RAG.
user-invocable: false
---

# RAG & Vector Database Skill

Qdrant + Embeddings + Semantic Search

> Reference rapide API : voir [reference.md](./reference.md)
> Exemples complets (migration, tests, recherche) : voir [examples.md](./examples.md)

## Principes

- **Vectoriser** donnees Spotify enrichies
- **Similarite semantique** pour suggestions
- **Cache** embeddings eviter recalcul
- **Filtres** pour recherches precises

## Dependencies

```gradle
dependencies {
    implementation 'org.springframework.ai:spring-ai-qdrant-store-spring-boot-starter'
}
```

## Configuration

### Docker Qdrant
```yaml
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
    public VectorStore vectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder()
            .client(qdrantClient).embeddingModel(embeddingModel)
            .collectionName("spotify_tracks").initializeSchema(true).build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return new OllamaEmbeddingModel(OllamaOptions.create().withModel("nomic-embed-text"));
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

```java
@Service
@Slf4j
public class VectorStoreService {
    private final VectorStore vectorStore;

    @Value("${dj-tagger.rag.similarity-threshold}")
    private double similarityThreshold;

    public void vectorizeSpotifyTrack(SpotifyTrackData track) {
        try {
            String text = createEmbeddingText(track);
            Map<String, Object> metadata = Map.of(
                "spotifyId", track.spotifyId(), "artist", track.artist(),
                "title", track.title(), "genres", String.join(",", track.genres()),
                "releaseYear", track.releaseYear(),
                "tempo", track.audioFeatures().tempo(),
                "energy", track.audioFeatures().energy());

            Document document = new Document(text, metadata);
            document.setId(track.spotifyId());
            vectorStore.add(List.of(document));
        } catch (Exception e) {
            log.error("Vectorization failed: {}", track.spotifyId(), e);
        }
    }

    private String createEmbeddingText(SpotifyTrackData track) {
        return String.format(
            "Track: %s by %s. Album: %s. Genres: %s. Year: %d. "
            + "Energy %.2f, Danceability %.2f, Mood %.2f. "
            + "Tempo: %d BPM, Key: %s %s. Popularity: %.0f.",
            track.title(), track.artist(), track.album(),
            String.join(", ", track.genres()), track.releaseYear(),
            track.audioFeatures().energy(), track.audioFeatures().danceability(),
            track.audioFeatures().valence(), track.audioFeatures().tempo(),
            track.audioFeatures().key(), track.audioFeatures().mode(),
            track.popularity());
    }

    public void vectorizeBatch(List<SpotifyTrackData> tracks) {
        int batchSize = 100;
        for (int i = 0; i < tracks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, tracks.size());
            List<Document> documents = tracks.subList(i, end).stream()
                .map(this::trackToDocument).toList();
            vectorStore.add(documents);
        }
    }
}
```

## Recherche Similarite

```java
public List<SpotifyTrackData> findSimilar(String query, int limit) {
    SearchRequest request = SearchRequest.query(query)
        .withTopK(limit).withSimilarityThreshold(similarityThreshold);

    return vectorStore.similaritySearch(request).stream()
        .map(doc -> {
            String spotifyId = (String) doc.getMetadata().get("spotifyId");
            return repository.findBySpotifyId(spotifyId)
                .map(SpotifyTrackEntity::toRecord).orElse(null);
        })
        .filter(Objects::nonNull).toList();
}

public List<SpotifyTrackData> findSimilarWithFilters(
    String query, int limit, Map<String, Object> filters
) {
    FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
    if (filters.containsKey("genre")) filterBuilder.eq("genres", filters.get("genre"));
    if (filters.containsKey("minYear")) filterBuilder.gte("releaseYear", filters.get("minYear"));
    if (filters.containsKey("minTempo")) filterBuilder.gte("tempo", filters.get("minTempo"));
    if (filters.containsKey("maxTempo")) filterBuilder.lte("tempo", filters.get("maxTempo"));

    SearchRequest request = SearchRequest.query(query)
        .withTopK(limit).withSimilarityThreshold(similarityThreshold)
        .withFilterExpression(filterBuilder.build());

    return vectorStore.similaritySearch(request).stream()
        .map(this::documentToTrackData).filter(Objects::nonNull).toList();
}
```

## Suggestions Intelligentes

```java
@Service
public class RagSuggestionService {
    private final VectorStoreService vectorStore;
    private final SpotifyEnrichmentService spotify;

    public TagSuggestions smartSuggest(MusicFileInfo file) {
        SpotifyEnrichmentResult spotifyResult = null;
        if (file.hasArtistAndTitle())
            spotifyResult = spotify.enrichTrack(file.filepath(), file.artist(), file.title());

        List<SpotifyTrackData> similarTracks = List.of();
        if (spotifyResult != null && spotifyResult.isSuccess()) {
            String query = buildQueryFromSpotifyTrack(spotifyResult.data());
            similarTracks = vectorStore.findSimilar(query, 5);
        }

        return aggregateSuggestions(file, spotifyResult, similarTracks);
    }
}
```

## Testing

```java
@SpringBootTest
class VectorStoreServiceTest {
    @Autowired private VectorStoreService service;

    @Test
    void vectorizeSpotifyTrack_shouldStoreInQdrant() {
        SpotifyTrackData track = createTestTrack();
        service.vectorizeSpotifyTrack(track);

        List<SpotifyTrackData> similar = service.findSimilar(
            track.artist() + " " + track.title(), 1);
        assertFalse(similar.isEmpty());
    }
}
```

## Checklist

- [ ] Qdrant configure et running
- [ ] Embedding model fonctionnel
- [ ] Vectorisation donnees Spotify
- [ ] Recherche similarite precise
- [ ] Filtres sur metadonnees
- [ ] Migration donnees existantes
- [ ] Cache pour performance
- [ ] Tests avec vraies donnees
