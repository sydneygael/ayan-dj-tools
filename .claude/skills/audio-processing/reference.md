# Audio Processing — Reference Rapide

## JAudiotagger 3.0.1 — FieldKey Enum (Principaux)

| FieldKey | Description | Obligatoire DJ |
|----------|-------------|----------------|
| `ARTIST` | Artiste principal | Oui |
| `TITLE` | Titre du morceau | Oui |
| `ALBUM` | Nom de l'album | Oui |
| `GENRE` | Genre musical | Oui |
| `BPM` | Battements par minute | Oui |
| `KEY` | Tonalite musicale | Oui |
| `YEAR` | Annee de sortie | Non |
| `TRACK` | Numero de piste | Non |
| `ALBUM_ARTIST` | Artiste album | Non |
| `COMMENT` | Commentaire libre | Non |
| `COMPOSER` | Compositeur | Non |
| `DISC_NO` | Numero de disque | Non |
| `ISRC` | Code ISRC | Non |
| `LABEL` | Label musical (custom) | Non |
| `REMIXER` | Remixeur | Non |

## Formats Supportes et Specificites

| Format | Extension | Tag System | Particularites |
|--------|-----------|------------|----------------|
| MP3 | `.mp3` | ID3v2.4 (prefere), ID3v1 | Plus repandu, BPM/KEY bien supportes |
| FLAC | `.flac` | Vorbis Comments | Lossless, tags illimites |
| WAV | `.wav` | INFO chunks / ID3 | Support tags limite |
| AIFF | `.aiff` | ID3v2 | Apple, bon support tags |
| M4A | `.m4a` | iTunes MP4 atoms | AAC, tags Apple-style |
| OGG | `.ogg` | Vorbis Comments | Comme FLAC pour les tags |

## JAudiotagger API Principales

```java
// Lecture
AudioFile audioFile = AudioFileIO.read(new File(filepath));
Tag tag = audioFile.getTag();
String value = tag.getFirst(FieldKey.ARTIST);       // "" si absent
List<TagField> fields = tag.getFields(FieldKey.GENRE);

// Ecriture
tag.setField(FieldKey.BPM, "128");                   // Ecraser valeur
tag.addField(FieldKey.GENRE, "House");                // Ajouter (multi-valeur)
tag.deleteField(FieldKey.COMMENT);                    // Supprimer
audioFile.commit();                                    // Sauvegarder sur disque

// Creation tag si absent
if (audioFile.getTag() == null) {
    audioFile.setTag(audioFile.createDefaultTag());
}
```

## Extensions Supportees (Config)

```yaml
dj-tagger:
  audio:
    supported-extensions: mp3,flac,wav,aiff,m4a,ogg
    max-file-size-mb: 100
```

## Validation Fichier — Checklist

| Check | Methode | Exception si echec |
|-------|---------|-------------------|
| Existe | `Files.exists(path)` | `IllegalArgumentException` |
| Lisible | `Files.isReadable(path)` | `IllegalArgumentException` |
| Ecrivable | `Files.isWritable(path)` | `IllegalArgumentException` |
| Format supporte | Extension dans `SUPPORTED_EXTENSIONS` | `IllegalArgumentException` |
| Pas corrompu | `AudioFileIO.read()` sans exception | `AudioProcessingException` |
| Pas path traversal | `!path.contains("..")` | `IllegalArgumentException` |
| Taille max | `Files.size(path) < maxFileSizeMb * 1024 * 1024` | `IllegalArgumentException` |

## Backup/Rollback Pattern

```
1. Copier fichier → fichier.backup
2. Ecrire tags → fichier
3. commit() → disque
4. Supprimer fichier.backup
   └─ Si exception → restaurer fichier.backup → fichier
```

## Records Phase 5

| Record | Champs |
|--------|--------|
| `TagWriteResult` | `filepath`, `success`, `changedFields`, `error` |
| `BatchApplyResult` | `totalFiles`, `successCount`, `failureCount`, `results` |
| `TagChange` | `field`, `oldValue`, `newValue` |
| `TagPreview` | `filepath`, `changes` (List<TagChange>) |
| `TaggingHistoryEntry` | `filepath`, `planId`, `changes`, `timestamp`, `status` |
