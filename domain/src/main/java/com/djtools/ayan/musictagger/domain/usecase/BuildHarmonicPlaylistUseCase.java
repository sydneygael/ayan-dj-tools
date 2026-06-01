package com.djtools.ayan.musictagger.domain.usecase;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.HarmonicPlaylist;
import com.djtools.ayan.musictagger.domain.model.PlaylistStats;
import com.djtools.ayan.musictagger.domain.model.PlaylistTrack;
import com.djtools.ayan.musictagger.domain.model.vo.CamelotKey;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Construit une séquence harmonique « Mixed In Key » à partir d'une liste de candidats.
 *
 * <p>Logique pure (aucune dépendance Spring/RAG) : groupe les morceaux par clé Camelot, démarre sur
 * la clé la plus fournie, puis enchaîne en restant dans des tonalités compatibles tout en
 * privilégiant un écart de ±6 BPM avec le morceau précédent. Best-effort : s'arrête dès qu'aucun
 * morceau compatible n'est disponible et ne lève jamais d'exception.
 */
public class BuildHarmonicPlaylistUseCase {

    private static final double BPM_TOLERANCE = 6.0;

    public HarmonicPlaylist build(String playlistId,
                                  String name,
                                  List<EnrichedTrackMetadata> candidates,
                                  int targetCount,
                                  Double targetEnergy) {

        final Map<CamelotKey, List<EnrichedTrackMetadata>> byKey = new LinkedHashMap<>();
        for (final var track : candidates == null ? List.<EnrichedTrackMetadata>of() : candidates) {
            CamelotKey.fromAudioFeatures(track.audioFeatures())
                    .ifPresent(key -> byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(track));
        }

        final List<PlaylistTrack> playlist = new ArrayList<>();
        final Set<String> usedIds = new HashSet<>();

        if (!byKey.isEmpty() && targetCount > 0) {
            CamelotKey currentKey = selectStartingKey(byKey);
            EnrichedTrackMetadata current = selectBestStartTrack(byKey.get(currentKey), targetEnergy);
            usedIds.add(identity(current));
            playlist.add(new PlaylistTrack(current, 1, currentKey.code(), null, 0.0));

            while (playlist.size() < targetCount) {
                final var previous = current;
                final var previousKey = currentKey;
                final Optional<EnrichedTrackMetadata> next = byKey.entrySet().stream()
                        .filter(e -> previousKey.isCompatibleWith(e.getKey()))
                        .flatMap(e -> e.getValue().stream())
                        .filter(t -> !usedIds.contains(identity(t)))
                        .max(Comparator.comparingDouble(t -> selectionScore(previous, t, targetEnergy)));

                if (next.isEmpty()) {
                    break;
                }
                current = next.get();
                currentKey = CamelotKey.fromAudioFeatures(current.audioFeatures()).orElseThrow();
                usedIds.add(identity(current));
                playlist.add(new PlaylistTrack(
                        current,
                        playlist.size() + 1,
                        currentKey.code(),
                        transitionType(previousKey, currentKey),
                        transitionQuality(previous, current)));
            }
        }

        return new HarmonicPlaylist(playlistId, name, playlist, computeStats(playlist), Instant.now());
    }

    private CamelotKey selectStartingKey(Map<CamelotKey, List<EnrichedTrackMetadata>> byKey) {
        return byKey.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().size()))
                .map(Map.Entry::getKey)
                .orElseThrow();
    }

    private EnrichedTrackMetadata selectBestStartTrack(List<EnrichedTrackMetadata> tracks, Double targetEnergy) {
        return tracks.stream()
                .max(Comparator.comparingDouble(t -> energyScoreToTarget(t, targetEnergy)))
                .orElseThrow();
    }

    /** Score de sélection du prochain morceau : proximité BPM (±6) puis proximité d'énergie cible. */
    private double selectionScore(EnrichedTrackMetadata previous, EnrichedTrackMetadata candidate, Double targetEnergy) {
        return bpmScore(previous, candidate) * 0.6 + energyScoreToTarget(candidate, targetEnergy) * 0.4;
    }

    /** Qualité de transition affichée : 40 % BPM (±6), 30 % proximité d'énergie, +0,3 de base. */
    private double transitionQuality(EnrichedTrackMetadata from, EnrichedTrackMetadata to) {
        final double energyScore = energyProximity(from, to);
        return bpmScore(from, to) * 0.4 + energyScore * 0.3 + 0.3;
    }

    private double bpmScore(EnrichedTrackMetadata from, EnrichedTrackMetadata to) {
        final Double fromBpm = bpm(from);
        final Double toBpm = bpm(to);
        if (fromBpm == null || toBpm == null) {
            return 0.5;
        }
        final double diff = Math.abs(fromBpm - toBpm);
        return diff <= BPM_TOLERANCE ? 1.0 : Math.max(0.0, 1.0 - (diff - BPM_TOLERANCE) / 10.0);
    }

    private double energyScoreToTarget(EnrichedTrackMetadata track, Double targetEnergy) {
        final Double energy = energy(track);
        if (targetEnergy == null || energy == null) {
            return 0.5;
        }
        return Math.max(0.0, 1.0 - Math.abs(energy - targetEnergy));
    }

    private double energyProximity(EnrichedTrackMetadata from, EnrichedTrackMetadata to) {
        final Double fromEnergy = energy(from);
        final Double toEnergy = energy(to);
        if (fromEnergy == null || toEnergy == null) {
            return 0.5;
        }
        return Math.max(0.0, 1.0 - Math.abs(fromEnergy - toEnergy));
    }

    private String transitionType(CamelotKey from, CamelotKey to) {
        if (from.equals(to)) {
            return "PERFECT_MATCH";
        }
        if (from.letter() != to.letter()) {
            return "MODE_CHANGE";
        }
        final int diff = Math.abs(to.number() - from.number());
        return (diff == 1 || diff == 11) ? "ADJACENT_KEY" : "JUMP";
    }

    private PlaylistStats computeStats(List<PlaylistTrack> playlist) {
        if (playlist.isEmpty()) {
            return new PlaylistStats(0, 0, 0, 0, Map.of(), 0);
        }
        final double avgBpm = playlist.stream()
                .map(pt -> bpm(pt.track()))
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average().orElse(0.0);
        final double avgEnergy = playlist.stream()
                .map(pt -> energy(pt.track()))
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average().orElse(0.0);
        // Qualité moyenne sur les transitions (hors 1er morceau qui n'en a pas).
        final double avgQuality = playlist.size() > 1
                ? playlist.stream().skip(1).mapToDouble(PlaylistTrack::transitionQuality).average().orElse(0.0)
                : 1.0;
        final Map<String, Long> keyDistribution = new LinkedHashMap<>();
        for (final var pt : playlist) {
            keyDistribution.merge(pt.camelotKey(), 1L, Long::sum);
        }
        final long perfect = playlist.stream()
                .filter(pt -> "PERFECT_MATCH".equals(pt.transitionType()))
                .count();
        return new PlaylistStats(playlist.size(), avgBpm, avgEnergy, avgQuality, keyDistribution, perfect);
    }

    private static Double bpm(EnrichedTrackMetadata track) {
        final AudioFeatures af = track.audioFeatures();
        return af == null ? null : af.bpm();
    }

    private static Double energy(EnrichedTrackMetadata track) {
        final AudioFeatures af = track.audioFeatures();
        return af == null ? null : af.energy();
    }

    private static String identity(EnrichedTrackMetadata track) {
        if (track.sourceId() != null && !track.sourceId().isBlank()) {
            return track.sourceId();
        }
        return (track.artist() + "|" + track.title()).toLowerCase(Locale.ROOT);
    }
}
