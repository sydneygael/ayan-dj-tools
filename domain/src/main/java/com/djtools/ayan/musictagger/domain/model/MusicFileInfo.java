package com.djtools.ayan.musictagger.domain.model;

import com.djtools.ayan.musictagger.domain.model.vo.Filepath;

/** Informations extraites d'un fichier audio : métadonnées ID3 + infos fichier. */
public record MusicFileInfo(
        Filepath filepath,
        String filename,
        String artist,
        String title,
        String album,
        String genre,
        String bpm,
        String key,
        long fileSize,
        long lastModified,
        boolean seratoAnalyzed
) {

    public boolean hasArtistAndTitle() {
        return isPresent(artist) && isPresent(title);
    }

    public boolean isMissingTag(String tagName) {
        return switch (tagName.toLowerCase()) {
            case "artist" -> !isPresent(artist);
            case "title" -> !isPresent(title);
            case "album" -> !isPresent(album);
            case "genre" -> !isPresent(genre);
            case "bpm" -> !isPresent(bpm);
            case "key" -> !isPresent(key);
            default -> false;
        };
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
