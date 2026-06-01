package com.djtools.ayan.musictagger.domain.usecase;

import com.djtools.ayan.musictagger.domain.model.MissingTagsReport;
import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.in.AudioFileReader;
import com.djtools.ayan.musictagger.domain.port.out.ScannedTrackRepository;

import java.util.List;

/**
 * Scanne des fichiers audio pour lire leurs tags et détecter les tags manquants.
 * Cas d'usage de base, sans enrichissement externe.
 */
public class ScanMusicUseCase {

    private static final System.Logger log = System.getLogger(ScanMusicUseCase.class.getName());
    private static final List<String> ALL_TAGS = List.of("artist", "title", "album", "genre", "bpm", "key");

    private final AudioFileReader audioFileReader;
    private final ScannedTrackRepository scannedTrackRepository;

    public ScanMusicUseCase(
            AudioFileReader audioFileReader,
            ScannedTrackRepository scannedTrackRepository) {
        this.audioFileReader = audioFileReader;
        this.scannedTrackRepository = scannedTrackRepository;
    }

    /** Lit les tags de chaque fichier et persiste l'état scanné. Les fichiers illisibles sont ignorés. */
    public List<MusicFileInfo> execute(List<Filepath> paths) {
        log.log(System.Logger.Level.INFO, "Scan de {0} fichier(s)", paths.size());
        final var results = paths.stream()
                .map(audioFileReader::readTags)
                .flatMap(java.util.Optional::stream)
                .peek(scannedTrackRepository::save)
                .toList();
        log.log(System.Logger.Level.INFO, "{0}/{1} fichier(s) lus avec succès", results.size(), paths.size());
        return results;
    }

    /** Détecte les tags manquants d'un fichier. Si illisible, tous les tags sont déclarés manquants. */
    public MissingTagsReport detectMissingTags(Filepath path) {
        return audioFileReader.readTags(path)
                .map(info -> {
                    List<String> missing = ALL_TAGS.stream()
                            .filter(info::isMissingTag)
                            .toList();
                    log.log(System.Logger.Level.DEBUG, "Tags manquants pour {0} : {1}", path.value(), missing);
                    return new MissingTagsReport(path, missing);
                })
                .orElse(new MissingTagsReport(path, ALL_TAGS));
    }
}
