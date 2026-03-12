---
name: audio-processing
description: Traitement audio avec JAudiotagger 3.0.1. Lecture/ecriture tags, scan fichiers, backup/rollback, validation, formats supportes (MP3, FLAC, WAV, AIFF, M4A, OGG). Utiliser pour tout code lie aux fichiers audio.
user-invocable: false
---

# Audio Processing Skill

JAudiotagger + File Scanning + Tags Manipulation

> Ecriture tags, backup/rollback, batch, historique : voir [tag-writing.md](./tag-writing.md)
> Reference rapide API : voir [reference.md](./reference.md)
> Exemples complets : voir [examples.md](./examples.md)

## Principes

- **JAudiotagger** pour lecture/ecriture tags
- **Validation stricte** chemins fichiers
- **Backup** avant modifications
- **Atomic operations** pour securite

## Dependencies

```gradle
dependencies {
    implementation 'net.jthink:jaudiotagger:3.0.1'
}
```

## Formats Supportes

MP3 (ID3v1, ID3v2), FLAC, WAV, AIFF, M4A (MP4/AAC), OGG Vorbis

## Service Scan

```java
@Service
@Slf4j
public class AudioScannerService {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        "mp3", "flac", "wav", "aiff", "m4a", "ogg");

    @Value("${dj-tagger.audio.max-file-size-mb}")
    private int maxFileSizeMb;

    public List<MusicFileInfo> scanFolder(String folderPath) {
        validateFolderPath(folderPath);
        Path path = Paths.get(folderPath);

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
            throw new AudioProcessingException("Scan failed", e);
        }
    }

    private boolean isSupportedAudioFile(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(ext -> filename.endsWith("." + ext));
    }
}
```

## Service Tags - Lecture

```java
@Service
@Slf4j
public class AudioTagService {

    public MusicFileInfo readTags(String filepath) {
        validateFilePath(filepath);
        try {
            AudioFile audioFile = AudioFileIO.read(new File(filepath));
            Tag tag = audioFile.getTag();
            return new MusicFileInfo(filepath,
                Paths.get(filepath).getFileName().toString(),
                getField(tag, FieldKey.ARTIST), getField(tag, FieldKey.TITLE),
                getField(tag, FieldKey.ALBUM), getField(tag, FieldKey.GENRE),
                getField(tag, FieldKey.BPM), getField(tag, FieldKey.KEY),
                new File(filepath).length(),
                Files.getLastModifiedTime(Paths.get(filepath)).toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
        } catch (Exception e) {
            throw new AudioProcessingException("Read tags failed", e);
        }
    }

    private String getField(Tag tag, FieldKey key) {
        try {
            String value = tag.getFirst(key);
            return (value == null || value.isBlank()) ? null : value.trim();
        } catch (Exception e) { return null; }
    }

    private void validateFilePath(String path) {
        if (path == null || path.isBlank()) throw new IllegalArgumentException("Path blank");
        if (path.contains("..")) throw new IllegalArgumentException("Path traversal not allowed");
        if (!Files.exists(Paths.get(path))) throw new IllegalArgumentException("File not found: " + path);
    }
}
```

## Detection Tags Manquants

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
```

## Extraction depuis Filename

```java
public TagSuggestions extractFromFilename(String filename) {
    // Patterns: "Artist - Title.mp3", "Artist - Title (Original Mix).mp3"
    String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
    if (nameWithoutExt.contains(" - ")) {
        String[] parts = nameWithoutExt.split(" - ", 2);
        String artist = parts[0].replaceFirst("^\\d{2,3}\\s+", "")
            .replaceAll("^\\[.*?\\]\\s*", "").trim();
        String title = parts[1].trim();
        return TagSuggestions.builder()
            .filepath(filename).suggestedArtist(artist)
            .suggestedTitle(title).confidence(0.7).build();
    }
    return TagSuggestions.builder().filepath(filename).confidence(0.0).build();
}
```

## Validation Fichier

```java
public record FileValidation(
    boolean exists, boolean readable, boolean writable,
    boolean supported, boolean notCorrupted, List<String> errors
) {
    public boolean isValid() {
        return exists && readable && writable && supported && notCorrupted && errors.isEmpty();
    }
}

public FileValidation validateFile(String filepath) {
    List<String> errors = new ArrayList<>();
    Path path = Paths.get(filepath);
    boolean exists = Files.exists(path);
    if (!exists) errors.add("File not found");
    boolean readable = exists && Files.isReadable(path);
    boolean writable = exists && Files.isWritable(path);
    boolean supported = isSupportedFormat(filepath);
    boolean notCorrupted = true;
    if (exists && readable) {
        try { AudioFileIO.read(new File(filepath)); }
        catch (Exception e) { notCorrupted = false; errors.add("File corrupted"); }
    }
    return new FileValidation(exists, readable, writable, supported, notCorrupted, List.copyOf(errors));
}
```

## Error Handling

```java
public class AudioProcessingException extends RuntimeException {
    public AudioProcessingException(String message) { super(message); }
    public AudioProcessingException(String message, Throwable cause) { super(message, cause); }
}
```

## Checklist

- [ ] Scan recursif fonctionne
- [ ] Tous formats supportes
- [ ] Validation stricte chemins
- [ ] Backup avant ecriture
- [ ] Restauration si erreur
- [ ] Batch operations
- [ ] Historique changements
- [ ] Error handling robuste
- [ ] Tests avec vrais fichiers audio
