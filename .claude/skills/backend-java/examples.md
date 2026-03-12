# Backend Java — Exemples Complets

## 1. Nouvel Endpoint REST Complet

Record request → Controller → Service → Record response

```java
// --- Request/Response records ---
public record FileAnalysisRequest(
    @NotBlank String filepath,
    @NotNull List<String> requestedFields
) {
    public FileAnalysisRequest {
        Objects.requireNonNull(filepath, "filepath required");
        Objects.requireNonNull(requestedFields, "requestedFields required");
        if (filepath.contains("..")) throw new IllegalArgumentException("Path traversal");
    }
}

public record FileAnalysisResponse(
    String filepath,
    Map<String, String> fields,
    int missingCount,
    LocalDateTime analyzedAt
) {}

// --- Controller ---
@RestController
@RequestMapping("/api/analysis")
@Slf4j
public class AnalysisController {
    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    public ResponseEntity<FileAnalysisResponse> analyze(
            @RequestBody @Valid FileAnalysisRequest request) {
        return ResponseEntity.ok(analysisService.analyze(request));
    }

    @GetMapping("/batch")
    public ResponseEntity<List<FileAnalysisResponse>> analyzeBatch(
            @RequestParam List<String> filepaths) {
        return ResponseEntity.ok(
            filepaths.stream().map(fp ->
                analysisService.analyze(new FileAnalysisRequest(fp, List.of()))
            ).toList()
        );
    }
}

// --- Service ---
@Service
@Slf4j
public class AnalysisService {
    private final AudioFileReader audioFileReader;

    public AnalysisService(AudioFileReader audioFileReader) {
        this.audioFileReader = audioFileReader;
    }

    public FileAnalysisResponse analyze(FileAnalysisRequest request) {
        MusicFileInfo info = audioFileReader.readTags(request.filepath());
        Map<String, String> fields = extractRequestedFields(info, request.requestedFields());
        int missingCount = (int) fields.values().stream().filter(Objects::isNull).count();
        return new FileAnalysisResponse(request.filepath(), fields, missingCount, LocalDateTime.now());
    }

    private Map<String, String> extractRequestedFields(MusicFileInfo info, List<String> requested) {
        Map<String, String> result = new LinkedHashMap<>();
        if (requested.isEmpty() || requested.contains("artist")) result.put("artist", info.artist());
        if (requested.isEmpty() || requested.contains("title")) result.put("title", info.title());
        if (requested.isEmpty() || requested.contains("bpm")) result.put("bpm",
            info.bpm() != null ? String.valueOf(info.bpm()) : null);
        return result;
    }
}
```

## 2. Test d'Integration MockMvc + Testcontainers

```java
@SpringBootTest(classes = {
    AnalysisController.class,
    AnalysisService.class,
    JAudioTaggerAdapter.class,
    DomainConfig.class
})
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
    OllamaApiAutoConfiguration.class,
    OllamaChatAutoConfiguration.class,
    OllamaEmbeddingAutoConfiguration.class
})
class AnalysisControllerIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void analyze_shouldReturn200_whenFileValid() throws Exception {
        // Given — fichier test cree par TestAudioFileHelper
        Path testFile = TestAudioFileHelper.createTestMp3("TestArtist", "TestTitle");

        FileAnalysisRequest request = new FileAnalysisRequest(
            testFile.toString(), List.of("artist", "title"));

        // When & Then
        mockMvc.perform(post("/api/analysis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.filepath").value(testFile.toString()))
            .andExpect(jsonPath("$.fields.artist").value("TestArtist"))
            .andExpect(jsonPath("$.missingCount").value(0));
    }

    @Test
    void analyze_shouldReturn400_whenPathTraversal() throws Exception {
        FileAnalysisRequest request = new FileAnalysisRequest(
            "../../../etc/passwd", List.of());

        mockMvc.perform(post("/api/analysis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
```

## 3. Record avec Compact Constructor et Methodes Utilitaires

```java
public record TrackCompatibility(
    String filepathA,
    String filepathB,
    int bpmDifference,
    boolean harmonicMatch,
    double energyDifference,
    double overallScore
) {
    // Compact constructor — validation
    public TrackCompatibility {
        Objects.requireNonNull(filepathA, "filepathA required");
        Objects.requireNonNull(filepathB, "filepathB required");
        if (bpmDifference < 0) throw new IllegalArgumentException("bpmDifference must be >= 0");
        if (overallScore < 0 || overallScore > 1)
            throw new IllegalArgumentException("overallScore must be 0.0-1.0");
    }

    // Factory methods
    public static TrackCompatibility compute(MusicFileInfo a, MusicFileInfo b,
            boolean harmonic, double energy) {
        int bpmDiff = Math.abs(
            (a.bpm() != null ? a.bpm() : 0) - (b.bpm() != null ? b.bpm() : 0));
        double score = computeScore(bpmDiff, harmonic, energy);
        return new TrackCompatibility(
            a.filepath(), b.filepath(), bpmDiff, harmonic, energy, score);
    }

    // Methodes utilitaires
    public boolean isHighlyCompatible() { return overallScore >= 0.8; }

    public String compatibilityLevel() {
        if (overallScore >= 0.8) return "EXCELLENT";
        if (overallScore >= 0.6) return "GOOD";
        if (overallScore >= 0.4) return "MODERATE";
        return "LOW";
    }

    // withX pour immutable updates
    public TrackCompatibility withScore(double newScore) {
        return new TrackCompatibility(filepathA, filepathB, bpmDifference,
            harmonicMatch, energyDifference, newScore);
    }

    private static double computeScore(int bpmDiff, boolean harmonic, double energyDiff) {
        double bpmScore = Math.max(0, 1.0 - (bpmDiff / 20.0));
        double harmonicScore = harmonic ? 1.0 : 0.3;
        double energyScore = Math.max(0, 1.0 - energyDiff);
        return (bpmScore * 0.4 + harmonicScore * 0.4 + energyScore * 0.2);
    }
}
```
