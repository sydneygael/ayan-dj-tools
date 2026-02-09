package com.djtools.ayan.musictagger.domain.service;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeMetadataEnricherTest {

    @Test
    void shouldMergeAllSuccessfulProviders() {
        var spotify = provider(EnrichmentResult.success(new EnrichedTrackMetadata(
                "spotify:1", "Artist", "Title", "Album",
                List.of("Electronic"), List.of(), null, null, null, List.of(),
                2024, 80, 210000,
                new AudioFeatures(0.8, 0.7, 0.6, 0.1, 0.05, 0.03, 128.0, "C", "Major", 4)
        )));
        var discogs = provider(EnrichmentResult.success(new EnrichedTrackMetadata(
                "discogs:2", "Artist", "Title", null,
                List.of("Electronic", "Dance"), List.of("Deep House", "Tech House"), "Defected", "UK", null, List.of(),
                2024, 0, 0, null
        )));
        var musicBrainz = provider(EnrichmentResult.success(new EnrichedTrackMetadata(
                "mb:3", "Artist", "Title", "Album",
                List.of(), List.of(), null, null, "GBAYE0000001", List.of("electronic", "deep house"),
                2024, 0, 210000, null
        )));

        var enricher = new CompositeMetadataEnricher(List.of(spotify, discogs, musicBrainz));
        var result = enricher.enrich("Artist", "Title");

        assertThat(result.isSuccess()).isTrue();
        var data = result.data();
        assertThat(data.sourceId()).isEqualTo("spotify:1");
        assertThat(data.artist()).isEqualTo("Artist");
        assertThat(data.genres()).containsExactly("Electronic", "Dance");
        assertThat(data.styles()).containsExactly("Deep House", "Tech House");
        assertThat(data.label()).isEqualTo("Defected");
        assertThat(data.country()).isEqualTo("UK");
        assertThat(data.isrc()).isEqualTo("GBAYE0000001");
        assertThat(data.tags()).containsExactly("electronic", "deep house");
        assertThat(data.audioFeatures()).isNotNull();
        assertThat(data.audioFeatures().bpm()).isEqualTo(128.0);
        assertThat(data.popularity()).isEqualTo(80);
    }

    @Test
    void shouldMergePartialResultsWhenOneProviderFails() {
        var spotify = provider(EnrichmentResult.success(new EnrichedTrackMetadata(
                "spotify:1", "Artist", "Title", "Album",
                List.of("Pop"), List.of(), null, null, null, List.of(),
                2023, 70, 200000, null
        )));
        var discogs = provider(EnrichmentResult.error("Connection timeout"));
        var musicBrainz = provider(EnrichmentResult.success(new EnrichedTrackMetadata(
                "mb:3", "Artist", "Title", null,
                List.of(), List.of(), null, null, "USRC10000001", List.of("pop"),
                0, 0, 0, null
        )));

        var enricher = new CompositeMetadataEnricher(List.of(spotify, discogs, musicBrainz));
        var result = enricher.enrich("Artist", "Title");

        assertThat(result.isSuccess()).isTrue();
        var data = result.data();
        assertThat(data.sourceId()).isEqualTo("spotify:1");
        assertThat(data.isrc()).isEqualTo("USRC10000001");
        assertThat(data.tags()).containsExactly("pop");
    }

    @Test
    void shouldReturnErrorWhenAllProvidersFail() {
        var spotify = provider(EnrichmentResult.error("Spotify error"));
        var discogs = provider(EnrichmentResult.error("Discogs error"));
        var musicBrainz = provider(EnrichmentResult.error("MusicBrainz error"));

        var enricher = new CompositeMetadataEnricher(List.of(spotify, discogs, musicBrainz));
        var result = enricher.enrich("Artist", "Title");

        assertThat(result).isInstanceOf(EnrichmentResult.Error.class);
        assertThat(((EnrichmentResult.Error) result).message()).contains("Spotify error");
    }

    @Test
    void shouldReturnNotFoundWhenAllProvidersReturnNotFound() {
        var spotify = provider(EnrichmentResult.notFound());
        var discogs = provider(EnrichmentResult.notFound());
        var musicBrainz = provider(EnrichmentResult.notFound());

        var enricher = new CompositeMetadataEnricher(List.of(spotify, discogs, musicBrainz));
        var result = enricher.enrich("Unknown", "Song");

        assertThat(result).isInstanceOf(EnrichmentResult.NotFound.class);
    }

    @Test
    void shouldMergeSuccessesIgnoringNotFound() {
        var spotify = provider(EnrichmentResult.notFound());
        var discogs = provider(EnrichmentResult.success(new EnrichedTrackMetadata(
                "discogs:5", "Artist", "Title", null,
                List.of("Rock"), List.of("Indie Rock"), "SubPop", "US", null, List.of(),
                2022, 0, 0, null
        )));
        var musicBrainz = provider(EnrichmentResult.notFound());

        var enricher = new CompositeMetadataEnricher(List.of(spotify, discogs, musicBrainz));
        var result = enricher.enrich("Artist", "Title");

        assertThat(result.isSuccess()).isTrue();
        var data = result.data();
        assertThat(data.sourceId()).isEqualTo("discogs:5");
        assertThat(data.genres()).containsExactly("Rock");
        assertThat(data.styles()).containsExactly("Indie Rock");
        assertThat(data.label()).isEqualTo("SubPop");
    }

    @Test
    void shouldHandleProviderException() {
        MusicMetadataProvider throwing = (artist, title) -> { throw new RuntimeException("Unexpected"); };
        var discogs = provider(EnrichmentResult.success(new EnrichedTrackMetadata(
                "discogs:6", "Artist", "Title", null,
                List.of("Techno"), List.of(), null, "DE", null, List.of(),
                2021, 0, 0, null
        )));

        var enricher = new CompositeMetadataEnricher(List.of(throwing, discogs));
        var result = enricher.enrich("Artist", "Title");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data().genres()).containsExactly("Techno");
    }

    private static MusicMetadataProvider provider(EnrichmentResult result) {
        return (artist, title) -> result;
    }
}
