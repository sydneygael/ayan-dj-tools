# Hexagonal DDD — Exemples Adapters & Tests

## Adapter Entrant - REST
```java
@RestController
@RequestMapping("/api/music")
public class MusicController {
    private final ScanMusicUseCase scanMusicUseCase;

    @PostMapping("/scan")
    public ScanResponse scan(@RequestBody ScanRequest request) {
        List<Filepath> authorizedFiles = request.filepaths().stream()
            .map(Filepath::new).toList();
        List<MusicFile> files = scanMusicUseCase.execute(authorizedFiles);
        return new ScanResponse(files.stream().map(this::toDto).toList());
    }
}

public record ScanRequest(List<String> filepaths) {}
```

## Adapter Entrant - MCP Tools
```java
@Component
public class AyanMusicTools {
    private final ScanMusicUseCase scanMusicUseCase;
    private final CreatePlanUseCase createPlanUseCase;

    @Tool(name = "scanMusicFiles", description = "Scanne les fichiers audio selectionnes")
    public ScanResult scanFiles(
        @ToolParam(description = "Liste des chemins de fichiers autorises") List<String> filepaths
    ) {
        List<Filepath> authorized = filepaths.stream().map(Filepath::new).toList();
        return new ScanResult(scanMusicUseCase.execute(authorized).size());
    }

    @Tool(name = "createPlan", description = "Cree plan tagging pour fichiers selectionnes")
    public PlanResult createPlan(
        @ToolParam(description = "Liste des fichiers a traiter") List<String> filepaths
    ) {
        List<Filepath> authorized = filepaths.stream().map(Filepath::new).toList();
        TaggingPlan plan = createPlanUseCase.execute(authorized);
        return new PlanResult(plan.id().value(), plan.pendingCount());
    }
}
```

## Adapter Sortant - Persistence
```java
@Repository
public class JpaAudioRepository implements AudioRepository {
    private final SpringDataMusicFileRepository jpaRepository;
    private final MusicFileMapper mapper;

    @Override
    public Optional<MusicFile> findByPath(Filepath path) {
        return jpaRepository.findByFilepath(path.value()).map(mapper::toDomain);
    }

    @Override
    public void save(MusicFile file) {
        jpaRepository.save(mapper.toEntity(file));
    }
}
```

## Adapter Sortant - Spotify
```java
@Component
public class SpotifyAdapter implements SpotifyPort {
    private final SpotifyApiClient client;
    private final SpotifyMapper mapper;

    @Override
    public Optional<SpotifyTrackInfo> search(String artist, String title) {
        String query = String.format("artist:%s track:%s", artist, title);
        SpotifySearchResponse response = client.searchTracks(query, "track", 1);
        if (response.tracks().items().isEmpty()) return Optional.empty();
        return Optional.of(mapper.toDomain(response.tracks().items().get(0)));
    }

    @Override
    public AudioFeatures getAudioFeatures(SpotifyId id) {
        return mapper.toDomain(client.getAudioFeatures(id.value()));
    }
}
```

## Adapter Sortant - JAudiotagger
```java
@Component
public class JAudioTaggerAdapter implements AudioRepository {

    @Override
    public Optional<MusicFile> findByPath(Filepath filepath) {
        try {
            AudioFile audioFile = AudioFileIO.read(new File(filepath.value()));
            Tag tag = audioFile.getTag();
            MusicFile file = new MusicFile(filepath);
            if (tag.getFirst(FieldKey.ARTIST) != null)
                file.updateTag(TagKey.ARTIST, new TagValue(tag.getFirst(FieldKey.ARTIST)));
            if (tag.getFirst(FieldKey.TITLE) != null)
                file.updateTag(TagKey.TITLE, new TagValue(tag.getFirst(FieldKey.TITLE)));
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
            file.tags().values().forEach((key, value) -> {
                try { tag.setField(toFieldKey(key), value.value()); }
                catch (Exception e) { log.warn("Failed to set tag {}", key, e); }
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

## Tests Architecture (ArchUnit)

```java
public class HexagonalArchTest {

    @Test
    void domainShouldNotDependOnInfrastructure() {
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .check(importedClasses);
    }

    @Test
    void portsShouldBeInterfaces() {
        classes().that().resideInAPackage("..domain.port..")
            .should().beInterfaces().check(importedClasses);
    }

    @Test
    void useCasesShouldBeAnnotatedWithService() {
        classes().that().resideInAPackage("..domain.usecase..")
            .should().beAnnotatedWith(Service.class).check(importedClasses);
    }

    @Test
    void valueObjectsShouldBeRecords() {
        classes().that().resideInAPackage("..domain.model.vo..")
            .should().beRecords().check(importedClasses);
    }
}
```
