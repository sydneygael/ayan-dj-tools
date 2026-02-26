package com.djtools.ayan.musictagger.infrastructure.config;

import com.djtools.ayan.musictagger.domain.port.in.AudioFileReader;
import com.djtools.ayan.musictagger.domain.usecase.ScanMusicUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public ScanMusicUseCase scanMusicUseCase(AudioFileReader audioFileReader) {
        return new ScanMusicUseCase(audioFileReader);
    }
}
