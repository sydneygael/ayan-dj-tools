package com.djtools.ayan.musictagger.infrastructure.config;

import com.djtools.ayan.musictagger.infrastructure.adapter.out.web.WebSearchAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class WebSearchConfig {

    @Bean
    WebSearchAdapter webSearchAdapter(
            @Value("${web.search.brave-api-key:}") String braveApiKey) {
        return new WebSearchAdapter(braveApiKey);
    }
}
