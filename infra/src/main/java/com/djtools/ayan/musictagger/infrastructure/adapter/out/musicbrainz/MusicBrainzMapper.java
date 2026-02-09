package com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz.dto.MBRecording;

import java.util.Comparator;
import java.util.List;

public class MusicBrainzMapper {

    public EnrichedTrackMetadata toEnrichedMetadata(MBRecording recording) {
        String artist = extractArtist(recording);
        String isrc = recording.isrcs() != null && !recording.isrcs().isEmpty()
                ? recording.isrcs().getFirst()
                : null;
        List<String> tags = recording.tags() != null
                ? recording.tags().stream()
                        .sorted(Comparator.comparingInt(t -> -t.count()))
                        .map(t -> t.name())
                        .toList()
                : List.of();
        String album = extractAlbum(recording);
        int year = extractYear(recording);

        return new EnrichedTrackMetadata(
                "musicbrainz:" + recording.id(),
                artist,
                recording.title(),
                album,
                List.of(),
                List.of(),
                null,
                null,
                isrc,
                tags,
                year,
                0,
                recording.length() != null ? recording.length() : 0,
                null
        );
    }

    private String extractArtist(MBRecording recording) {
        if (recording.artistCredit() == null || recording.artistCredit().isEmpty()) {
            return null;
        }
        return recording.artistCredit().stream()
                .map(ac -> ac.name() != null ? ac.name() : (ac.artist() != null ? ac.artist().name() : ""))
                .reduce((a, b) -> a + b)
                .orElse(null);
    }

    private String extractAlbum(MBRecording recording) {
        if (recording.releases() == null || recording.releases().isEmpty()) {
            return null;
        }
        return recording.releases().getFirst().title();
    }

    private int extractYear(MBRecording recording) {
        if (recording.releases() == null || recording.releases().isEmpty()) {
            return 0;
        }
        String date = recording.releases().getFirst().date();
        if (date == null || date.length() < 4) {
            return 0;
        }
        try {
            return Integer.parseInt(date.substring(0, 4));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
