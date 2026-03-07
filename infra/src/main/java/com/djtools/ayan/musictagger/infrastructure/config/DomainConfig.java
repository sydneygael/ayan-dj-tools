package com.djtools.ayan.musictagger.infrastructure.config;

import com.djtools.ayan.musictagger.domain.port.in.AudioFileReader;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.domain.port.out.AudioFileWriter;
import com.djtools.ayan.musictagger.domain.port.out.TaggingHistoryRepository;
import com.djtools.ayan.musictagger.domain.usecase.CreatePlanUseCase;
import com.djtools.ayan.musictagger.domain.usecase.ExecutePlanUseCase;
import com.djtools.ayan.musictagger.domain.usecase.ScanMusicUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public ScanMusicUseCase scanMusicUseCase(AudioFileReader audioFileReader) {
        return new ScanMusicUseCase(audioFileReader);
    }

    @Bean
    public CreatePlanUseCase createPlanUseCase(AudioFileReader audioFileReader, MusicMetadataProvider metadataProvider) {
        return new CreatePlanUseCase(audioFileReader, metadataProvider);
    }

    @Bean
    public ExecutePlanUseCase executePlanUseCase(AudioFileWriter audioFileWriter, TaggingHistoryRepository historyRepository) {
        return new ExecutePlanUseCase(audioFileWriter, historyRepository);
    }
}
