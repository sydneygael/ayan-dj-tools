package com.djtools.ayan.musictagger.infrastructure.adapter.out.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Recherche web via Brave Search (si clé configurée) ou DuckDuckGo Instant Answer (fallback gratuit).
 * Brave : https://api.search.brave.com — gratuit jusqu'à 2000 req/mois
 * DDG   : https://api.duckduckgo.com  — gratuit, sans clé, réponses type Wikipedia
 */
public class WebSearchAdapter {

    private static final Logger log = LoggerFactory.getLogger(WebSearchAdapter.class);

    private static final String BRAVE_BASE  = "https://api.search.brave.com";
    private static final String DDG_BASE    = "https://api.duckduckgo.com";
    private static final int    MAX_RESULTS = 5;

    private final RestClient braveClient;
    private final RestClient ddgClient;
    private final String     braveApiKey;

    public WebSearchAdapter(String braveApiKey) {
        this.braveApiKey = braveApiKey != null ? braveApiKey.strip() : "";
        this.braveClient = RestClient.builder().baseUrl(BRAVE_BASE).build();
        this.ddgClient   = RestClient.builder().baseUrl(DDG_BASE).build();
    }

    public WebSearchResult search(String query) {
        if (!braveApiKey.isEmpty()) {
            return searchWithBrave(query);
        }
        return searchWithDdg(query);
    }

    // ─── Brave Search ────────────────────────────────────────────────────────

    private WebSearchResult searchWithBrave(String query) {
        log.debug("Brave search: {}", query);
        try {
            var response = braveClient.get()
                    .uri("/res/v1/web/search?q={q}&count={n}&search_lang=fr", query, MAX_RESULTS)
                    .header("Accept", "application/json")
                    .header("X-Subscription-Token", braveApiKey)
                    .retrieve()
                    .body(BraveResponse.class);

            if (response == null || response.web() == null || response.web().results().isEmpty()) {
                return WebSearchResult.empty(query, "Aucun résultat Brave pour cette requête.");
            }

            var entries = response.web().results().stream()
                    .map(r -> new WebSearchResult.Entry(r.title(), r.description(), r.url()))
                    .toList();

            return new WebSearchResult(query, "Brave Search", entries.getFirst().snippet(), entries);

        } catch (Exception e) {
            log.error("Brave search failed for '{}': {}", query, e.getMessage());
            return WebSearchResult.empty(query, "Erreur Brave Search : " + e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BraveResponse(WebResults web) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WebResults(List<BraveResult> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BraveResult(String title, String url, String description) {}

    // ─── DuckDuckGo Instant Answer (fallback) ────────────────────────────────

    private WebSearchResult searchWithDdg(String query) {
        log.debug("DDG search: {}", query);
        try {
            var response = ddgClient.get()
                    .uri("/?q={q}&format=json&no_html=1&skip_disambig=1", query)
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(DdgResponse.class);

            if (response == null) {
                return WebSearchResult.empty(query, "Aucune réponse DuckDuckGo.");
            }

            var entries = response.toEntries();
            if (entries.isEmpty() && (response.abstractText() == null || response.abstractText().isBlank())) {
                return WebSearchResult.empty(query, "Aucun résultat DDG pour cette requête. Essaie des mots-clés plus précis.");
            }

            var summary = response.abstractText() != null && !response.abstractText().isBlank()
                    ? response.abstractText()
                    : (entries.isEmpty() ? "" : entries.getFirst().snippet());

            return new WebSearchResult(query, "DuckDuckGo", summary, entries);

        } catch (Exception e) {
            log.error("DDG search failed for '{}': {}", query, e.getMessage());
            return WebSearchResult.empty(query, "Erreur recherche web : " + e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DdgResponse(
            @JsonProperty("AbstractText") String abstractText,
            @JsonProperty("AbstractURL")  String abstractUrl,
            @JsonProperty("AbstractSource") String abstractSource,
            @JsonProperty("Answer")       String answer,
            @JsonProperty("RelatedTopics") List<DdgTopic> relatedTopics
    ) {
        List<WebSearchResult.Entry> toEntries() {
            var results = new java.util.ArrayList<WebSearchResult.Entry>();
            if (abstractText != null && !abstractText.isBlank()) {
                results.add(new WebSearchResult.Entry(
                        abstractSource != null ? abstractSource : "Résultat",
                        abstractText,
                        abstractUrl != null ? abstractUrl : ""));
            }
            if (relatedTopics != null) {
                relatedTopics.stream()
                        .filter(t -> t.text() != null && !t.text().isBlank())
                        .limit(MAX_RESULTS - results.size())
                        .forEach(t -> results.add(new WebSearchResult.Entry(
                                t.text().length() > 60 ? t.text().substring(0, 60) + "…" : t.text(),
                                t.text(),
                                t.firstUrl() != null ? t.firstUrl() : "")));
            }
            return results;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DdgTopic(
            @JsonProperty("Text")     String text,
            @JsonProperty("FirstURL") String firstUrl
    ) {}
}
