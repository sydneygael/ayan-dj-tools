package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SoundchartsExternalIds(
        String spotify,
        String youtube,
        String deezer,
        String appleMusic
) {}
