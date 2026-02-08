# Audio Processing Skill

JAudiotagger + File Scanning + Tags Manipulation

## Principes

- **JAudiotagger** pour lecture/écriture tags
- **Validation stricte** chemins fichiers
- **Backup** avant modifications
- **Atomic operations** pour sécurité

## Dependencies

```gradle
dependencies {
    implementation 'net.jthink:jaudiotagger:3.0.1'
}
```

## Formats Supportés

- **MP3** (ID3v1, ID3v2)
- **FLAC**
- **WAV**
- **AIFF**
- **M4A** (MP4/AAC)
- **OGG Vorbis**

## Service Scan

### Scanner récursif
```java
@Service
@Slf4j
public class AudioScannerService {
    
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        "mp3", "flac", "wav", "aiff", "m4a", "ogg"
    );
    
    @Value("${dj-tagger.audio.max-file-size-mb}")
    private int maxFileSizeMb;
    
    public List<MusicFileInfo> scanFolder(String folderPath) {
        validateFolderPath(folderPath);
        
        Path path = Paths.get(folderPath);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new IllegalArgumentException("Invalid folder: " + folderPath);
        }
        
        try (Stream<Path> paths = Files.walk(path)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(this::isSupportedAudioFile)
                .filter(this::isNotTooLarge)
                .map(this::pathToFileInfo)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        } catch (IOException e) {
            log.error("Scan failed: {}", folderPath, e);
            throw new AudioProcessingException("Scan failed", e);
        }
    }
    
    private boolean isSupportedAudioFile(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream()
            .anyMatch(ext -> filename.endsWith("." + ext));
    }
    
    private boolean isNotTooLarge(Path path) {
        try {
            long sizeMb = Files.size(path) / (1024 * 1024);
            return sizeMb <= maxFileSizeMb;
        } catch (IOException e) {
            return false;
        }
    }
    
    private Optional<MusicFileInfo> pathToFileInfo(Path path) {
        try {
            return Optional.of(new MusicFileInfo(
                path.toString(),
                path.getFileName().toString(),
                null, null, null, null, null, null,
                Files.size(path),
                Files.getLastModifiedTime(path).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
            ));
        } catch (IOException e) {
            log.warn("Cannot read file info: {}", path, e);
            return Optional.empty();
        }
    }
    
    private void validateFolderPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path cannot be blank");
        }
        if (path.contains("..")) {
            throw new IllegalArgumentException("Path traversal not allowed");
        }
    }
}
```

## Service Tags

### Lecture tags
```java
@Service
@Slf4j
public class AudioTagService {
    
    public MusicFileInfo readTags(String filepath) {
        validateFilePath(filepath);
        
        try {
            AudioFile audioFile = AudioFileIO.read(new File(filepath));
            Tag tag = audioFile.getTag();
            AudioHeader header = audioFile.getAudioHeader();
            
            return new MusicFileInfo(
                filepath,
                Paths.get(filepath).getFileName().toString(),
                getField(tag, FieldKey.ARTIST),
                getField(tag, FieldKey.TITLE),
                getField(tag, FieldKey.ALBUM),
                getField(tag, FieldKey.GENRE),
                getField(tag, FieldKey.BPM),
                getField(tag, FieldKey.KEY),
                new File(filepath).length(),
                Files.getLastModifiedTime(Paths.get(filepath))
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
            );
            
        } catch (Exception e) {
            log.error("Failed read tags: {}", filepath, e);
            throw new AudioProcessingException("Read tags failed", e);
        }
    }
    
    private String getField(Tag tag, FieldKey key) {
        try {
            String value = tag.getFirst(key);
            return (value == null || value.isBlank()) ? null : value.trim();
        } catch (Exception e) {
            return null;
        }
    }
    
    private void validateFilePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path blank");
        }
        if (path.contains("..")) {
            throw new IllegalArgumentException("Path traversal not allowed");
        }
        if (!Files.exists(Paths.get(path))) {
            throw new IllegalArgumentException("File not found: " + path);
        }
    }
}
```

### Écriture tags
```java
public void writeTags(String filepath, Map<String, String> tags) {
    validateFilePath(filepath);
    validateTags(tags);
    
    // Backup file avant modification
    Path backupPath = createBackup(filepath);
    
    try {
        AudioFile audioFile = AudioFileIO.read(new File(filepath));
        Tag tag = audioFile.getTagOrCreateAndSetDefault();
        
        // Appliquer modifications
        tags.forEach((key, value) -> {
            try {
                FieldKey fieldKey = FieldKey.valueOf(key.toUpperCase());
                tag.setField(fieldKey, value);
            } catch (Exception e) {
                log.warn("Failed set field {}: {}", key, e.getMessage());
            }
        });
        
        // Sauvegarder
        audioFile.commit();
        
        log.info("Tags written: {}", filepath);
        
        // Supprimer backup si succès
        Files.deleteIfExists(backupPath);
        
    } catch (Exception e) {
        log.error("Write tags failed: {}", filepath, e);
        
        // Restaurer backup
        restoreBackup(backupPath, filepath);
        
        throw new AudioProcessingException("Write tags failed", e);
    }
}

private Path createBackup(String filepath) {
    try {
        Path source = Paths.get(filepath);
        Path backup = Paths.get(filepath + ".backup");
        
        Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
        
        log.debug("Backup created: {}", backup);
        return backup;
        
    } catch (IOException e) {
        throw new AudioProcessingException("Backup creation failed", e);
    }
}

private void restoreBackup(Path backupPath, String originalPath) {
    try {
        if (Files.exists(backupPath)) {
            Files.copy(
                backupPath, 
                Paths.get(originalPath),
                StandardCopyOption.REPLACE_EXISTING
            );
            log.info("Backup restored: {}", originalPath);
        }
    } catch (IOException e) {
        log.error("Backup restore failed: {}", originalPath, e);
    }
}

private void validateTags(Map<String, String> tags) {
    if (tags == null || tags.isEmpty()) {
        throw new IllegalArgumentException("Tags empty");
    }
    
    // Valider clés
    tags.keySet().forEach(key -> {
        try {
            FieldKey.valueOf(key.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid tag key: " + key);
        }
    });
}
```

### Batch write
```java
public BatchApplyResult batchWriteTags(List<TagOperation> operations) {
    List<TagOperation> failed = new ArrayList<>();
    int successCount = 0;
    
    Instant start = Instant.now();
    
    for (TagOperation operation : operations) {
        try {
            writeTags(
                operation.filepath(),
                operation.suggestedTags()
            );
            successCount++;
            
        } catch (Exception e) {
            log.error("Batch operation failed: {}", operation.filepath(), e);
            failed.add(operation.withStatus(
                TagOperation.OperationStatus.ERROR
            ));
        }
    }
    
    Duration duration = Duration.between(start, Instant.now());
    
    return new BatchApplyResult(
        operations.size(),
        successCount,
        failed.size(),
        failed,
        duration
    );
}
```

## Détection Tags Manquants

```java
public MissingTagsReport detectMissingTags(String filepath) {
    MusicFileInfo info = readTags(filepath);
    List<String> missing = new ArrayList<>();
    
    if (isBlank(info.artist())) missing.add("ARTIST");
    if (isBlank(info.title())) missing.add("TITLE");
    if (isBlank(info.album())) missing.add("ALBUM");
    if (isBlank(info.genre())) missing.add("GENRE");
    if (isBlank(info.bpm())) missing.add("BPM");
    if (isBlank(info.key())) missing.add("KEY");
    
    return new MissingTagsReport(filepath, List.copyOf(missing));
}

private boolean isBlank(String str) {
    return str == null || str.isBlank();
}
```

## Extraction depuis Filename

```java
public TagSuggestions extractFromFilename(String filename) {
    // Patterns communs DJ:
    // "Artist - Title.mp3"
    // "Artist - Title (Original Mix).mp3"
    // "BPM Artist - Title.mp3"
    // "[Genre] Artist - Title.mp3"
    
    String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
    
    // Pattern: "Artist - Title"
    if (nameWithoutExt.contains(" - ")) {
        String[] parts = nameWithoutExt.split(" - ", 2);
        
        String artist = cleanArtistName(parts[0]);
        String title = cleanTrackTitle(parts[1]);
        
        return TagSuggestions.builder()
            .filepath(filename)
            .suggestedArtist(artist)
            .suggestedTitle(title)
            .confidence(0.7)
            .build();
    }
    
    return TagSuggestions.builder()
        .filepath(filename)
        .confidence(0.0)
        .build();
}

private String cleanArtistName(String raw) {
    // Enlever BPM: "128 Artist" -> "Artist"
    raw = raw.replaceFirst("^\\d{2,3}\\s+", "");
    
    // Enlever genre tags: "[Techno] Artist" -> "Artist"
    raw = raw.replaceAll("^\\[.*?\\]\\s*", "");
    
    return raw.trim();
}

private String cleanTrackTitle(String raw) {
    // Enlever remix info: "Title (Original Mix)" -> "Title"
    // Garder si c'est un remix important
    if (raw.contains("(") && !raw.contains("Remix")) {
        int idx = raw.indexOf('(');
        return raw.substring(0, idx).trim();
    }
    
    return raw.trim();
}
```

## Preview Modifications

```java
public record TagPreview(
    String filepath,
    Map<String, TagChange> changes
) {}

public record TagChange(
    String field,
    String oldValue,
    String newValue
) {}

public TagPreview previewChanges(
    String filepath,
    Map<String, String> newTags
) {
    MusicFileInfo current = readTags(filepath);
    Map<String, TagChange> changes = new HashMap<>();
    
    newTags.forEach((key, newValue) -> {
        String oldValue = switch(key.toUpperCase()) {
            case "ARTIST" -> current.artist();
            case "TITLE" -> current.title();
            case "ALBUM" -> current.album();
            case "GENRE" -> current.genre();
            case "BPM" -> current.bpm();
            case "KEY" -> current.key();
            default -> null;
        };
        
        if (!Objects.equals(oldValue, newValue)) {
            changes.put(key, new TagChange(key, oldValue, newValue));
        }
    });
    
    return new TagPreview(filepath, changes);
}
```

## Validation Fichier

```java
public record FileValidation(
    boolean exists,
    boolean readable,
    boolean writable,
    boolean supported,
    boolean notCorrupted,
    List<String> errors
) {
    public boolean isValid() {
        return exists && readable && writable 
            && supported && notCorrupted 
            && errors.isEmpty();
    }
}

public FileValidation validateFile(String filepath) {
    List<String> errors = new ArrayList<>();
    
    Path path = Paths.get(filepath);
    
    boolean exists = Files.exists(path);
    if (!exists) errors.add("File not found");
    
    boolean readable = exists && Files.isReadable(path);
    if (!readable) errors.add("Not readable");
    
    boolean writable = exists && Files.isWritable(path);
    if (!writable) errors.add("Not writable");
    
    boolean supported = isSupportedFormat(filepath);
    if (!supported) errors.add("Format not supported");
    
    boolean notCorrupted = true;
    if (exists && readable) {
        try {
            AudioFileIO.read(new File(filepath));
        } catch (Exception e) {
            notCorrupted = false;
            errors.add("File corrupted: " + e.getMessage());
        }
    }
    
    return new FileValidation(
        exists,
        readable,
        writable,
        supported,
        notCorrupted,
        List.copyOf(errors)
    );
}

private boolean isSupportedFormat(String filepath) {
    String lower = filepath.toLowerCase();
    return SUPPORTED_EXTENSIONS.stream()
        .anyMatch(ext -> lower.endsWith("." + ext));
}
```

## Error Handling

```java
public class AudioProcessingException extends RuntimeException {
    public AudioProcessingException(String message) {
        super(message);
    }
    
    public AudioProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Usage
try {
    writeTags(filepath, tags);
} catch (CannotWriteException e) {
    throw new AudioProcessingException("Write permission denied", e);
} catch (CannotReadException e) {
    throw new AudioProcessingException("File corrupted or unsupported", e);
} catch (TagException e) {
    throw new AudioProcessingException("Invalid tag data", e);
}
```

## Historique Modifications

```java
@Service
public class TaggingHistoryService {
    
    private final TaggingHistoryRepository repository;
    
    public void recordChange(
        String filepath,
        Map<String, String> oldTags,
        Map<String, String> newTags,
        String appliedBy
    ) {
        TaggingHistoryEntity history = new TaggingHistoryEntity();
        history.setFilePath(filepath);
        history.setOperationType("APPLY");
        history.setOldTags(convertToJson(oldTags));
        history.setNewTags(convertToJson(newTags));
        history.setAppliedBy(appliedBy);
        history.setAppliedAt(LocalDateTime.now());
        history.setStatus("SUCCESS");
        
        repository.save(history);
    }
    
    public void recordError(
        String filepath,
        String error
    ) {
        TaggingHistoryEntity history = new TaggingHistoryEntity();
        history.setFilePath(filepath);
        history.setOperationType("APPLY");
        history.setStatus("ERROR");
        history.setErrorMessage(error);
        history.setAppliedAt(LocalDateTime.now());
        
        repository.save(history);
    }
}
```

## Testing

```java
@Test
void readTags_shouldExtractAllFields() {
    // Given
    String filepath = "src/test/resources/test-with-tags.mp3";
    
    // When
    MusicFileInfo info = service.readTags(filepath);
    
    // Then
    assertNotNull(info);
    assertEquals("Artist Name", info.artist());
    assertEquals("Track Title", info.title());
}

@Test
void writeTags_shouldModifyFile() throws Exception {
    // Given
    String filepath = createTempAudioFile();
    Map<String, String> tags = Map.of(
        "ARTIST", "New Artist",
        "TITLE", "New Title"
    );
    
    // When
    service.writeTags(filepath, tags);
    
    // Then
    MusicFileInfo updated = service.readTags(filepath);
    assertEquals("New Artist", updated.artist());
    assertEquals("New Title", updated.title());
}

@Test
void writeTags_shouldRestoreBackup_whenFails() {
    // Given
    String filepath = "test.mp3";
    Map<String, String> invalidTags = Map.of("INVALID", "value");
    
    // When & Then
    assertThrows(
        AudioProcessingException.class,
        () -> service.writeTags(filepath, invalidTags)
    );
    
    // Vérifier fichier original intact
}
```

## Checklist

- [ ] Scan récursif fonctionne
- [ ] Tous formats supportés
- [ ] Validation stricte chemins
- [ ] Backup avant écriture
- [ ] Restauration si erreur
- [ ] Batch operations
- [ ] Historique changements
- [ ] Error handling robuste
- [ ] Tests avec vrais fichiers audio
