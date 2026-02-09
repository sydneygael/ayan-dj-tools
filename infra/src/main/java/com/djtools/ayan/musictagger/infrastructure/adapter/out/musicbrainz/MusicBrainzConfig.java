package com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz;

import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(MusicBrainzProperties.class)
public class MusicBrainzConfig {

    @Bean
    MusicBrainzApiClient musicBrainzApiClient(MusicBrainzProperties properties) {
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set("User-Agent", properties.userAgent());
                    request.getHeaders().set("Accept", "application/json");
                    return execution.execute(request, body);
                })
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(MusicBrainzApiClient.class);
    }

    @Bean
    MusicBrainzRateLimiter musicBrainzRateLimiter(MusicBrainzProperties properties) {
        return new MusicBrainzRateLimiter(properties.rateLimit().requestsPerSecond());
    }

    @Bean
    MusicBrainzCacheService musicBrainzCacheService(MusicBrainzProperties properties) {
        return new MusicBrainzCacheService(properties.rateLimit().cacheTtlMinutes());
    }

    @Bean
    MusicBrainzMapper musicBrainzMapper() {
        return new MusicBrainzMapper();
    }

    @Bean("musicBrainzMetadataProvider")
    MusicMetadataProvider musicBrainzMetadataProvider(
            MusicBrainzApiClient apiClient,
            MusicBrainzRateLimiter rateLimiter,
            MusicBrainzCacheService cacheService,
            MusicBrainzMapper mapper
    ) {
        return new MusicBrainzMusicMetadataAdapter(apiClient, rateLimiter, cacheService, mapper);
    }
}
