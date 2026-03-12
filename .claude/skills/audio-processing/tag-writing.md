# Audio Processing — Tag Writing, Backup & History

## Ecriture tags

```java
public void writeTags(String filepath, Map<String, String> tags) {
    validateFilePath(filepath);
    validateTags(tags);
    Path backupPath = createBackup(filepath);

    try {
        AudioFile audioFile = AudioFileIO.read(new File(filepath));
        Tag tag = audioFile.getTagOrCreateAndSetDefault();
        tags.forEach((key, value) -> {
            try {
                tag.setField(FieldKey.valueOf(key.toUpperCase()), value);
            } catch (Exception e) {
                log.warn("Failed set field {}: {}", key, e.getMessage());
            }
        });
        audioFile.commit();
        Files.deleteIfExists(backupPath);
    } catch (Exception e) {
        restoreBackup(backupPath, filepath);
        throw new AudioProcessingException("Write tags failed", e);
    }
}
```

## Backup / Restore

```java
private Path createBackup(String filepath) {
    try {
        Path source = Paths.get(filepath);
        Path backup = Paths.get(filepath + ".backup");
        Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
        return backup;
    } catch (IOException e) {
        throw new AudioProcessingException("Backup creation failed", e);
    }
}

private void restoreBackup(Path backupPath, String originalPath) {
    try {
        if (Files.exists(backupPath)) {
            Files.copy(backupPath, Paths.get(originalPath), StandardCopyOption.REPLACE_EXISTING);
            log.info("Backup restored: {}", originalPath);
        }
    } catch (IOException e) {
        log.error("Backup restore failed: {}", originalPath, e);
    }
}

private void validateTags(Map<String, String> tags) {
    if (tags == null || tags.isEmpty()) throw new IllegalArgumentException("Tags empty");
    tags.keySet().forEach(key -> {
        try { FieldKey.valueOf(key.toUpperCase()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid tag key: " + key); }
    });
}
```

## Batch write

```java
public BatchApplyResult batchWriteTags(List<TagOperation> operations) {
    List<TagOperation> failed = new ArrayList<>();
    int successCount = 0;
    Instant start = Instant.now();

    for (TagOperation operation : operations) {
        try {
            writeTags(operation.filepath(), operation.suggestedTags());
            successCount++;
        } catch (Exception e) {
            failed.add(operation.withStatus(TagOperation.OperationStatus.ERROR));
        }
    }

    return new BatchApplyResult(operations.size(), successCount, failed.size(), failed,
        Duration.between(start, Instant.now()));
}
```

## Preview Modifications

```java
public record TagPreview(String filepath, Map<String, TagChange> changes) {}
public record TagChange(String field, String oldValue, String newValue) {}

public TagPreview previewChanges(String filepath, Map<String, String> newTags) {
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
        if (!Objects.equals(oldValue, newValue))
            changes.put(key, new TagChange(key, oldValue, newValue));
    });
    return new TagPreview(filepath, changes);
}
```

## Historique Modifications

```java
@Service
public class TaggingHistoryService {
    private final TaggingHistoryRepository repository;

    public void recordChange(String filepath, Map<String, String> oldTags,
            Map<String, String> newTags, String appliedBy) {
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
}
```

## Testing

```java
@Test
void writeTags_shouldModifyFile() throws Exception {
    String filepath = createTempAudioFile();
    service.writeTags(filepath, Map.of("ARTIST", "New Artist", "TITLE", "New Title"));
    MusicFileInfo updated = service.readTags(filepath);
    assertEquals("New Artist", updated.artist());
    assertEquals("New Title", updated.title());
}

@Test
void writeTags_shouldRestoreBackup_whenFails() {
    assertThrows(AudioProcessingException.class,
        () -> service.writeTags("test.mp3", Map.of("INVALID", "value")));
}
```
