package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;

import java.util.List;

public record MusicLookupResult(
        boolean found,
        String source,
        String query,
        List<EnrichedTrackMetadata> tracks,
        String webSummary,
        List<MusicFileInfo> localTracks
) {
    public MusicLookupResult {
        tracks = tracks != null ? List.copyOf(tracks) : List.of();
        localTracks = localTracks != null ? List.copyOf(localTracks) : List.of();
    }

    public String toSummary() {
        if (!found) {
            return "Aucun résultat trouvé pour \"" + query + "\".";
        }
        final var sb = new StringBuilder();
        if (webSummary != null && !webSummary.isBlank()) {
            sb.append(webSummary);
        }
        if (!tracks.isEmpty()) {
            if (!sb.isEmpty()) sb.append("\n\n");
            sb.append("Morceaux trouvés (").append(source).append(") :\n");
            tracks.forEach(t -> {
                sb.append("  • ").append(t.artist()).append(" — ").append(t.title());
                if (t.releaseYear() != null) sb.append(" (").append(t.releaseYear()).append(')');
                if (t.audioFeatures() != null && t.audioFeatures().bpm() != null && t.audioFeatures().bpm() > 0)
                    sb.append(" | BPM ").append(t.audioFeatures().bpm().intValue());
                sb.append('\n');
            });
        }
        if (!localTracks.isEmpty()) {
            sb.append("\nDans ta bibliothèque locale : ")
              .append(localTracks.size()).append(" morceau(x) de cet artiste.\n");
            localTracks.stream().limit(5).forEach(t ->
                sb.append("  • ").append(t.title() != null ? t.title() : t.filepath().value()).append('\n'));
        }
        return sb.toString().trim();
    }
}
