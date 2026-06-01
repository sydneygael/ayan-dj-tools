package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto.SoundchartsTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
        final var term = (normalizedArtist + " " + normalizedTitle).trim();
        if (term.isBlank()) {
            return EnrichmentResult.notFound();
        }

        try {
            log.info("Soundcharts enrich -> term='{}'", term);
            final var search = apiClient.searchSongByName(term, 0, searchLimit);
            final var items = search != null && search.items() != null ? search.items() : List.<SoundchartsTrack>of();
            if (items.isEmpty()) {
                log.info("Soundcharts NOT_FOUND for '{} - {}'", normalizedArtist, normalizedTitle);
                return EnrichmentResult.notFound();
            }

            final var bestMatch = selectBestMatch(items, normalizedArtist, normalizedTitle);
            final var details = fetchDetails(bestMatch.uuid()).orElse(bestMatch);
            final var metadata = map(details, normalizedArtist, normalizedTitle);

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

    private java.util.Optional<SoundchartsTrack> fetchDetails(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return java.util.Optional.empty();
        }
        final var response = apiClient.getSongMetadata(uuid);
        if (response == null || response.object() == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(response.object());
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

    private EnrichedTrackMetadata map(SoundchartsTrack track, String fallbackArtist, String fallbackTitle) {
        final var genres = track.genres() == null
                ? List.<String>of()
                : track.genres().stream()
                .map(g -> g != null ? g.name() : null)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .toList();

        return new EnrichedTrackMetadata(
                track.uuid(),
                firstNonBlank(track.primaryArtist(), fallbackArtist),
                firstNonBlank(track.name(), fallbackTitle),
                null,
                genres,
                List.of(),
                track.label(),
                track.country(),
                track.isrc(),
                List.of("soundcharts"),
                track.releaseYear(),
                null,
                track.duration(),
                (AudioFeatures) null
        );
    }

    private String firstNonBlank(String first, String fallback) {
        final var one = safeTrim(first);
        return one.isBlank() ? safeTrim(fallback) : one;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String value) {
        return safeTrim(value).toLowerCase(Locale.ROOT);
    }
}
