package com.djtools.ayan.musictagger.infrastructure.adapter.out.web;

import java.util.List;

/**
 * Résultat d'une recherche web retourné au LLM.
 * Format compact : le LLM lit summary + entries pour formuler sa réponse.
 */
public record WebSearchResult(
        String query,
        String source,
        String summary,
        List<Entry> entries
) {
    public record Entry(String title, String snippet, String url) {}

    public static WebSearchResult empty(String query, String reason) {
        return new WebSearchResult(query, "none", reason, List.of());
    }
}
