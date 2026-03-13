package com.djtools.ayan.musictagger.domain.port.in;

import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;

import java.util.Optional;

/** Port entrant : lecture des tags ID3 d'un fichier audio. Implémenté par JAudioTaggerAdapter. */
public interface AudioFileReader {

    /** Lit les métadonnées du fichier. Retourne empty si le fichier est illisible. */
    Optional<MusicFileInfo> readTags(Filepath path);
}
