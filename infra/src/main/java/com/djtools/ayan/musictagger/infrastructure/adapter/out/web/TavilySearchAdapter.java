package com.djtools.ayan.musictagger.infrastructure.adapter.out.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class TavilySearchAdapter {

    private static final Logger log = LoggerFactory.getLogger(TavilySearchAdapter.class);

    private final RestClient restClient;
    private final int maxResults;
    private final boolean enabled;

    public TavilySearchAdapter(
            @Value("${tavily.api-key:}") String apiKey,
            @Value("${tavily.max-results:3}") int maxResults) {
        this.maxResults = maxResults;
        this.enabled = apiKey != null && !apiKey.isBlank();
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl("https://api.tavily.com/search")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .requestFactory(factory)
                .build();
    }

    public Optional<String> search(String query) {
        if (query == null || query.isBlank()) return Optional.empty();
        if (!enabled) {
            log.warn("Tavily API key not configured — web search skipped");
            return Optional.empty();
        }
        try {
            log.info("Tavily search: '{}'", query);
            final var response = restClient.post()
                    .body(new TavilySearchRequest(query, "basic", maxResults))
                    .retrieve()
                    .body(TavilySearchResponse.class);

            if (response == null || CollectionUtils.isEmpty(response.results())) {
                log.info("Tavily: no results for '{}'", query);
                return Optional.empty();
            }

            final var top = response.results().getFirst();
            final var text = top.content() != null && !top.content().isBlank()
                    ? top.content()
                    : top.title();
            if (text == null || text.isBlank()) return Optional.empty();

            final var result = top.title() != null && !top.title().isBlank() && !top.title().equals(text)
                    ? top.title() + " — " + text
                    : text;
            log.info("Tavily: result for '{}' (score={})", query, top.score());
            return Optional.of(result);
        } catch (Exception e) {
            log.warn("Tavily search failed for '{}': {}", query, e.getMessage());
            return Optional.empty();
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record TavilySearchRequest(String query, String searchDepth, int maxResults) {}

    record TavilySearchResponse(List<Result> results) {
        record Result(String title, String url, String content, Double score) {}
    }
}
