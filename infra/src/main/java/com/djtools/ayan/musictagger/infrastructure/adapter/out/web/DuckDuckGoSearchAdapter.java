package com.djtools.ayan.musictagger.infrastructure.adapter.out.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

/**
 * Recherche web via l'API JSON DuckDuckGo (gratuite, sans clé).
 * Retourne un résumé textuel (AbstractText ou Answer) si disponible.
 */
@Component
public class DuckDuckGoSearchAdapter {

    private static final Logger log = LoggerFactory.getLogger(DuckDuckGoSearchAdapter.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public DuckDuckGoSearchAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl("https://api.duckduckgo.com")
                .requestFactory(factory)
                .build();
    }

    public Optional<String> search(String query) {
        if (query == null || query.isBlank()) return Optional.empty();
        try {
            log.info("DuckDuckGo search: '{}'", query);
            final var raw = restClient.get()
                    .uri("/?q={q}&format=json&no_html=1&skip_disambig=1", query)
                    .retrieve()
                    .body(String.class);

            if (raw == null || raw.isBlank()) return Optional.empty();

            final var node = objectMapper.readTree(raw);
            final var text = firstNonBlank(
                    node.path("Answer").asText(),
                    node.path("AbstractText").asText());

            if (text.isBlank()) {
                log.info("DuckDuckGo: no summary for '{}'", query);
                return Optional.empty();
            }
            final var heading = node.path("Heading").asText();
            final var result = heading.isBlank() ? text : heading + " — " + text;
            log.info("DuckDuckGo: found result for '{}'", query);
            return Optional.of(result);
        } catch (Exception e) {
            log.warn("DuckDuckGo search failed for '{}': {}", query, e.getMessage());
            return Optional.empty();
        }
    }

    private String firstNonBlank(String... values) {
        for (final var v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }
}
