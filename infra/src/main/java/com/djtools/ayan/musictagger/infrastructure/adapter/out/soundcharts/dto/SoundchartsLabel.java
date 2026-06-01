package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Label musical : {name, type} (ex: {"Island", "Universal"}). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SoundchartsLabel(String name, String type) {}
