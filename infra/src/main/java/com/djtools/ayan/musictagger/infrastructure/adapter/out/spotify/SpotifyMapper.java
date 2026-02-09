package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifyAudioFeatures;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifyTrackItem;

import java.util.List;

public class SpotifyMapper {

    public EnrichedTrackMetadata toEnrichedMetadata(SpotifyTrackItem track, SpotifyAudioFeatures audioFeatures, List<String> genres) {
        return new EnrichedTrackMetadata(
                track.id(),
                track.primaryArtist(),
                track.name(),
                track.album() != null ? track.album().name() : null,
                genres != null ? genres : List.of(),
                track.album() != null ? track.album().releaseYear() : 0,
                track.popularity(),
                track.duration_ms(),
                mapAudioFeatures(audioFeatures)
        );
    }

    private AudioFeatures mapAudioFeatures(SpotifyAudioFeatures src) {
        if (src == null) {
            return null;
        }
        return new AudioFeatures(
                src.danceability(),
                src.energy(),
                src.valence(),
                src.acousticness(),
                src.instrumentalness(),
                src.speechiness(),
                src.bpm(),
                src.musicalKey(),
                src.musicalMode(),
                src.time_signature()
        );
    }
}
