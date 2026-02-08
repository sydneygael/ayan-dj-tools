# Backend Java Skill

Bonnes pratiques Java 25 + Spring Boot 4.0.2 + Gradle

## Principes

- **Records partout** pour DTOs/responses
- **Noms explicites** > commentaires
- **Concision extrême**
- **Validation stricte** dans compact constructors
- **Immutabilité** par défaut
- **Spring JDBC > Spring Data** - SQL explicite, pas de magie JPA

## Structure Projet

```
src/main/java/com/ayan/djtools/musictagger/
├── config/          # Configurations Spring
├── controller/      # REST endpoints
├── service/         # Logique métier
├── mcp/            # @Tool functions
├── client/         # @HttpExchange interfaces
├── model/
│   └── record/     # Records (DTOs, Value Objects)
├── repository/     # Spring JDBC repositories
└── exception/      # Exceptions custom
```

## Records Pattern

### Définition
```java
public record MusicFileInfo(
        @NotBlank String filepath,
        String artist,
        String title,
        @Min(0) @Max(300) Integer bpm
) {
    // Compact constructor - validation
    public MusicFileInfo {
        Objects.requireNonNull(filepath, "filepath required");
        if (filepath.isBlank()) throw new IllegalArgumentException("filepath blank");
    }

    // Méthodes utilitaires
    public boolean hasArtistAndTitle() {
        return artist != null && !artist.isBlank()
                && title != null && !title.isBlank();
    }
}
```

### Records imbriqués
```java
public record TaggingPlan(
    String folderPath,
    List<TagOperation> operations,
    PlanStatus status
) {
    public enum PlanStatus { DRAFT, READY, APPROVED, COMPLETED }

    // Copie immutable modifiée
    public TaggingPlan withStatus(PlanStatus newStatus) {
        return new TaggingPlan(folderPath, operations, newStatus);
    }
}
```

### Builder pour records complexes
```java
public record TagSuggestions(...) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String filepath;
        private String artist;
    
        public Builder filepath(String val) { 
            filepath = val; 
            return this; 
        }
    
        public TagSuggestions build() {
            return new TagSuggestions(filepath, artist, ...);
        }
    }
}
```

## Services Pattern

### Structure
```java
@Service
@Slf4j
public class AudioTagService {

    private final AudioScannerService scanner;
    private final TaggingHistoryRepository historyRepo;

    // Constructor injection (pas @Autowired)
    public AudioTagService(
        AudioScannerService scanner,
        TaggingHistoryRepository historyRepo
    ) {
        this.scanner = scanner;
        this.historyRepo = historyRepo;
    }

    public MusicFileInfo readTags(String filepath) {
        validateFilePath(filepath);
    
        try {
            AudioFile audioFile = AudioFileIO.read(new File(filepath));
            Tag tag = audioFile.getTag();
        
            return new MusicFileInfo(
                filepath,
                tag.getFirst(FieldKey.ARTIST),
                tag.getFirst(FieldKey.TITLE),
                parseBpm(tag.getFirst(FieldKey.BPM))
            );
        } catch (Exception e) {
            log.error("Failed read tags: {}", filepath, e);
            throw new AudioProcessingException("Read failed", e);
        }
    }

    private void validateFilePath(String path) {
        if (path.contains("..")) {
            throw new IllegalArgumentException("Invalid path");
        }
    }

    private Integer parseBpm(String bpmStr) {
        if (bpmStr == null || bpmStr.isBlank()) return null;
        try {
            return Integer.parseInt(bpmStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
```

### Noms de méthodes explicites

❌ **Mauvais**
```java
public void process(String path) { ... }
```

✅ **Bon**
```java
public void scanFolderAndDetectMissingTags(String path) { ... }
public MissingTagsReport detectMissingTagsForFile(String filepath) { ... }
public boolean isFileWritableAndNotCorrupted(String filepath) { ... }
```

## Controllers Pattern

### REST Controller
```java
@RestController
@RequestMapping("/api/music")
@Slf4j
public class MusicFileController {

    private final AudioTagService audioService;

    public MusicFileController(AudioTagService audioService) {
        this.audioService = audioService;
    }

    @PostMapping("/scan")
    public ResponseEntity<ScanResult> scanFolder(
        @RequestBody @Valid ScanRequest request
    ) {
        log.info("Scan folder: {}", request.folderPath());
    
        try {
            List<MusicFileInfo> files = audioService.scanFolder(request.folderPath());
        
            return ResponseEntity.ok(new ScanResult(
                files,
                files.size(),
                countMissingTags(files)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private int countMissingTags(List<MusicFileInfo> files) {
        return (int) files.stream()
            .filter(f -> !f.hasArtistAndTitle())
            .count();
    }
}

public record ScanRequest(@NotBlank String folderPath) {}

public record ScanResult(
    List<MusicFileInfo> files,
    int totalFiles,
    int filesWithMissingTags
) {}
```

### Gestion erreurs
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AudioProcessingException.class)
    public ResponseEntity<ErrorResponse> handleAudioError(
        AudioProcessingException e
    ) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("AUDIO_ERROR", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        IllegalArgumentException e
    ) {
        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
    }
}

public record ErrorResponse(String code, String message) {}
```

## Configuration Pattern

### Application Config
```java
@Configuration
public class AppConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:4200")
                    .allowedMethods("GET", "POST", "PUT", "DELETE");
            }
        };
    }
}
```

### Properties avec @ConfigurationProperties
```java
@ConfigurationProperties(prefix = "dj-tagger")
public record DJTaggerProperties(
    Audio audio,
    Agent agent,
    Rag rag
) {
    public record Audio(
        List<String> supportedFormats,
        int maxFileSizeMb
    ) {}

    public record Agent(
        String defaultMode,
        int batchSize,
        double confidenceThreshold
    ) {}

    public record Rag(
        double similarityThreshold,
        int maxSimilarTracks
    ) {}
}

@SpringBootApplication
@EnableConfigurationProperties(DJTaggerProperties.class)
public class MusicTaggerApplication { ... }
```

## Repository Pattern (Spring JDBC)

**Spring JDBC uniquement** — SQL explicite, contrôle total, zéro magie.

### Conventions

- Toujours `JdbcClient` (moderne) ou `JdbcTemplate` (classique)
- RowMapper en méthode privée ou lambda
- `Optional` pour les requêtes single-result
- SQL en text blocks `"""`
- `ON CONFLICT` / `MERGE` pour les upserts
- Pas d'entité JPA, pas de `@Entity`, pas de `@Table`

### Repository avec JdbcClient (recommandé Spring Boot 4.x)
```java
@Repository
public class SpotifyTrackRepository {

    private final JdbcClient jdbcClient;

    public SpotifyTrackRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<SpotifyTrackData> findBySpotifyId(String spotifyId) {
        String sql = """
            SELECT spotify_id, local_file_path, artist, title, bpm, 
                   musical_key, release_year, popularity
            FROM spotify_tracks
            WHERE spotify_id = ?
            """;

        return jdbcClient.sql(sql)
            .param(spotifyId)
            .query(this::mapRow)
            .optional();
    }

    public List<SpotifyTrackData> findByArtistAndYearRange(
        String artist,
        int yearStart,
        int yearEnd
    ) {
        String sql = """
            SELECT spotify_id, local_file_path, artist, title, bpm,
                   musical_key, release_year, popularity
            FROM spotify_tracks
            WHERE artist = ?
            AND release_year BETWEEN ? AND ?
            ORDER BY popularity DESC
            """;

        return jdbcClient.sql(sql)
            .params(artist, yearStart, yearEnd)
            .query(this::mapRow)
            .list();
    }

    public long countByArtist(String artist) {
        String sql = "SELECT COUNT(*) FROM spotify_tracks WHERE artist = ?";

        return jdbcClient.sql(sql)
            .param(artist)
            .query(Long.class)
            .single();
    }

    public boolean existsBySpotifyId(String spotifyId) {
        String sql = "SELECT EXISTS(SELECT 1 FROM spotify_tracks WHERE spotify_id = ?)";

        return jdbcClient.sql(sql)
            .param(spotifyId)
            .query(Boolean.class)
            .single();
    }

    public void save(SpotifyTrackData track) {
        String sql = """
            INSERT INTO spotify_tracks (
                spotify_id, local_file_path, artist, title, bpm,
                musical_key, release_year, popularity
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (spotify_id) DO UPDATE SET
                local_file_path = EXCLUDED.local_file_path,
                artist = EXCLUDED.artist,
                title = EXCLUDED.title,
                bpm = EXCLUDED.bpm,
                musical_key = EXCLUDED.musical_key,
                release_year = EXCLUDED.release_year,
                popularity = EXCLUDED.popularity
            """;

        jdbcClient.sql(sql)
            .params(
                track.spotifyId(),
                track.localFilePath(),
                track.artist(),
                track.title(),
                track.bpm(),
                track.musicalKey(),
                track.releaseYear(),
                track.popularity()
            )
            .update();
    }

    public void deleteBySpotifyId(String spotifyId) {
        jdbcClient.sql("DELETE FROM spotify_tracks WHERE spotify_id = ?")
            .param(spotifyId)
            .update();
    }

    private SpotifyTrackData mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SpotifyTrackData(
            rs.getString("spotify_id"),
            rs.getString("local_file_path"),
            rs.getString("artist"),
            rs.getString("title"),
            rs.getInt("bpm"),
            rs.getString("musical_key"),
            rs.getInt("release_year"),
            rs.getInt("popularity")
        );
    }
}
```

### Repository avec JdbcTemplate (alternative classique)
```java
@Repository
public class TaggingHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public TaggingHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<TaggingHistory> findById(long id) {
        String sql = """
            SELECT id, filepath, operation, status, created_at
            FROM tagging_history
            WHERE id = ?
            """;

        try {
            TaggingHistory result = jdbcTemplate.queryForObject(sql, this::mapRow, id);
            return Optional.ofNullable(result);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<TaggingHistory> findRecentByStatus(String status, int limit) {
        String sql = """
            SELECT id, filepath, operation, status, created_at
            FROM tagging_history
            WHERE status = ?
            ORDER BY created_at DESC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, this::mapRow, status, limit);
    }

    public long save(TaggingHistory history) {
        var keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement("""
                INSERT INTO tagging_history (filepath, operation, status, created_at)
                VALUES (?, ?, ?, ?)
                """,
                new String[]{"id"}
            );
            ps.setString(1, history.filepath());
            ps.setString(2, history.operation());
            ps.setString(3, history.status());
            ps.setTimestamp(4, Timestamp.valueOf(history.createdAt()));
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    private TaggingHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TaggingHistory(
            rs.getLong("id"),
            rs.getString("filepath"),
            rs.getString("operation"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
```

### Batch Operations
```java
public void saveBatch(List<SpotifyTrackData> tracks) {
    String sql = """
        INSERT INTO spotify_tracks (
            spotify_id, local_file_path, artist, title, bpm, musical_key
        ) VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT (spotify_id) DO UPDATE SET
            local_file_path = EXCLUDED.local_file_path,
            artist = EXCLUDED.artist,
            title = EXCLUDED.title
        """;

    jdbcTemplate.batchUpdate(
        sql,
        tracks,
        tracks.size(),
        (ps, track) -> {
            ps.setString(1, track.spotifyId());
            ps.setString(2, track.localFilePath());
            ps.setString(3, track.artist());
            ps.setString(4, track.title());
            ps.setInt(5, track.bpm());
            ps.setString(6, track.musicalKey());
        }
    );
}
```

### Transactions explicites
```java
@Service
public class TrackImportService {

    private final TransactionTemplate txTemplate;
    private final SpotifyTrackRepository trackRepo;
    private final TaggingHistoryRepository historyRepo;

    public TrackImportService(
        PlatformTransactionManager txManager,
        SpotifyTrackRepository trackRepo,
        TaggingHistoryRepository historyRepo
    ) {
        this.txTemplate = new TransactionTemplate(txManager);
        this.trackRepo = trackRepo;
        this.historyRepo = historyRepo;
    }

    // Transaction programmatique — visible et contrôlée
    public void importTrackWithHistory(SpotifyTrackData track) {
        txTemplate.executeWithoutResult(status -> {
            trackRepo.save(track);
            historyRepo.save(new TaggingHistory(
                null, track.localFilePath(), "IMPORT", "COMPLETED",
                LocalDateTime.now()
            ));
        });
    }

    // @Transactional aussi accepté pour les cas simples
    @Transactional
    public void deleteTrackWithHistory(String spotifyId) {
        trackRepo.deleteBySpotifyId(spotifyId);
        historyRepo.save(new TaggingHistory(
            null, spotifyId, "DELETE", "COMPLETED",
            LocalDateTime.now()
        ));
    }
}
```

### Choix JdbcClient vs JdbcTemplate

| Critère             | JdbcClient ✅          | JdbcTemplate           |
|---------------------|------------------------|------------------------|
| API                 | Fluent, moderne        | Classique, callback    |
| `Optional` natif    | ✅ `.optional()`       | ❌ try/catch manuel    |
| Batch               | ❌ Non supporté        | ✅ `batchUpdate()`     |
| GeneratedKeys       | ❌ Limité              | ✅ `KeyHolder`         |
| Cas simple (CRUD)   | **Préféré**            | Ok                     |
| Cas complexe        | Ok                     | **Préféré**            |

**Règle : `JdbcClient` par défaut, `JdbcTemplate` quand batch/keys/callbacks nécessaires.**

**Pourquoi Spring JDBC ?**
- ✅ SQL visible et contrôlé
- ✅ Pas de lazy loading ni sessions
- ✅ Pas de problèmes N+1
- ✅ Performances prévisibles
- ✅ Tests simples avec vraie DB
- ✅ `JdbcClient` API fluent et concise
- ❌ Spring Data JPA : Magie cachée, problèmes de perf

## Tests Pattern

### Tests unitaires
```java
@ExtendWith(MockitoExtension.class)
class AudioTagServiceTest {

    @Mock
    private AudioScannerService scanner;

    @Mock
    private TaggingHistoryRepository historyRepo;

    @InjectMocks
    private AudioTagService service;

    @Test
    void readTags_shouldReturnMusicFileInfo_whenFileValid() {
        String filepath = "/music/test.mp3";
        MusicFileInfo result = service.readTags(filepath);
        assertNotNull(result);
        assertEquals(filepath, result.filepath());
    }

    @Test
    void readTags_shouldThrowException_whenPathInvalid() {
        String invalidPath = "../../../etc/passwd";
        assertThrows(
            IllegalArgumentException.class,
            () -> service.readTags(invalidPath)
        );
    }
}
```

### Tests Repository (intégration DB)
```java
@JdbcTest
@Import(SpotifyTrackRepository.class)
class SpotifyTrackRepositoryTest {

    @Autowired
    private SpotifyTrackRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void save_thenFindBySpotifyId_shouldReturnTrack() {
        var track = new SpotifyTrackData(
            "spotify:123", "/music/test.mp3", "Artist", "Title",
            128, "Am", 2024, 85
        );

        repository.save(track);

        Optional<SpotifyTrackData> found = repository.findBySpotifyId("spotify:123");
        assertThat(found).isPresent();
        assertThat(found.get().artist()).isEqualTo("Artist");
    }

    @Test
    void findBySpotifyId_shouldReturnEmpty_whenNotExists() {
        Optional<SpotifyTrackData> found = repository.findBySpotifyId("unknown");
        assertThat(found).isEmpty();
    }
}
```

### Tests d'intégration
```java
@SpringBootTest
@AutoConfigureMockMvc
class MusicFileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void scanFolder_shouldReturn200_whenValidPath() throws Exception {
        ScanRequest request = new ScanRequest("/music/test");
    
        mockMvc.perform(post("/api/music/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFiles").exists());
    }
}
```

## Gradle Configuration

### gradle-wrapper.properties
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.2.1-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### build.gradle
```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.2'
    id 'io.spring.dependency-management' version '1.1.7'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // Spring Boot starters — JDBC, pas JPA
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-jdbc'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // Database
    runtimeOnly 'org.postgresql:postgresql'

    // Lombok (optionnel avec records)
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Tests
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'com.h2database:h2'
}

test {
    useJUnitPlatform()
}
```

## Checklist Code Review

- [ ] Records pour tous DTOs/responses
- [ ] Validation dans compact constructors
- [ ] Noms méthodes explicites (pas commentaires)
- [ ] Constructor injection (pas @Autowired)
- [ ] Gestion erreurs avec exceptions custom
- [ ] Logging approprié (log.info/error)
- [ ] Spring JDBC uniquement — pas de JPA, pas de @Entity
- [ ] `JdbcClient` par défaut, `JdbcTemplate` pour batch/keys
- [ ] SQL en text blocks `"""`
- [ ] Transactions explicites (programmatiques ou @Transactional)
- [ ] Tests unitaires écrits
- [ ] Tests Repository avec @JdbcTest
- [ ] Pas de code mort
- [ ] Concision maximale
```
