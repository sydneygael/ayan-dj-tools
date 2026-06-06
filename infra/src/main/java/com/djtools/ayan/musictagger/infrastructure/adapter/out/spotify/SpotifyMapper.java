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
                List.of(),
                null,
                null,
                null,
                List.of(),
                track.album() != null ? track.album().releaseYear() : null,
                track.popularity(),
                track.duration_ms(),
                mapAudioFeatures(audioFeatures),
                null,
                null,
                null
        );
    }

    private AudioFeatures mapAudioFeatures(SpotifyAudioFeatures src) {
        if (src == null) {
            return null;
        }
        return new AudioFeatures(
                src.danceability() != null ? (double) src.danceability() : null,
                src.energy()       != null ? (double) src.energy()       : null,
                src.valence()      != null ? (double) src.valence()      : null,
                src.acousticness() != null ? (double) src.acousticness() : null,
                src.instrumentalness() != null ? (double) src.instrumentalness() : null,
                src.speechiness()  != null ? (double) src.speechiness()  : null,
                src.bpm(),
                src.musicalKey(),
                src.musicalMode(),
                src.time_signature(),
                null,
                null
        );
    }
}
