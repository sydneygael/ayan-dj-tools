package com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs.dto.DiscogsRelease;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs.dto.DiscogsSearchResult;

import java.util.List;

public class DiscogsMapper {

    public EnrichedTrackMetadata toEnrichedMetadata(DiscogsSearchResult searchResult, DiscogsRelease release) {
        String artist = extractArtist(searchResult.title());
        String title = extractTitle(searchResult.title());
        String label = release.labels() != null && !release.labels().isEmpty()
                ? release.labels().getFirst().name()
                : null;
        int year = release.year() != null ? release.year() : (searchResult.year() != null ? searchResult.year() : 0);

        return new EnrichedTrackMetadata(
                "discogs:" + searchResult.id(),
                artist,
                title,
                null,
                release.genres() != null ? release.genres() : List.of(),
                release.styles() != null ? release.styles() : List.of(),
                label,
                release.country(),
                null,
                List.of(),
                year,
                0,
                0,
                null
        );
    }

    private String extractArtist(String discogsTitle) {
        if (discogsTitle == null) return null;
        int dashIndex = discogsTitle.indexOf(" - ");
        return dashIndex >= 0 ? discogsTitle.substring(0, dashIndex).trim() : null;
    }

    private String extractTitle(String discogsTitle) {
        if (discogsTitle == null) return null;
        int dashIndex = discogsTitle.indexOf(" - ");
        return dashIndex >= 0 ? discogsTitle.substring(dashIndex + 3).trim() : discogsTitle;
    }
}
