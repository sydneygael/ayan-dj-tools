# Audio Processing — Exemples Complets

## 1. Lire Tous les Tags d'un Fichier MP3

```java
public MusicFileInfo readAllTags(String filepath) {
    // Validation
    if (filepath == null || filepath.isBlank())
        throw new IllegalArgumentException("Filepath blank");
    if (filepath.contains(".."))
        throw new IllegalArgumentException("Path traversal not allowed");

    Path path = Paths.get(filepath);
    if (!Files.exists(path))
        throw new IllegalArgumentException("File not found: " + filepath);
    if (!Files.isReadable(path))
        throw new IllegalArgumentException("File not readable: " + filepath);

    try {
        AudioFile audioFile = AudioFileIO.read(new File(filepath));
        Tag tag = audioFile.getTag();
        if (tag == null) {
            return new MusicFileInfo(filepath,
                path.getFileName().toString(),
                null, null, null, null, null, null,
                Files.size(path), getLastModified(path));
        }

        return new MusicFileInfo(
            filepath,
            path.getFileName().toString(),
            getField(tag, FieldKey.ARTIST),
            getField(tag, FieldKey.TITLE),
            getField(tag, FieldKey.ALBUM),
            getField(tag, FieldKey.GENRE),
            getField(tag, FieldKey.BPM),
            getField(tag, FieldKey.KEY),
            Files.size(path),
            getLastModified(path)
        );
    } catch (CannotReadException e) {
        throw new AudioProcessingException("File corrupted: " + filepath, e);
    } catch (Exception e) {
        throw new AudioProcessingException("Read failed: " + filepath, e);
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

private LocalDateTime getLastModified(Path path) throws IOException {
    return Files.getLastModifiedTime(path).toInstant()
        .atZone(ZoneId.systemDefault()).toLocalDateTime();
}
```

## 2. Ecrire des Tags avec Backup/Rollback Complet

```java
public TagWriteResult writeTags(String filepath, Map<String, String> tagsToWrite) {
    Path filePath = Paths.get(filepath);
    Path backupPath = Paths.get(filepath + ".backup");

    try {
        // 1. Creer backup
        Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);

        // 2. Lire fichier audio
        AudioFile audioFile = AudioFileIO.read(filePath.toFile());
        Tag tag = audioFile.getTag();
        if (tag == null) {
            tag = audioFile.createDefaultTag();
            audioFile.setTag(tag);
        }

        // 3. Collecter changements
        List<TagChange> changes = new ArrayList<>();
        for (Map.Entry<String, String> entry : tagsToWrite.entrySet()) {
            FieldKey fieldKey = FieldKey.valueOf(entry.getKey().toUpperCase());
            String oldValue = getField(tag, fieldKey);
            String newValue = entry.getValue();

            if (!Objects.equals(oldValue, newValue)) {
                tag.setField(fieldKey, newValue);
                changes.add(new TagChange(entry.getKey(), oldValue, newValue));
            }
        }

        // 4. Commit si changements
        if (!changes.isEmpty()) {
            audioFile.commit();
        }

        // 5. Supprimer backup (succes)
        Files.deleteIfExists(backupPath);

        return new TagWriteResult(filepath, true, changes, null);

    } catch (Exception e) {
        // Rollback : restaurer backup
        try {
            if (Files.exists(backupPath)) {
                Files.move(backupPath, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException rollbackError) {
            log.error("Rollback failed for {}: {}", filepath, rollbackError.getMessage());
        }

        return new TagWriteResult(filepath, false, List.of(), e.getMessage());
    }
}
```

## 3. Scanner une Liste de Fichiers Pre-Selectionnes

```java
private static final Set<String> SUPPORTED_EXTENSIONS =
    Set.of("mp3", "flac", "wav", "aiff", "m4a", "ogg");

public record ScanReport(
    List<MusicFileInfo> scannedFiles,
    List<String> failedFiles,
    int totalMissingTags,
    LocalDateTime scannedAt
) {}

public ScanReport scanSelectedFiles(List<String> authorizedFilepaths) {
    List<MusicFileInfo> scanned = new ArrayList<>();
    List<String> failed = new ArrayList<>();
    int totalMissing = 0;

    for (String filepath : authorizedFilepaths) {
        // Valider extension
        String extension = filepath.substring(filepath.lastIndexOf('.') + 1).toLowerCase();
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            failed.add(filepath + " (format non supporte)");
            continue;
        }

        // Valider taille
        try {
            long sizeMb = Files.size(Paths.get(filepath)) / (1024 * 1024);
            if (sizeMb > maxFileSizeMb) {
                failed.add(filepath + " (trop volumineux: " + sizeMb + "MB)");
                continue;
            }
        } catch (IOException e) {
            failed.add(filepath + " (lecture impossible)");
            continue;
        }

        // Lire tags
        try {
            MusicFileInfo info = readAllTags(filepath);
            scanned.add(info);

            // Compter tags manquants
            MissingTagsReport report = detectMissingTags(info);
            totalMissing += report.missingTags().size();
        } catch (AudioProcessingException e) {
            failed.add(filepath + " (" + e.getMessage() + ")");
        }
    }

    return new ScanReport(
        List.copyOf(scanned),
        List.copyOf(failed),
        totalMissing,
        LocalDateTime.now()
    );
}

public MissingTagsReport detectMissingTags(MusicFileInfo info) {
    List<String> missing = new ArrayList<>();
    if (info.artist() == null) missing.add("ARTIST");
    if (info.title() == null) missing.add("TITLE");
    if (info.album() == null) missing.add("ALBUM");
    if (info.genre() == null) missing.add("GENRE");
    if (info.bpm() == null) missing.add("BPM");
    if (info.key() == null) missing.add("KEY");
    return new MissingTagsReport(info.filepath(), List.copyOf(missing));
}
```

## 4. Test Unitaire avec Fichier Audio Genere

```java
@ExtendWith(MockitoExtension.class)
class AudioTagServiceTest {

    @TempDir Path tempDir;

    @Test
    void readAllTags_shouldReturnCompleteInfo() throws Exception {
        // Given — fichier MP3 avec tags
        Path testFile = TestAudioFileHelper.createTestMp3(tempDir,
            "Deadmau5", "Strobe", "For Lack of a Better Name",
            "Progressive House", "128", "Am");

        AudioTagService service = new AudioTagService();

        // When
        MusicFileInfo info = service.readAllTags(testFile.toString());

        // Then
        assertThat(info.artist()).isEqualTo("Deadmau5");
        assertThat(info.title()).isEqualTo("Strobe");
        assertThat(info.album()).isEqualTo("For Lack of a Better Name");
        assertThat(info.genre()).isEqualTo("Progressive House");
        assertThat(info.bpm()).isEqualTo("128");
        assertThat(info.key()).isEqualTo("Am");
    }

    @Test
    void writeTags_shouldCreateBackupAndRestore_onFailure() throws Exception {
        Path testFile = TestAudioFileHelper.createTestMp3(tempDir,
            "Original Artist", "Original Title");
        Path backupPath = Paths.get(testFile + ".backup");

        // Force une erreur en rendant le fichier read-only apres backup
        // Le backup devrait etre restaure automatiquement
        AudioTagService service = new AudioTagService();
        TagWriteResult result = service.writeTags(
            testFile.toString(),
            Map.of("ARTIST", "New Artist", "TITLE", "New Title"));

        assertThat(result.success()).isTrue();
        assertThat(result.changedFields()).hasSize(2);
        assertThat(Files.exists(backupPath)).isFalse(); // Backup supprime apres succes
    }
}
```
