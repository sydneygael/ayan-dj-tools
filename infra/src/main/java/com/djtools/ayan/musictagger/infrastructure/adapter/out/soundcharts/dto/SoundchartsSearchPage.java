package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SoundchartsSearchPage(Integer offset, Integer limit, Integer total) {}
