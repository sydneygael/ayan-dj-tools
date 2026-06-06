package com.djtools.ayan.musictagger.infrastructure.service;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.domain.usecase.BuildHarmonicPlaylistUseCase;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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

    /** Génère une playlist loop-mixing via RAG sémantique, filtrée par BPM et danceability. */
    public Playlist generateLoopMixingPlaylist(int bpmMin, int bpmMax, String genre) {
        final var query = buildLoopMixingQuery(midBpm(bpmMin, bpmMax), genre);
        final var tracks = trackVectorizationService.findSimilarTracks(query, 20).stream()
                .filter(r -> matchesLoopCriteria(r.track(), bpmMin, bpmMax))
                .sorted(Comparator.comparingDouble(SimilarTrackResult::similarityScore).reversed())
                .limit(10)
                .map(SimilarTrackResult::track)
                .toList();
        return new Playlist(UUID.randomUUID().toString(), "3/4 Loop Mixing", "THREE_QUARTER_LOOP", tracks, Instant.now());
    }

    /**
     * Génère une playlist mixée harmoniquement via la roue de Camelot (« Mixed In Key »).
     * Récupère un large pool de candidats via RAG, le borne par BPM, puis délègue le séquençage
     * harmonique (transitions ±6 BPM, tonalités compatibles) au use case domaine.
     */
    public HarmonicPlaylist generateHarmonicPlaylist(int bpmMin, int bpmMax, String genre,
                                                     Double targetEnergy, int count) {
        final var query = buildHarmonicQuery(midBpm(bpmMin, bpmMax), genre, targetEnergy);
        final var candidates = trackVectorizationService.findSimilarTracks(query, HARMONIC_CANDIDATE_POOL).stream()
                .map(SimilarTrackResult::track)
                .filter(t -> withinBpmRange(t, bpmMin, bpmMax))
                .toList();
        final var name = harmonicPlaylistName(genre);
        return buildHarmonicPlaylistUseCase.build(UUID.randomUUID().toString(), name, candidates, count, targetEnergy);
    }

    /**
     * Génère une playlist structurée en arc narratif (intro → montée → peak → outro)
     * à partir d'un thème lyrical ou d'une ambiance. Exploite les embeddings lyrics (topics, mood, sentiment).
     */
    public Playlist generateThematicPlaylist(String theme, int bpmMin, int bpmMax, int count) {
        final var safeCount = Math.max(4, Math.min(count, 50));
        final var query = buildThematicQuery(theme);
        final var tracks = trackVectorizationService.findSimilarTracks(query, safeCount * 6).stream()
                .map(SimilarTrackResult::track)
                .filter(t -> withinBpmRange(t, bpmMin, bpmMax))
                .sorted(Comparator.comparingDouble(this::energyOf))
                .limit(safeCount)
                .toList();
        return new Playlist(UUID.randomUUID().toString(), "Thème : " + theme.trim(), "THEMATIC_ARC", arrangeInArc(tracks), Instant.now());
    }

    // ─── Query builders ───────────────────────────────────────────────────────

    private static String buildLoopMixingQuery(int bpmMid, String genre) {
        return "high danceability energetic groove " + genrePrefix(genre) + bpmMid + " BPM loop mixing rhythmic";
    }

    private static String buildHarmonicQuery(int bpmMid, String genre, Double targetEnergy) {
        final var energyPart = isHighEnergy(targetEnergy) ? "energetic " : "";
        return "harmonic mixing " + energyPart + genrePrefix(genre) + bpmMid + " BPM key compatible";
    }

    private static String buildThematicQuery(String theme) {
        return "lyrics themes topics " + theme.trim() + " mood sentiment";
    }

    private static String genrePrefix(String genre) {
        return (genre != null && !genre.isBlank()) ? genre.trim() + " " : "";
    }

    private static String harmonicPlaylistName(String genre) {
        return "Harmonic Mix" + (genre != null && !genre.isBlank() ? " - " + genre.trim() : "");
    }

    private static int midBpm(int bpmMin, int bpmMax) {
        return (bpmMin + bpmMax) / 2;
    }

    private static boolean isHighEnergy(Double energy) {
        return energy != null && energy >= 0.6;
    }

    // ─── Arc arrangement ──────────────────────────────────────────────────────

    /**
     * Réarrange les morceaux (triés par énergie croissante) en arc narratif :
     * intro (bas) → montée → peak (haut) → outro (retour moyen).
     * Découpe en 4 quartiles : A(low) B(mid-low) C(mid-high) D(high) → ordonne A B D C.
     */
    private List<EnrichedTrackMetadata> arrangeInArc(List<EnrichedTrackMetadata> sortedByEnergy) {
        int n = sortedByEnergy.size();
        if (n < 4) return sortedByEnergy;
        int q = n / 4;
        var intro  = sortedByEnergy.subList(0,     q);
        var build  = sortedByEnergy.subList(q,     2 * q);
        var outro  = sortedByEnergy.subList(2 * q, 3 * q);
        var peak   = sortedByEnergy.subList(3 * q, n);
        return Stream.of(intro, build, peak, outro).flatMap(List::stream).toList();
    }

    // ─── Predicates & helpers ─────────────────────────────────────────────────

    private double energyOf(EnrichedTrackMetadata track) {
        final var af = track.audioFeatures();
        return (af != null && af.energy() != null) ? af.energy() : 0.5;
    }

    /** Retourne true si le track n'a pas de BPM ou si son BPM est dans [bpmMin, bpmMax]. */
    private boolean withinBpmRange(EnrichedTrackMetadata track, int bpmMin, int bpmMax) {
        final var af = track.audioFeatures();
        if (af == null || af.bpm() == null) return true;
        return af.bpm() >= bpmMin && af.bpm() <= bpmMax;
    }

    /** Filtre loop-mixing : BPM dans la plage ET danceability ≥ 0.5 (ou champ absent). */
    private boolean matchesLoopCriteria(EnrichedTrackMetadata track, int bpmMin, int bpmMax) {
        final var af = track.audioFeatures();
        if (af == null) return true;
        boolean bpmOk = af.bpm() == null || (af.bpm() >= bpmMin && af.bpm() <= bpmMax);
        boolean danceabilityOk = af.danceability() == null || af.danceability() >= 0.5;
        return bpmOk && danceabilityOk;
    }
}
