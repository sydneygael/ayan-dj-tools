package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "soundcharts")
public record SoundchartsProperties(
        String appId,
        String apiKey,
        String baseUrl,
        Integer searchLimit
) {}
