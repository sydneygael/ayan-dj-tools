package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** ISRC renvoyé par Soundcharts sous forme d'objet : {value, countryCode, countryName}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SoundchartsIsrc(String value, String countryCode, String countryName) {}
