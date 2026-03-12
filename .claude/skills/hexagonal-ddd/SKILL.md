---
name: hexagonal-ddd
description: Architecture hexagonale + DDD pour ce projet. Règles de séparation domain/infra, ports/adapters, value objects, use cases. Utiliser pour toute modification d'architecture, création de port, adapter, ou use case.
user-invocable: false
---

# Architecture Hexagonale + DDD Skill

Simple, concis, pragmatique

> Exemples d'adapters et tests ArchUnit : voir [examples.md](./examples.md)
> Reference rapide architecture : voir [reference.md](./reference.md)

## Principes

- **Domain** = coeur metier pur + use cases
- **Ports** = interfaces in et out
- **Adapters** = implementations tech
- **Pas de couche application** = use cases dans domain
- **Independance** = domain ne depend de rien

## Structure en 2 Couches

```
INFRASTRUCTURE (Adapters In + Out + Config)
  - REST Controllers, MCP Tools, JDBC Repositories, Spotify Client, JAudiotagger
              |
              | depend de
              v
DOMAIN
  - Model (Entites + Value Objects)
  - Service (logique metier)
  - UseCase (orchestration)
  - Port (interfaces dependances)
```

## Domain Layer

### Entites
```java
public class MusicFile {
    private final Filepath filepath;
    private Tags tags;

    public MusicFile(Filepath filepath) {
        this.filepath = filepath;
        this.tags = Tags.empty();
    }

    public void updateTag(TagKey key, TagValue value) {
        tags = tags.with(key, value);
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
public record Filepath(String value) {
    public Filepath {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Filepath blank");
        if (value.contains("..")) throw new IllegalArgumentException("Path traversal not allowed");
    }

    public String filename() {
        return value.substring(value.lastIndexOf('/') + 1);
    }
}

public record BPM(int value) {
    public BPM {
        if (value < 60 || value > 200) throw new IllegalArgumentException("BPM must be 60-200");
    }

    public boolean isCompatibleWith(BPM other) {
        return Math.abs(value - other.value) <= 6;
    }
}

public record MusicalKey(String key, Mode mode) {
    public enum Mode { MAJOR, MINOR }
    public CamelotKey toCamelot() { return CamelotKey.from(key, mode); }
}

public record Tags(Map<TagKey, TagValue> values) {
    public static Tags empty() { return new Tags(Map.of()); }

    public Tags with(TagKey key, TagValue value) {
        Map<TagKey, TagValue> updated = new HashMap<>(values);
        updated.put(key, value);
        return new Tags(Map.copyOf(updated));
    }

    public boolean contains(TagKey key) { return values.containsKey(key); }
}
```

### Domain Service
```java
public class TaggingService {
    public TaggingPlan createPlan(List<MusicFile> files) {
        List<TagOperation> operations = files.stream()
            .filter(file -> !file.missingTags().isEmpty())
            .map(this::createOperation)
            .toList();
        return new TaggingPlan(operations);
    }
}
```

### Agregat TaggingPlan
```java
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
        if (status != PlanStatus.READY_FOR_REVIEW) throw new IllegalStateException("Plan not ready");
        status = PlanStatus.APPROVED;
    }

    public void execute() {
        if (status != PlanStatus.APPROVED) throw new IllegalStateException("Plan not approved");
        status = PlanStatus.EXECUTING;
    }

    public int pendingCount() {
        return (int) operations.stream()
            .filter(op -> op.status() == OperationStatus.PENDING).count();
    }
}
```

## Ports (Interfaces Sortantes)

Les use cases sont des classes concretes dans `domain/usecase/`, pas des interfaces.
Seules les dependances externes sont des ports (interfaces).

```java
public interface AudioRepository {
    Optional<MusicFile> findByPath(Filepath path);
    void save(MusicFile file);
}

public interface SpotifyPort {
    Optional<SpotifyTrackInfo> search(String artist, String title);
    AudioFeatures getAudioFeatures(SpotifyId id);
}

public interface VectorStorePort {
    void store(MusicFile file, float[] embedding);
    List<MusicFile> findSimilar(String query, int limit);
}

public interface AIAgentPort {
    Tags suggestTags(MusicFile file);
    TaggingPlan generatePlan(List<MusicFile> files);
}
```

## Use Cases (Dans Domain)

```java
@Service
public class ScanMusicUseCase {
    private final AudioRepository audioRepository;

    public List<MusicFile> execute(List<Filepath> authorizedFiles) {
        return authorizedFiles.stream()
            .map(audioRepository::findByPath)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }
}

@Service
public class CreatePlanUseCase {
    private final AudioRepository audioRepository;
    private final TaggingService taggingService;

    public TaggingPlan execute(List<Filepath> authorizedFiles) {
        List<MusicFile> files = authorizedFiles.stream()
            .map(audioRepository::findByPath)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
        return taggingService.createPlan(files);
    }
}

@Service
public class EnrichTagsUseCase {
    private final AudioRepository audioRepository;
    private final SpotifyPort spotifyPort;
    private final VectorStorePort vectorStorePort;

    public EnrichmentResult execute(MusicFile file) {
        Optional<SpotifyTrackInfo> spotify = spotifyPort.search(
            file.tags().get(TagKey.ARTIST).value(),
            file.tags().get(TagKey.TITLE).value()
        );
        if (spotify.isEmpty()) return EnrichmentResult.notFound();

        SpotifyTrackInfo track = spotify.get();
        file.updateTag(TagKey.BPM, new TagValue(String.valueOf(track.bpm())));
        file.updateTag(TagKey.KEY, new TagValue(track.key()));
        audioRepository.save(file);

        float[] embedding = createEmbedding(track);
        vectorStorePort.store(file, embedding);
        return EnrichmentResult.success(track);
    }
}
```

## Configuration (Wiring)

```java
@Configuration
public class DomainConfig {
    @Bean
    public TaggingService taggingService() { return new TaggingService(); }

    @Bean
    public HarmonicMixingService harmonicMixingService() { return new HarmonicMixingService(); }
}
```

## Regles d'Or

1. **Domain pur** - Aucune dependance externe (ni Spring, ni Hibernate, ni lib)
2. **Use cases dans domain** - Pas de couche application separee
3. **Ports = interfaces sortantes** - Pour dependances externes uniquement
4. **Adapters remplacables** - Changez tech sans toucher domain
5. **Value Objects** - Immutables, validation dans constructeur
6. **Agregats coherents** - Frontieres transactionnelles claires
7. **@Service sur use cases** - Auto-wiring Spring
8. **Securite fichiers** - JAMAIS de scan recursif, uniquement fichiers autorises par UI

## Securite - Selection Fichiers

**CRITIQUE** : Le backend ne scanne **JAMAIS** de dossiers de facon recursive.

```java
// INTERDIT
public List<MusicFile> scanFolder(Filepath folder) {
    return Files.walk(folder).collect(...); // NON !
}

// AUTORISE
public List<MusicFile> scanFiles(List<Filepath> authorizedFiles) {
    return authorizedFiles.stream()
        .map(this::loadFile)
        .toList(); // OK - fichiers pre-approuves
}
```

**Workflow** :
1. User clique "Selectionner fichiers" (UI Electron)
2. File picker natif s'ouvre (dialog.showOpenDialog)
3. User selectionne fichiers audio manuellement
4. UI envoie liste chemins -> Backend
5. Backend traite UNIQUEMENT ces fichiers

## Checklist

- [ ] Domain ne depend de rien
- [ ] Ports sortants sont interfaces
- [ ] Use cases annotes @Service
- [ ] Value Objects sont records
- [ ] Adapters implementent ports
- [ ] Tests ArchUnit passent
- [ ] Logique metier dans domain/service
- [ ] Orchestration dans domain/usecase
- [ ] Tech dans infrastructure
- [ ] **Pas de scan recursif - fichiers pre-selectionnes uniquement**
