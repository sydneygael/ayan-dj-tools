package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify;

import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(SpotifyProperties.class)
public class SpotifyConfig {

    @Bean
    SpotifyTokenService spotifyTokenService(SpotifyProperties properties) {
        return new SpotifyTokenService(
                properties.authUrl(),
                properties.clientId(),
                properties.clientSecret()
        );
    }

    @Bean
    SpotifyApiClient spotifyApiClient(SpotifyTokenService tokenService) {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.spotify.com/v1")
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(tokenService.getAccessToken());
                    return execution.execute(request, body);
                })
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(SpotifyApiClient.class);
    }

    @Bean
    SpotifyRateLimiter spotifyRateLimiter(SpotifyProperties properties) {
        return new SpotifyRateLimiter(properties.rateLimit().requestsPerSecond());
    }

    @Bean
    SpotifyCacheService spotifyCacheService(SpotifyProperties properties) {
        return new SpotifyCacheService(properties.rateLimit().cacheTtlMinutes());
    }

    @Bean
    SpotifyMapper spotifyMapper() {
        return new SpotifyMapper();
    }

    @Bean("spotifyMetadataProvider")
    SpotifyMusicMetadataAdapter spotifyMetadataProvider(
            SpotifyApiClient apiClient,
            SpotifyRateLimiter rateLimiter,
            SpotifyCacheService cacheService,
            SpotifyMapper mapper
    ) {
        return new SpotifyMusicMetadataAdapter(apiClient, rateLimiter, cacheService, mapper);
    }

}
