# Architecture Hexagonale + DDD Skill

Simple, concis, pragmatique

## Principes

- **Domain** = cœur métier pur + use cases
- **Ports** = interfaces in et out
- **Adapters** = implémentations tech
- **Pas de couche application** = use cases dans domain
- **Indépendance** = domain ne dépend de rien

## Structure en 2 Couches

```
┌─────────────────────────────────────────┐
│         INFRASTRUCTURE                  │
│  (Adapters In + Out + Config)          │
│  - REST Controllers                     │
│  - MCP Tools                            │
│  - JDBC Repositories                     │
│  - Spotify Client                       │
│  - JAudiotagger                         │
└─────────────┬───────────────────────────┘
              │ dépend de
┌─────────────▼───────────────────────────┐
│           DOMAIN                        │
│  - Model (Entités + Value Objects)     │
│  - Service (logique métier)            │
│  - UseCase (orchestration)             │
│  - Port (interfaces dépendances)   │
└─────────────────────────────────────────┘
```

## Domain Layer

### Entités
```java
// domain/model/MusicFile.java
public class MusicFile {
    private final Filepath filepath;
    private Tags tags;
    private final AudioMetadata metadata;
    
    public MusicFile(Filepath filepath) {
        this.filepath = filepath;
        this.tags = Tags.empty();
    }
    
    public void updateTag(TagKey key, TagValue value) {
        tags = tags.with(key, value);
    }
    
    public boolean hasTag(TagKey key) {
        return tags.contains(key);
    }
    
    public List<TagKey> missingTags() {
        return TagKey.essentials().stream()
            .filter(key -> !tags.contains(key))
            .toList();
    }
}
```

### Value Objects
```java
// domain/model/vo/Filepath.java
public record Filepath(String value) {
    public Filepath {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Filepath blank");
        }
        if (value.contains("..")) {
            throw new IllegalArgumentException("Path traversal not allowed");
        }
    }
    
    public String filename() {
        return value.substring(value.lastIndexOf('/') + 1);
    }
}

// domain/model/vo/BPM.java
public record BPM(int value) {
    public BPM {
        if (value < 60 || value > 200) {
            throw new IllegalArgumentException("BPM must be 60-200");
        }
    }
    
    public boolean isCompatibleWith(BPM other) {
        return Math.abs(value - other.value) <= 6;
    }
}

// domain/model/vo/MusicalKey.java
public record MusicalKey(String key, Mode mode) {
    public enum Mode { MAJOR, MINOR }
    
    public CamelotKey toCamelot() {
        return CamelotKey.from(key, mode);
    }
}

// domain/model/vo/Tags.java
public record Tags(Map<TagKey, TagValue> values) {
    public static Tags empty() {
        return new Tags(Map.of());
    }
    
    public Tags with(TagKey key, TagValue value) {
        Map<TagKey, TagValue> updated = new HashMap<>(values);
        updated.put(key, value);
        return new Tags(Map.copyOf(updated));
    }
    
    public boolean contains(TagKey key) {
        return values.containsKey(key);
    }
}
```

### Domain Service
```java
// domain/service/TaggingService.java
public class TaggingService {
    
    public TaggingPlan createPlan(List<MusicFile> files) {
        List<TagOperation> operations = files.stream()
            .filter(file -> !file.missingTags().isEmpty())
            .map(this::createOperation)
            .toList();
            
        return new TaggingPlan(operations);
    }
    
    private TagOperation createOperation(MusicFile file) {
        return new TagOperation(
            file.filepath(),
            file.tags(),
            suggestTags(file)
        );
    }
    
    private Tags suggestTags(MusicFile file) {
        // Logique métier pure
        return Tags.empty();
    }
}
```

### Agrégat TaggingPlan
```java
// domain/model/TaggingPlan.java
public class TaggingPlan {
    private final PlanId id;
    private List<TagOperation> operations;
    private PlanStatus status;
    
    public TaggingPlan(List<TagOperation> operations) {
        this.id = PlanId.generate();
        this.operations = List.copyOf(operations);
        this.status = PlanStatus.DRAFT;
    }
    
    public void approve() {
        if (status != PlanStatus.READY_FOR_REVIEW) {
            throw new IllegalStateException("Plan not ready");
        }
        status = PlanStatus.APPROVED;
    }
    
    public void execute() {
        if (status != PlanStatus.APPROVED) {
            throw new IllegalStateException("Plan not approved");
        }
        status = PlanStatus.EXECUTING;
    }
    
    public int pendingCount() {
        return (int) operations.stream()
            .filter(op -> op.status() == OperationStatus.PENDING)
            .count();
    }
}
```

## Ports (Interfaces Sortantes)

Les use cases sont des classes concrètes dans `domain/usecase/`, pas des interfaces.
Seules les dépendances externes sont des ports (interfaces).

### Ports Sortants
```java
// domain/port/out/AudioRepository.java
public interface AudioRepository {
    Optional<MusicFile> findByPath(Filepath path);
    void save(MusicFile file);
    // NOTE: Pas de findInFolder() - sécurité
    // Les fichiers sont sélectionnés via UI uniquement
}

// domain/port/out/SpotifyPort.java
public interface SpotifyPort {
    Optional<SpotifyTrackInfo> search(String artist, String title);
    AudioFeatures getAudioFeatures(SpotifyId id);
}

// domain/port/out/VectorStorePort.java
public interface VectorStorePort {
    void store(MusicFile file, float[] embedding);
    List<MusicFile> findSimilar(String query, int limit);
}

// domain/port/out/AIAgentPort.java
public interface AIAgentPort {
    Tags suggestTags(MusicFile file);
    TaggingPlan generatePlan(List<MusicFile> files);
}
```

## Use Cases (Dans Domain)

### Use Case Simple - Fichiers Autorisés
```java
// domain/usecase/ScanMusicUseCase.java
@Service
public class ScanMusicUseCase {
    
    private final AudioRepository audioRepository;
    
    public ScanMusicUseCase(AudioRepository audioRepository) {
        this.audioRepository = audioRepository;
    }
    
    // L'utilisateur sélectionne les fichiers via l'UI
    public List<MusicFile> execute(List<Filepath> authorizedFiles) {
        return authorizedFiles.stream()
            .map(audioRepository::findByPath)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }
}
```

### Use Case avec Logique Métier
```java
// domain/usecase/CreatePlanUseCase.java
@Service
public class CreatePlanUseCase {
    
    private final AudioRepository audioRepository;
    private final TaggingService taggingService;
    
    public CreatePlanUseCase(
        AudioRepository audioRepository,
        TaggingService taggingService
    ) {
        this.audioRepository = audioRepository;
        this.taggingService = taggingService;
    }
    
    // Fichiers pré-sélectionnés par l'utilisateur
    public TaggingPlan execute(List<Filepath> authorizedFiles) {
        List<MusicFile> files = authorizedFiles.stream()
            .map(audioRepository::findByPath)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
            
        return taggingService.createPlan(files);
    }
}
```

### Use Case avec Orchestration
```java
// domain/usecase/EnrichTagsUseCase.java
@Service
public class EnrichTagsUseCase {
    
    private final AudioRepository audioRepository;
    private final SpotifyPort spotifyPort;
    private final VectorStorePort vectorStorePort;
    
    public EnrichTagsUseCase(
        AudioRepository audioRepository,
        SpotifyPort spotifyPort,
        VectorStorePort vectorStorePort
    ) {
        this.audioRepository = audioRepository;
        this.spotifyPort = spotifyPort;
        this.vectorStorePort = vectorStorePort;
    }
    
    public EnrichmentResult execute(MusicFile file) {
        // 1. Recherche Spotify
        Optional<SpotifyTrackInfo> spotify = spotifyPort.search(
            file.tags().get(TagKey.ARTIST).value(),
            file.tags().get(TagKey.TITLE).value()
        );
        
        if (spotify.isEmpty()) {
            return EnrichmentResult.notFound();
        }
        
        // 2. Mettre à jour fichier
        SpotifyTrackInfo track = spotify.get();
        file.updateTag(TagKey.BPM, new TagValue(String.valueOf(track.bpm())));
        file.updateTag(TagKey.KEY, new TagValue(track.key()));
        
        // 3. Sauvegarder
        audioRepository.save(file);
        
        // 4. Vectoriser
        float[] embedding = createEmbedding(track);
        vectorStorePort.store(file, embedding);
        
        return EnrichmentResult.success(track);
    }
    
    private float[] createEmbedding(SpotifyTrackInfo track) {
        // Logique embedding
        return new float[0];
    }
}
```

### Use Case Complexe (Agent Ayan)
```java
// domain/usecase/AyanAgentUseCase.java
@Service
public class AyanAgentUseCase {
    
    private final ScanMusicUseCase scanMusicUseCase;
    private final EnrichTagsUseCase enrichTagsUseCase;
    private final CreatePlanUseCase createPlanUseCase;
    private final AIAgentPort aiAgent;
    
    public AyanAgentUseCase(
        ScanMusicUseCase scanMusicUseCase,
        EnrichTagsUseCase enrichTagsUseCase,
        CreatePlanUseCase createPlanUseCase,
        AIAgentPort aiAgent
    ) {
        this.scanMusicUseCase = scanMusicUseCase;
        this.enrichTagsUseCase = enrichTagsUseCase;
        this.createPlanUseCase = createPlanUseCase;
        this.aiAgent = aiAgent;
    }
    
    public TaggingPlan createIntelligentPlan(List<Filepath> authorizedFiles) {
        // 1. Scan
        List<MusicFile> files = scanMusicUseCase.execute(authorizedFiles);
        
        // 2. Enrichir ceux qui ont artist+title
        files.stream()
            .filter(file -> file.hasTag(TagKey.ARTIST) && file.hasTag(TagKey.TITLE))
            .forEach(enrichTagsUseCase::execute);
        
        // 3. Générer plan avec IA
        return aiAgent.generatePlan(files);
    }
}
```

## Infrastructure Layer

### Adapter Entrant - REST
```java
// infrastructure/adapter/in/rest/MusicController.java
@RestController
@RequestMapping("/api/music")
public class MusicController {
    
    private final ScanMusicUseCase scanMusicUseCase;
    
    public MusicController(ScanMusicUseCase scanMusicUseCase) {
        this.scanMusicUseCase = scanMusicUseCase;
    }
    
    @PostMapping("/scan")
    public ScanResponse scan(@RequestBody ScanRequest request) {
        // L'UI envoie la liste des fichiers sélectionnés
        List<Filepath> authorizedFiles = request.filepaths().stream()
            .map(Filepath::new)
            .toList();
            
        List<MusicFile> files = scanMusicUseCase.execute(authorizedFiles);
        
        return new ScanResponse(
            files.stream().map(this::toDto).toList()
        );
    }
    
    private MusicFileDto toDto(MusicFile file) {
        return new MusicFileDto(
            file.filepath().value(),
            file.tags().values()
        );
    }
}

// DTO
public record ScanRequest(List<String> filepaths) {}
```

### Adapter Entrant - MCP Tools
```java
// infrastructure/adapter/in/mcp/AyanMusicTools.java
@Component
public class AyanMusicTools {
    
    private final ScanMusicUseCase scanMusicUseCase;
    private final CreatePlanUseCase createPlanUseCase;
    private final GeneratePlaylistUseCase generatePlaylistUseCase;
    
    public AyanMusicTools(
        ScanMusicUseCase scanMusicUseCase,
        CreatePlanUseCase createPlanUseCase,
        GeneratePlaylistUseCase generatePlaylistUseCase
    ) {
        this.scanMusicUseCase = scanMusicUseCase;
        this.createPlanUseCase = createPlanUseCase;
        this.generatePlaylistUseCase = generatePlaylistUseCase;
    }
    
    @Tool(
        name = "scanMusicFiles",
        description = "Scanne les fichiers audio sélectionnés par l'utilisateur"
    )
    public ScanResult scanFiles(
        @ToolParam(description = "Liste des chemins de fichiers autorisés") 
        List<String> filepaths
    ) {
        List<Filepath> authorized = filepaths.stream()
            .map(Filepath::new)
            .toList();
            
        List<MusicFile> files = scanMusicUseCase.execute(authorized);
        return new ScanResult(files.size());
    }
    
    @Tool(
        name = "createPlan",
        description = "Crée plan tagging pour fichiers sélectionnés"
    )
    public PlanResult createPlan(
        @ToolParam(description = "Liste des fichiers à traiter") 
        List<String> filepaths
    ) {
        List<Filepath> authorized = filepaths.stream()
            .map(Filepath::new)
            .toList();
            
        TaggingPlan plan = createPlanUseCase.execute(authorized);
        return new PlanResult(plan.id().value(), plan.pendingCount());
    }
    
    @Tool(name = "generateHarmonicPlaylist", description = "Génère playlist harmonic mix")
    public PlaylistResult generatePlaylist(
        @ToolParam String genre,
        @ToolParam Integer minBpm,
        @ToolParam Integer maxBpm
    ) {
        PlaylistCriteria criteria = new PlaylistCriteria(genre, minBpm, maxBpm);
        HarmonicPlaylist playlist = generatePlaylistUseCase.execute(criteria);
        return new PlaylistResult(playlist.tracks().size());
    }
}
```

### Adapter Sortant - Persistence
```java
// infrastructure/adapter/out/persistence/JpaAudioRepository.java
@Repository
public class JpaAudioRepository implements AudioRepository {
    
    private final SpringDataMusicFileRepository jpaRepository;
    private final MusicFileMapper mapper;
    
    public JpaAudioRepository(
        SpringDataMusicFileRepository jpaRepository,
        MusicFileMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Optional<MusicFile> findByPath(Filepath path) {
        return jpaRepository.findByFilepath(path.value())
            .map(mapper::toDomain);
    }
    
    @Override
    public void save(MusicFile file) {
        MusicFileEntity entity = mapper.toEntity(file);
        jpaRepository.save(entity);
    }
}
```

### Adapter Sortant - Spotify
```java
// infrastructure/adapter/out/spotify/SpotifyAdapter.java
@Component
public class SpotifyAdapter implements SpotifyPort {
    
    private final SpotifyApiClient client;
    private final SpotifyMapper mapper;
    
    public SpotifyAdapter(SpotifyApiClient client, SpotifyMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }
    
    @Override
    public Optional<SpotifyTrackInfo> search(String artist, String title) {
        String query = String.format("artist:%s track:%s", artist, title);
        
        SpotifySearchResponse response = client.searchTracks(query, "track", 1);
        
        if (response.tracks().items().isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(mapper.toDomain(response.tracks().items().get(0)));
    }
    
    @Override
    public AudioFeatures getAudioFeatures(SpotifyId id) {
        SpotifyAudioFeatures features = client.getAudioFeatures(id.value());
        return mapper.toDomain(features);
    }
}
```

### Adapter Sortant - JAudiotagger
```java
// infrastructure/adapter/out/audio/JAudioTaggerAdapter.java
@Component
public class JAudioTaggerAdapter implements AudioRepository {
    
    @Override
    public Optional<MusicFile> findByPath(Filepath filepath) {
        try {
            AudioFile audioFile = AudioFileIO.read(new File(filepath.value()));
            Tag tag = audioFile.getTag();
            
            MusicFile file = new MusicFile(filepath);
            
            if (tag.getFirst(FieldKey.ARTIST) != null) {
                file.updateTag(
                    TagKey.ARTIST,
                    new TagValue(tag.getFirst(FieldKey.ARTIST))
                );
            }
            
            if (tag.getFirst(FieldKey.TITLE) != null) {
                file.updateTag(
                    TagKey.TITLE,
                    new TagValue(tag.getFirst(FieldKey.TITLE))
                );
            }
            
            return Optional.of(file);
            
        } catch (Exception e) {
            log.error("Failed to read file: {}", filepath.value(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public void save(MusicFile file) {
        try {
            AudioFile audioFile = AudioFileIO.read(new File(file.filepath().value()));
            Tag tag = audioFile.getTagOrCreateAndSetDefault();
            
            // Appliquer les tags
            file.tags().values().forEach((key, value) -> {
                try {
                    tag.setField(toFieldKey(key), value.value());
                } catch (Exception e) {
                    log.warn("Failed to set tag {}", key, e);
                }
            });
            
            audioFile.commit();
            
        } catch (Exception e) {
            throw new AudioSaveException("Failed to save file", e);
        }
    }
    
    private FieldKey toFieldKey(TagKey key) {
        return switch (key) {
            case ARTIST -> FieldKey.ARTIST;
            case TITLE -> FieldKey.TITLE;
            case ALBUM -> FieldKey.ALBUM;
            case BPM -> FieldKey.BPM;
            case KEY -> FieldKey.KEY;
            case GENRE -> FieldKey.GENRE;
        };
    }
}
```

## Configuration (Wiring)

```java
// infrastructure/config/DomainConfig.java
@Configuration
public class DomainConfig {
    
    // Services métier
    @Bean
    public TaggingService taggingService() {
        return new TaggingService();
    }
    
    @Bean
    public HarmonicMixingService harmonicMixingService() {
        return new HarmonicMixingService();
    }
    
    // Use cases (auto-wiring via @Service)
    // Pas besoin de @Bean, Spring les détecte automatiquement
}
```

## Tests Architecture (ArchUnit)

```java
// test/architecture/HexagonalArchTest.java
public class HexagonalArchTest {
    
    @Test
    void domainShouldNotDependOnInfrastructure() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .check(importedClasses);
    }
    
    @Test
    void portsShouldBeInterfaces() {
        classes()
            .that().resideInAPackage("..domain.port..")
            .should().beInterfaces()
            .check(importedClasses);
    }
    
    @Test
    void useCasesShouldBeAnnotatedWithService() {
        classes()
            .that().resideInAPackage("..domain.usecase..")
            .should().beAnnotatedWith(Service.class)
            .check(importedClasses);
    }
    
    @Test
    void valueObjectsShouldBeRecords() {
        classes()
            .that().resideInAPackage("..domain.model.vo..")
            .should().beRecords()
            .check(importedClasses);
    }
}
```

## Règles d'Or

1. **Domain pur** - Aucune dépendance externe (ni Spring, ni Hibernate, ni lib)
2. **Use cases dans domain** - Pas de couche application séparée
3. **Ports = interfaces sortantes** - Pour dépendances externes uniquement
4. **Adapters remplaçables** - Changez tech sans toucher domain
5. **Value Objects** - Immutables, validation dans constructeur
6. **Agrégats cohérents** - Frontières transactionnelles claires
7. **@Service sur use cases** - Auto-wiring Spring
8. **Sécurité fichiers** - JAMAIS de scan récursif, uniquement fichiers autorisés par UI

## Sécurité - Sélection Fichiers

⚠️ **CRITIQUE** : Le backend ne scanne **JAMAIS** de dossiers de façon récursive.

```java
// ❌ INTERDIT
public List<MusicFile> scanFolder(Filepath folder) {
    return Files.walk(folder).collect(...); // NON !
}

// ✅ AUTORISÉ
public List<MusicFile> scanFiles(List<Filepath> authorizedFiles) {
    return authorizedFiles.stream()
        .map(this::loadFile)
        .toList(); // OK - fichiers pré-approuvés
}
```

**Workflow** :
1. User clique "Sélectionner fichiers" (UI Electron)
2. File picker natif s'ouvre (dialog.showOpenDialog)
3. User sélectionne fichiers audio manuellement
4. UI envoie liste chemins → Backend
5. Backend traite UNIQUEMENT ces fichiers

## Checklist

- [ ] Domain ne dépend de rien
- [ ] Ports sortants sont interfaces
- [ ] Use cases annotés @Service
- [ ] Value Objects sont records
- [ ] Adapters implémentent ports
- [ ] Tests ArchUnit passent
- [ ] Logique métier dans domain/service
- [ ] Orchestration dans domain/usecase
- [ ] Tech dans infrastructure
- [ ] **Pas de scan récursif - fichiers pré-sélectionnés uniquement**
