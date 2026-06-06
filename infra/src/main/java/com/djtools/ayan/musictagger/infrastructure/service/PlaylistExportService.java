package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.port.out.ScannedTrackRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlaylistExportService {

    private final ScannedTrackRepository scannedTrackRepository;

    public PlaylistExportService(ScannedTrackRepository scannedTrackRepository) {
        this.scannedTrackRepository = scannedTrackRepository;
    }

    public record TrackExportEntry(String artist, String title, Long durationMs) {}

    /**
     * Génère le contenu M3U étendu pour une liste de morceaux.
     * Chaque morceau dont le chemin est trouvé dans la bibliothèque locale produit une entrée
     * #EXTINF + filepath. Les morceaux introuvables sont inclus comme commentaires.
     */
    public String buildM3uContent(List<TrackExportEntry> tracks) {
        final var sb = new StringBuilder("#EXTM3U\n");
        for (final var entry : tracks) {
            final var artist = entry.artist() != null ? entry.artist() : "";
            final var title  = entry.title()  != null ? entry.title()  : "";
            scannedTrackRepository.findByArtistAndTitle(artist, title)
                    .ifPresentOrElse(
                            found -> appendExtInf(sb, artist, title, entry.durationMs(), found.filepath().value()),
                            () -> appendNotFound(sb, artist, title));
        }
        return sb.toString();
    }

    public static String safeFilename(String name) {
        final var sanitized = name != null ? name.replaceAll("[\\\\/:*?\"<>|]", "_").trim() : "";
        return sanitized.isBlank() ? "playlist" : sanitized;
    }

    private static void appendExtInf(StringBuilder sb, String artist, String title,
                                     Long durationMs, String filepath) {
        final long durationSec = durationMs != null ? durationMs / 1000 : -1;
        sb.append("#EXTINF:").append(durationSec).append(",")
          .append(artist).append(" - ").append(title).append("\n")
          .append(filepath).append("\n");
    }

    private static void appendNotFound(StringBuilder sb, String artist, String title) {
        sb.append("# NOT FOUND: ").append(artist).append(" - ").append(title).append("\n");
    }
}
