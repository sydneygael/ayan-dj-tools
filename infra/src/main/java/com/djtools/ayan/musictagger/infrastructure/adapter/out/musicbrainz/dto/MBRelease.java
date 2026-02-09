package com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MBRelease(
        String id,
        String title,
        String status,
        String date,
        @JsonProperty("release-group") MBReleaseGroup releaseGroup
) {}
