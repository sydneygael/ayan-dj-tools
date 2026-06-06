package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.domain.port.out.EnrichedMetadataCacheRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoundchartsFirstMetadataProviderTest {

    @Mock SoundchartsMusicMetadataAdapter soundcharts;
    @Mock MusicMetadataProvider fallback;
    @Mock EnrichedMetadataCacheRepository cache;

    @Test
    void shouldReturnSoundchartsWhenSuccessful() {
        var data = new EnrichedTrackMetadata("id", "Artist", "Title", null, List.of(), List.of(),
                null, null, null, List.of(), null, null, null, null, null, null);
        when(soundcharts.enrich("Artist", "Title")).thenReturn(EnrichmentResult.success(data));

        var provider = new SoundchartsFirstMetadataProvider(cache, soundcharts, fallback);
        var result = provider.enrich("Artist", "Title");

        assertThat(result).isInstanceOf(EnrichmentResult.Success.class);
        verify(soundcharts).enrich("Artist", "Title");
    }

    @Test
    void shouldFallbackWhenSoundchartsNotFound() {
        when(soundcharts.enrich("Artist", "Title")).thenReturn(EnrichmentResult.notFound());
        when(fallback.enrich("Artist", "Title")).thenReturn(EnrichmentResult.error("fallback"));

        var provider = new SoundchartsFirstMetadataProvider(cache, soundcharts, fallback);
        var result = provider.enrich("Artist", "Title");

        assertThat(result).isInstanceOf(EnrichmentResult.Error.class);
        verify(fallback).enrich("Artist", "Title");
    }
}
