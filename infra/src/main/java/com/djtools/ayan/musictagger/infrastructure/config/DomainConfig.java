package com.djtools.ayan.musictagger.infrastructure.config;

import com.djtools.ayan.musictagger.domain.port.in.AudioFileReader;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.domain.service.CompositeMetadataEnricher;
import com.djtools.ayan.musictagger.domain.usecase.ScanMusicUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DomainConfig {

    @Bean
    public ScanMusicUseCase scanMusicUseCase(AudioFileReader audioFileReader) {
        return new ScanMusicUseCase(audioFileReader);
    }

    @Bean
    public CompositeMetadataEnricher compositeMetadataEnricher(
            @Qualifier("spotifyMetadataProvider") MusicMetadataProvider spotify,
            @Qualifier("discogsMetadataProvider") MusicMetadataProvider discogs,
            @Qualifier("musicBrainzMetadataProvider") MusicMetadataProvider musicBrainz
    ) {
        return new CompositeMetadataEnricher(List.of(spotify, discogs, musicBrainz));
    }
}
