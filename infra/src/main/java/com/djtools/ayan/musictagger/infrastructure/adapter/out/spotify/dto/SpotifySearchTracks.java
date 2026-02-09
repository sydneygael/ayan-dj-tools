package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto;

import java.util.List;

public record SpotifySearchTracks(List<SpotifyTrackItem> items, int total) {}
