package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts;

import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import com.djtools.ayan.musictagger.domain.port.out.EnrichedMetadataCacheRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(SoundchartsProperties.class)
public class SoundchartsConfig {

    @Bean
    SoundchartsApiClient soundchartsApiClient(SoundchartsProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set("x-app-id", properties.appId());
                    request.getHeaders().set("x-api-key", properties.apiKey());
                    return execution.execute(request, body);
                })
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builderFor(adapter).build();
        return proxyFactory.createClient(SoundchartsApiClient.class);
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
