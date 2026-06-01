package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts;

import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.domain.port.out.EnrichedMetadataCacheRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(SoundchartsProperties.class)
public class SoundchartsConfig {

    @Bean
    SoundchartsApiClient soundchartsApiClient(SoundchartsProperties properties) {
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set("x-app-id", properties.appId());
                    request.getHeaders().set("x-api-key", properties.apiKey());
                    return execution.execute(request, body);
                })
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(SoundchartsApiClient.class);
    }

    @Bean
    SoundchartsMusicMetadataAdapter soundchartsMetadataProvider(
            SoundchartsApiClient apiClient,
            SoundchartsProperties properties
    ) {
        return new SoundchartsMusicMetadataAdapter(apiClient, properties.searchLimit());
    }

    @Bean
    @Primary
    MusicMetadataProvider soundchartsFirstMetadataProvider(
            EnrichedMetadataCacheRepository enrichedMetadataCacheRepository,
            SoundchartsMusicMetadataAdapter soundchartsMetadataProvider,
            @Qualifier("spotifyMetadataProvider") MusicMetadataProvider fallbackProvider
    ) {
        return new SoundchartsFirstMetadataProvider(
                enrichedMetadataCacheRepository, soundchartsMetadataProvider, fallbackProvider);
    }
}
