package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify;

import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifyArtist;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifyAudioFeatures;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifySearchResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

public interface SpotifyApiClient {

    @GetExchange("/search")
    SpotifySearchResponse searchTracks(
            @RequestParam("q") String query,
            @RequestParam("type") String type,
            @RequestParam("limit") int limit
    );

    @GetExchange("/audio-features/{id}")
    SpotifyAudioFeatures getAudioFeatures(@PathVariable("id") String id);

    @GetExchange("/artists/{id}")
    SpotifyArtist getArtist(@PathVariable("id") String id);
}
