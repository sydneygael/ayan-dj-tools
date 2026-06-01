package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.usecase.BuildHarmonicPlaylistUseCase;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class PlaylistService {

    private static final int HARMONIC_CANDIDATE_POOL = 200;

    private final TrackVectorizationService trackVectorizationService;
    private final BuildHarmonicPlaylistUseCase buildHarmonicPlaylistUseCase;

    public PlaylistService(TrackVectorizationService trackVectorizationService,
                           BuildHarmonicPlaylistUseCase buildHarmonicPlaylistUseCase) {
        this.trackVectorizationService = trackVectorizationService;
        this.buildHarmonicPlaylistUseCase = buildHarmonicPlaylistUseCase;
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

    /**
     * Génère une playlist mixée harmoniquement via la roue de Camelot (« Mixed In Key »).
     * Récupère un large pool de candidats via RAG, le borne par BPM, puis délègue le séquençage
     * harmonique (transitions ±6 BPM, tonalités compatibles) au use case domaine.
     */
    public HarmonicPlaylist generateHarmonicPlaylist(int bpmMin, int bpmMax, String genre,
                                                     Double targetEnergy, int count) {
        final var bpmMid = (bpmMin + bpmMax) / 2;
        final var genrePart = (genre != null && !genre.isBlank()) ? genre + " " : "";
        final var energyPart = (targetEnergy != null && targetEnergy >= 0.6) ? "energetic " : "";
        final var query = "harmonic mixing " + energyPart + genrePart + bpmMid + " BPM key compatible";

        final var candidates = trackVectorizationService.findSimilarTracks(query, HARMONIC_CANDIDATE_POOL).stream()
                .map(SimilarTrackResult::track)
                .filter(t -> withinBpmRange(t, bpmMin, bpmMax))
                .toList();

        final var name = "Harmonic Mix" + (genrePart.isBlank() ? "" : " - " + genre.trim());
        return buildHarmonicPlaylistUseCase.build(
                UUID.randomUUID().toString(), name, candidates, count, targetEnergy);
    }

    private boolean withinBpmRange(EnrichedTrackMetadata track, int bpmMin, int bpmMax) {
        final var af = track.audioFeatures();
        if (af == null || af.bpm() == null) {
            return true;
        }
        return af.bpm() >= bpmMin && af.bpm() <= bpmMax;
    }

    private boolean matchesCriteria(EnrichedTrackMetadata track, int bpmMin, int bpmMax) {
        final var af = track.audioFeatures();
        if (af == null) return true;

        boolean bpmOk = af.bpm() == null || (af.bpm() >= bpmMin && af.bpm() <= bpmMax);
        boolean danceabilityOk = af.danceability() == null || af.danceability() >= 0.5;

        return bpmOk && danceabilityOk;
    }
}
