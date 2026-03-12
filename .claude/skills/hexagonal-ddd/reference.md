# Architecture Hexagonale + DDD — Reference Rapide

## Structure Packages

```
domain/src/main/java/com/djtools/ayan/musictagger/domain/
├── exception/         AudioProcessingException
├── model/             MusicFileInfo, MissingTagsReport, AudioFeatures,
│   │                  EnrichedTrackMetadata, EnrichmentResult, TaggingPlan,
│   │                  TagOperation, BatchApplyResult, TagPreview, TagChange,
│   │                  TagWriteResult, TaggingHistoryEntry, SimilarTrackResult,
│   │                  SmartTagSuggestion
│   └── vo/            Filepath, BPM, MusicalKey, CamelotKey
├── port/
│   ├── in/            AudioFileReader, MusicMetadataProvider
│   └── out/           AudioFileWriter, PlanRepository, TaggingHistoryRepository,
│                      VectorStorePort
└── usecase/           ScanMusicUseCase, CreatePlanUseCase, ExecutePlanUseCase

infra/src/main/java/com/djtools/ayan/musictagger/
├── MusicTaggerApplication.java
└── infrastructure/
    ├── adapter/in/
    │   ├── mcp/       AyanMusicTools (@Tool functions)
    │   ├── rest/      AgentController, PlanController, TagController, RagController
    │   └── ws/        AgentWebSocketController
    ├── adapter/out/
    │   ├── audio/     JAudioTaggerAdapter, AudioScannerService
    │   ├── persistence/  RedisPlanRepository, RedisTaggingHistoryRepository
    │   ├── spotify/   SpotifyMusicMetadataAdapter, SpotifyApiClient, SpotifyTokenService
    │   └── vectorstore/  QdrantVectorStoreAdapter
    ├── service/       AyanAgentService, PlanManagementService,
    │                  ConversationHistoryService, TrackVectorizationService
    └── config/        DomainConfig, AIConfig, RedisConfig, WebSocketConfig, CorsConfig
```

## Mapping Ports → Adapters

| Port (domain) | Adapter (infra) | Technologie |
|----------------|------------------|-------------|
| `AudioFileReader` | `JAudioTaggerAdapter` | JAudiotagger 3.0.1 |
| `AudioFileWriter` | `JAudioTaggerAdapter` | JAudiotagger 3.0.1 |
| `MusicMetadataProvider` | `SpotifyMusicMetadataAdapter` | Spotify API |
| `PlanRepository` | `RedisPlanRepository` | Redis (TTL 48h) |
| `TaggingHistoryRepository` | `RedisTaggingHistoryRepository` | Redis (TTL 7j) |
| `VectorStorePort` | `QdrantVectorStoreAdapter` | Qdrant + Spring AI |

## Regles de Dependance

| Couche | Peut dependre de | NE PEUT PAS dependre de |
|--------|-----------------|------------------------|
| `domain/` | Rien (Java pur) | Spring, JAudiotagger, Redis, Qdrant, Spotify |
| `infra/` | `domain/`, Spring, toutes libs | — |

## Annotations par Couche

| Couche | Annotations autorisees |
|--------|----------------------|
| `domain/model/` | Aucune (records purs) |
| `domain/model/vo/` | Aucune (records avec validation compact constructor) |
| `domain/usecase/` | Aucune (classes pures, PAS de `@Service`) |
| `domain/port/` | Aucune (interfaces pures) |
| `infra/config/` | `@Configuration`, `@Bean`, `@EnableWebSocketMessageBroker` |
| `infra/adapter/in/rest/` | `@RestController`, `@RequestMapping`, `@PostMapping`... |
| `infra/adapter/in/mcp/` | `@Component`, `@Tool`, `@ToolParam` |
| `infra/adapter/in/ws/` | `@Controller`, `@MessageMapping`, `@SendTo` |
| `infra/adapter/out/` | `@Component`, `@Service`, `@Slf4j` |
| `infra/service/` | `@Service`, `@Slf4j` |

## DomainConfig Wiring Pattern

```java
@Configuration
public class DomainConfig {
    @Bean
    public ScanMusicUseCase scanMusicUseCase(AudioFileReader reader) {
        return new ScanMusicUseCase(reader);
    }

    @Bean
    public CreatePlanUseCase createPlanUseCase(AudioFileReader reader,
            MusicMetadataProvider metadataProvider) {
        return new CreatePlanUseCase(reader, metadataProvider);
    }
}
```

## Regles d'Or (Resume)

1. Domain pur — zero dependance externe
2. Use cases = classes concretes (pas interfaces, pas `@Service`)
3. Ports = interfaces sortantes uniquement
4. Value Objects = records avec validation compact constructor
5. `DomainConfig` dans infra cree les beans domaine via `@Bean`
6. JAMAIS de scan recursif — fichiers pre-selectionnes par UI uniquement
