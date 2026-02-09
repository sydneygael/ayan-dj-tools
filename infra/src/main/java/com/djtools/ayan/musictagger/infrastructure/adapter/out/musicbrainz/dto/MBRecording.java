package com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MBRecording(
        String id,
        int score,
        String title,
        Integer length,
        @JsonProperty("artist-credit") List<MBArtistCredit> artistCredit,
        List<MBRelease> releases,
        List<String> isrcs,
        List<MBTag> tags
) {}
