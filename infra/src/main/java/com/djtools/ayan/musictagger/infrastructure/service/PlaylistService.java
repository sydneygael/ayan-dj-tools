package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class PlaylistService {

    private final TrackVectorizationService trackVectorizationService;

    public PlaylistService(TrackVectorizationService trackVectorizationService) {
        this.trackVectorizationService = trackVectorizationService;
    }

    public Playlist generateLoopMixingPlaylist(int bpmMin, int bpmMax, String genre) {
        final var bpmMid = (bpmMin + bpmMax) / 2;
        final var genrePart = (genre != null && !genre.isBlank()) ? genre + " " : "";
        final var query = "high danceability energetic groove " + genrePart + bpmMid + " BPM loop mixing rhythmic";

        final var candidates = trackVectorizationService.findSimilarTracks(query, 20);

        final var tracks = candidates.stream()
                .filter(r -> matchesCriteria(r.track(), bpmMin, bpmMax))
                .sorted(Comparator.comparingDouble(SimilarTrackResult::similarityScore).reversed())
                .limit(10)
                .map(SimilarTrackResult::track)
                .toList();

        return new Playlist(
                UUID.randomUUID().toString(),
                "3/4 Loop Mixing",
                "THREE_QUARTER_LOOP",
                tracks,
                Instant.now()
        );
    }

    private boolean matchesCriteria(EnrichedTrackMetadata track, int bpmMin, int bpmMax) {
        final var af = track.audioFeatures();
        if (af == null) return true;

        boolean bpmOk = af.bpm() == null || (af.bpm() >= bpmMin && af.bpm() <= bpmMax);
        boolean danceabilityOk = af.danceability() == null || af.danceability() >= 0.5;

        return bpmOk && danceabilityOk;
    }
}
