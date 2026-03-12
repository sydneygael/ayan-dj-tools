---
name: backend-java
description: Patterns Java 25 + Spring Boot 4.0.2 + Gradle 9.2. Records, services, controllers, Spring JDBC repositories, configuration, tests. Utiliser pour tout code backend Java.
user-invocable: false
---

# Backend Java Skill

Bonnes pratiques Java 25 + Spring Boot 4.0.2 + Gradle

> Patterns Spring JDBC Repository : voir [jdbc-patterns.md](./jdbc-patterns.md)
> Reference rapide API : voir [reference.md](./reference.md)
> Exemples complets : voir [examples.md](./examples.md)

## Principes

- **Records partout** pour DTOs/responses
- **Noms explicites** > commentaires
- **Concision extreme**
- **Validation stricte** dans compact constructors
- **Immutabilite** par defaut
- **Spring JDBC > Spring Data** - SQL explicite, pas de magie JPA

## Records Pattern

```java
public record MusicFileInfo(
        @NotBlank String filepath, String artist, String title,
        @Min(0) @Max(300) Integer bpm
) {
    public MusicFileInfo {
        Objects.requireNonNull(filepath, "filepath required");
        if (filepath.isBlank()) throw new IllegalArgumentException("filepath blank");
    }

    public boolean hasArtistAndTitle() {
        return artist != null && !artist.isBlank() && title != null && !title.isBlank();
    }
}

public record TaggingPlan(String folderPath, List<TagOperation> operations, PlanStatus status) {
    public enum PlanStatus { DRAFT, READY, APPROVED, COMPLETED }

    public TaggingPlan withStatus(PlanStatus newStatus) {
        return new TaggingPlan(folderPath, operations, newStatus);
    }
}
```

## Services Pattern

```java
@Service
@Slf4j
public class AudioTagService {
    private final AudioScannerService scanner;
    private final TaggingHistoryRepository historyRepo;

    // Constructor injection (pas @Autowired)
    public AudioTagService(AudioScannerService scanner, TaggingHistoryRepository historyRepo) {
        this.scanner = scanner;
        this.historyRepo = historyRepo;
    }

    public MusicFileInfo readTags(String filepath) {
        validateFilePath(filepath);
        try {
            AudioFile audioFile = AudioFileIO.read(new File(filepath));
            Tag tag = audioFile.getTag();
            return new MusicFileInfo(filepath, tag.getFirst(FieldKey.ARTIST),
                tag.getFirst(FieldKey.TITLE), parseBpm(tag.getFirst(FieldKey.BPM)));
        } catch (Exception e) {
            log.error("Failed read tags: {}", filepath, e);
            throw new AudioProcessingException("Read failed", e);
        }
    }

    private void validateFilePath(String path) {
        if (path.contains("..")) throw new IllegalArgumentException("Invalid path");
    }
}
```

### Noms de methodes explicites

```java
// Mauvais: public void process(String path) { ... }
// Bon:
public void scanFolderAndDetectMissingTags(String path) { ... }
public MissingTagsReport detectMissingTagsForFile(String filepath) { ... }
public boolean isFileWritableAndNotCorrupted(String filepath) { ... }
```

## Controllers Pattern

```java
@RestController
@RequestMapping("/api/music")
@Slf4j
public class MusicFileController {
    private final AudioTagService audioService;

    @PostMapping("/scan")
    public ResponseEntity<ScanResult> scanFolder(@RequestBody @Valid ScanRequest request) {
        try {
            List<MusicFileInfo> files = audioService.scanFolder(request.folderPath());
            return ResponseEntity.ok(new ScanResult(files, files.size(), countMissingTags(files)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

public record ScanRequest(@NotBlank String folderPath) {}
public record ScanResult(List<MusicFileInfo> files, int totalFiles, int filesWithMissingTags) {}
```

### Gestion erreurs
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AudioProcessingException.class)
    public ResponseEntity<ErrorResponse> handleAudioError(AudioProcessingException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("AUDIO_ERROR", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidation(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
    }
}

public record ErrorResponse(String code, String message) {}
```

## Configuration Pattern

```java
@ConfigurationProperties(prefix = "dj-tagger")
public record DJTaggerProperties(Audio audio, Agent agent, Rag rag) {
    public record Audio(List<String> supportedFormats, int maxFileSizeMb) {}
    public record Agent(String defaultMode, int batchSize, double confidenceThreshold) {}
    public record Rag(double similarityThreshold, int maxSimilarTracks) {}
}
```

## Tests Pattern

```java
@ExtendWith(MockitoExtension.class)
class AudioTagServiceTest {
    @Mock private AudioScannerService scanner;
    @Mock private TaggingHistoryRepository historyRepo;
    @InjectMocks private AudioTagService service;

    @Test
    void readTags_shouldReturnMusicFileInfo_whenFileValid() {
        MusicFileInfo result = service.readTags("/music/test.mp3");
        assertNotNull(result);
    }

    @Test
    void readTags_shouldThrowException_whenPathInvalid() {
        assertThrows(IllegalArgumentException.class, () -> service.readTags("../../../etc/passwd"));
    }
}

@SpringBootTest
@AutoConfigureMockMvc
class MusicFileControllerIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void scanFolder_shouldReturn200_whenValidPath() throws Exception {
        mockMvc.perform(post("/api/music/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ScanRequest("/music/test"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFiles").exists());
    }
}
```

## Gradle Configuration

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.2'
    id 'io.spring.dependency-management' version '1.1.7'
}

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-jdbc'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    runtimeOnly 'org.postgresql:postgresql'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

test { useJUnitPlatform() }
```

## Checklist Code Review

- [ ] Records pour tous DTOs/responses
- [ ] Validation dans compact constructors
- [ ] Noms methodes explicites (pas commentaires)
- [ ] Constructor injection (pas @Autowired)
- [ ] Gestion erreurs avec exceptions custom
- [ ] Spring JDBC uniquement — pas de JPA
- [ ] `JdbcClient` par defaut, `JdbcTemplate` pour batch/keys
- [ ] SQL en text blocks `"""`
- [ ] Transactions explicites
- [ ] Tests unitaires ecrits
- [ ] Concision maximale
