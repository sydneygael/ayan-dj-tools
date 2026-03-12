# RAG & Vector Database — Reference Rapide

## Spring AI VectorStore API

```java
// Ajout
vectorStore.add(List.of(document));

// Recherche
List<Document> results = vectorStore.similaritySearch(searchRequest);

// SearchRequest builder
SearchRequest request = SearchRequest.query("energetic house track")
    .withTopK(10)                          // max resultats
    .withSimilarityThreshold(0.75)         // seuil minimum
    .withFilterExpression(filterExpr);     // filtres metadata
```

## Document Class

```java
// Construction
Document doc = new Document(text, metadata);   // text + Map<String, Object>
Document doc = new Document(id, text, metadata);

// Builder (pour score)
Document doc = Document.builder()
    .id("spotify-123")
    .text("Track: Strobe by Deadmau5...")
    .metadata(Map.of("artist", "Deadmau5", "genre", "Progressive House"))
    .score(0.92)
    .build();

// Accesseurs
String id = doc.getId();
String text = doc.getText();                    // PAS getContent()
Map<String, Object> meta = doc.getMetadata();
double score = doc.getScore();                  // final, set via Builder
```

## FilterExpressionBuilder

```java
FilterExpressionBuilder b = new FilterExpressionBuilder();

// Operateurs
b.eq("artist", "Deadmau5")                    // egalite
b.gte("releaseYear", 2020)                     // >=
b.lte("tempo", 140)                            // <=
b.in("genres", List.of("House", "Techno"))     // dans liste

// Combinaison
b.and(b.eq("genres", "House"), b.gte("energy", 0.8))
b.or(b.eq("genres", "House"), b.eq("genres", "Techno"))

// Utilisation
SearchRequest.query("...").withFilterExpression(b.build())
```

## Qdrant Config

| Parametre | Valeur | Notes |
|-----------|--------|-------|
| Host | `localhost` | Docker container |
| Port gRPC | `6334` | Utilise par Spring AI |
| Port HTTP | `6333` | Dashboard UI + REST API |
| Collection | `dj-tracks` | Nom dans application.yml |
| Initialize schema | `true` | Auto-creation collection |

## Embedding Model

| Parametre | Valeur |
|-----------|--------|
| Modele | `nomic-embed-text` |
| Provider | Ollama |
| Dimensions | 768 |
| Methodes | `embed(Document)`, `embed(String)`, `embed(List<String>)`, `call(EmbeddingRequest)`, `dimensions()` |
| PAS disponible | ~~`embed(List<Document>)`~~ |

## Config Keys

```yaml
spring:
  ai:
    vectorstore:
      qdrant:
        host: localhost
        port: 6334
        collection-name: dj-tracks
        initialize-schema: true

dj-tagger:
  rag:
    similarity-threshold: 0.7
    max-similar-tracks: 5
    embedding-dimension: 768
```

## Metadata Document (Champs Stockes)

| Cle | Type | Exemple |
|-----|------|---------|
| `spotifyId` | String | `"4uLU6hMCjMI75M1A2tKUQC"` |
| `artist` | String | `"Deadmau5"` |
| `title` | String | `"Strobe"` |
| `genres` | String | `"Progressive House,Electro"` (comma-separated) |
| `releaseYear` | int | `2009` |
| `tempo` | double | `128.0` |
| `energy` | double | `0.85` |
| `popularity` | double | `72.0` |

## Domain Records Phase 6

| Record | Champs |
|--------|--------|
| `SimilarTrackResult` | `track` (EnrichedTrackMetadata), `similarityScore` (double) |
| `SmartTagSuggestion` | `filepath`, `suggestedTags`, `similarTracks`, `confidence`, `source` |

## Testcontainers IT Pattern

```java
// Container Qdrant
@Container
static QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:latest");

// Mock embedding model (deterministe, 768-dim)
DeterministicEmbeddingModel embeddingModel = new DeterministicEmbeddingModel(768);
```
