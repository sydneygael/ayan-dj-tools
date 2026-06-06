package com.djtools.ayan.musictagger.domain.port.out;

import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;

import java.util.List;
import java.util.Optional;

public interface ScannedTrackRepository {
    void save(MusicFileInfo track);
    void saveAll(List<MusicFileInfo> tracks);
    Optional<MusicFileInfo> findByFilepath(String filepath);
    List<MusicFileInfo> findAll();
    List<MusicFileInfo> findByArtist(String artist, int limit);
    Optional<MusicFileInfo> findByArtistAndTitle(String artist, String title);
    void delete(String filepath);
}
