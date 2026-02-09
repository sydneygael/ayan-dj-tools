package com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs;

import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(DiscogsProperties.class)
public class DiscogsConfig {

    @Bean
    DiscogsApiClient discogsApiClient(DiscogsProperties properties) {
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set("Authorization", "Discogs token=" + properties.token());
                    request.getHeaders().set("User-Agent", "AyanDJTools/1.0");
                    return execution.execute(request, body);
                })
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(DiscogsApiClient.class);
    }

    @Bean
    DiscogsRateLimiter discogsRateLimiter(DiscogsProperties properties) {
        return new DiscogsRateLimiter(properties.rateLimit().requestsPerSecond());
    }

    @Bean
    DiscogsCacheService discogsCacheService(DiscogsProperties properties) {
        return new DiscogsCacheService(properties.rateLimit().cacheTtlMinutes());
    }

    @Bean
    DiscogsMapper discogsMapper() {
        return new DiscogsMapper();
    }

    @Bean("discogsMetadataProvider")
    MusicMetadataProvider discogsMetadataProvider(
            DiscogsApiClient apiClient,
            DiscogsRateLimiter rateLimiter,
            DiscogsCacheService cacheService,
            DiscogsMapper mapper
    ) {
        return new DiscogsMusicMetadataAdapter(apiClient, rateLimiter, cacheService, mapper);
    }
}
