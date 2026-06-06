package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import com.djtools.ayan.musictagger.domain.model.TrackThemes;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto.SoundchartsAudio;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto.SoundchartsGenreRef;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto.SoundchartsLyricsAnalysis;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto.SoundchartsTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class SoundchartsMusicMetadataAdapter implements MusicMetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(SoundchartsMusicMetadataAdapter.class);

    private final SoundchartsApiClient apiClient;
    private final int searchLimit;

    public SoundchartsMusicMetadataAdapter(SoundchartsApiClient apiClient, Integer searchLimit) {
        this.apiClient = apiClient;
        this.searchLimit = searchLimit != null && searchLimit > 0 ? searchLimit : 5;
    }

    @Override
    public EnrichmentResult enrich(String artist, String title) {
        final var normalizedArtist = safeTrim(artist);
        final var normalizedTitle = safeTrim(title);
        if (normalizedTitle.isBlank() && normalizedArtist.isBlank()) {
            return EnrichmentResult.notFound();
        }

        // L'API /song/search/{term} est une recherche par titre — on n'utilise que le titre.
        // Si le titre est absent on replie sur l'artiste.
        final var searchTerm = sanitizeForSearch(normalizedTitle.isBlank() ? normalizedArtist : normalizedTitle);

        try {
            log.info("Soundcharts enrich -> title='{}' artist='{}'", normalizedTitle, normalizedArtist);
            final var search = apiClient.searchSongByName(searchTerm, 0, searchLimit * 2);
            var items = search != null && search.items() != null ? search.items() : List.<SoundchartsTrack>of();
            if (items.isEmpty()) {
                log.info("Soundcharts NOT_FOUND for '{} - {}'", normalizedArtist, normalizedTitle);
                return EnrichmentResult.notFound();
            }

            // Filtre par artiste (contains, case-insensitive). Si le filtre vide la liste, on garde tout.
            if (!normalizedArtist.isBlank()) {
                final var filtered = filterByArtist(items, normalizedArtist);
                if (!filtered.isEmpty()) {
                    items = filtered;
                    log.debug("Soundcharts: {} résultat(s) après filtre artiste '{}'", filtered.size(), normalizedArtist);
                } else {
                    log.debug("Soundcharts: filtre artiste '{}' vide — fallback sur {} résultat(s)", normalizedArtist, items.size());
                }
            }

            final var bestMatch = selectBestMatch(items, normalizedArtist, normalizedTitle);
            final var details = fetchDetails(bestMatch.uuid()).orElse(bestMatch);
            final var themes = fetchLyricsThemes(details.uuid());
            final var metadata = map(details, normalizedArtist, normalizedTitle, themes);

            log.info("Soundcharts enrichment OK for '{} - {}': id={}, genres={}",
                    normalizedArtist, normalizedTitle, metadata.sourceId(), metadata.genres());
            return EnrichmentResult.success(metadata);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Soundcharts NOT_FOUND for '{} - {}'", normalizedArtist, normalizedTitle);
            return EnrichmentResult.notFound();
        } catch (Exception e) {
            log.warn("Soundcharts enrichment failed for '{} - {}': {}", normalizedArtist, normalizedTitle, e.getMessage());
            return EnrichmentResult.error(e.getMessage());
        }
    }

    /** Recherche libre par terme — enrichit chaque résultat via /song/{uuid} pour avoir les audio features et genres. */
    public List<EnrichedTrackMetadata> searchByTerm(String term, int limit) {
        if (term == null || term.isBlank()) return List.of();
        try {
            final var response = apiClient.searchSongByName(term.trim(), 0, Math.max(1, limit));
            if (response == null || response.items() == null) return List.of();
            final var candidates = response.items().stream().filter(Objects::nonNull).toList();
            if (candidates.isEmpty()) return List.of();

            // Fetch détails en parallèle pour récupérer audio features, genres, labels
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                final var futures = candidates.stream()
                        .map(t -> CompletableFuture.supplyAsync(
                                () -> fetchDetails(t.uuid()).orElse(t), executor))
                        .toList();
                return futures.stream()
                        .map(CompletableFuture::join)
                        .map(t -> map(t, t.primaryArtist(), t.name(), fetchLyricsThemes(t.uuid())))
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Soundcharts searchByTerm failed for '{}': {}", term, e.getMessage());
            return List.of();
        }
    }

    private TrackThemes fetchLyricsThemes(String uuid) {
        if (uuid == null || uuid.isBlank()) return null;
        try {
            final var response = apiClient.getLyricsAnalysis(uuid);
            if (response == null || response.object() == null || response.object().isEmpty()) return null;
            final var l = response.object();
            return new TrackThemes(l.topics(), l.themes(), l.mood(), l.sentiment());
        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.Forbidden e) {
            log.debug("Lyrics analysis unavailable for uuid={}: {}", uuid, e.getStatusCode());
            return null;
        } catch (Exception e) {
            log.warn("Lyrics analysis fetch failed for uuid={}: {}", uuid, e.getMessage());
            return null;
        }
    }

    private Optional<SoundchartsTrack> fetchDetails(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return java.util.Optional.empty();
        }
        final var response = apiClient.getSongMetadata(uuid);
        if (response == null || response.object() == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(response.object());
    }

    private List<SoundchartsTrack> filterByArtist(List<SoundchartsTrack> items, String artist) {
        final var key = normalize(artist);
        return items.stream()
                .filter(Objects::nonNull)
                .filter(t -> {
                    final var trackArtist = normalize(t.primaryArtist());
                    return trackArtist.contains(key) || key.contains(trackArtist);
                })
                .toList();
    }

    private SoundchartsTrack selectBestMatch(List<SoundchartsTrack> items, String artist, String title) {
        return items.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(track -> score(track, artist, title)))
                .orElse(items.getFirst());
    }

    private int score(SoundchartsTrack track, String artist, String title) {
        int score = 0;
        final var normalizedArtist = normalize(artist);
        final var normalizedTitle = normalize(title);
        final var trackArtist = normalize(track.primaryArtist());
        final var trackTitle = normalize(track.name());

        if (!normalizedArtist.isBlank() && !trackArtist.isBlank()) {
            if (trackArtist.equals(normalizedArtist)) score += 40;
            else if (trackArtist.contains(normalizedArtist) || normalizedArtist.contains(trackArtist)) score += 25;
        }
        if (!normalizedTitle.isBlank() && !trackTitle.isBlank()) {
            if (trackTitle.equals(normalizedTitle)) score += 60;
            else if (trackTitle.contains(normalizedTitle) || normalizedTitle.contains(trackTitle)) score += 35;
        }
        return score;
    }

    private EnrichedTrackMetadata map(SoundchartsTrack track, String fallbackArtist, String fallbackTitle,
                                       TrackThemes themes) {
        return new EnrichedTrackMetadata(
                track.uuid(),
                firstNonBlank(track.primaryArtist(), fallbackArtist),
                firstNonBlank(track.name(), fallbackTitle),
                null,
                genreRoots(track),
                genreSubs(track),
                track.primaryLabel(),
                track.countryCode(),
                track.isrcValue(),
                List.of("soundcharts"),
                track.releaseYear(),
                null,
                track.durationMs(),
                toAudioFeatures(track.audio()),
                track.languageCode(),
                track.explicit(),
                themes
        );
    }

    /** Genres racines (ex: "electro", "pop"), distincts et non vides. */
    private List<String> genreRoots(SoundchartsTrack track) {
        if (track.genres() == null) return List.of();
        return track.genres().stream()
                .filter(Objects::nonNull)
                .map(SoundchartsGenreRef::root)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();
    }

    /** Sous-genres (ex: "dance", "electronic") aplatis, distincts et non vides → mappés sur styles. */
    private List<String> genreSubs(SoundchartsTrack track) {
        if (track.genres() == null) return List.of();
        return track.genres().stream()
                .filter(Objects::nonNull)
                .flatMap(g -> g.subSafe().stream())
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();
    }

    private AudioFeatures toAudioFeatures(SoundchartsAudio audio) {
        if (audio == null) return null;
        return new AudioFeatures(
                audio.danceability(),
                audio.energy(),
                audio.valence(),
                audio.acousticness(),
                audio.instrumentalness(),
                audio.speechiness(),
                audio.tempo(),
                pitchClassName(audio.key()),
                modeName(audio.mode()),
                audio.timeSignature(),
                audio.liveness(),
                audio.loudness()
        );
    }

    private static final String[] PITCH_CLASSES =
            {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    /** Pitch class Soundcharts (0–11) → nom de note. -1 ou hors borne → null. */
    private String pitchClassName(Integer key) {
        if (key == null || key < 0 || key > 11) return null;
        return PITCH_CLASSES[key];
    }

    /** mode Soundcharts : 1 = major, 0 = minor. */
    private String modeName(Integer mode) {
        if (mode == null) return null;
        return mode == 1 ? "major" : "minor";
    }

    private String firstNonBlank(String first, String fallback) {
        final var one = safeTrim(first);
        return one.isBlank() ? safeTrim(fallback) : one;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    /** Supprime les caractères non-alphanumériques en début et fin (ex: "-Agolo-" → "Agolo"). */
    private String sanitizeForSearch(String value) {
        return value.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$", "").trim();
    }

    private String normalize(String value) {
        return safeTrim(value).toLowerCase(Locale.ROOT);
    }
}
