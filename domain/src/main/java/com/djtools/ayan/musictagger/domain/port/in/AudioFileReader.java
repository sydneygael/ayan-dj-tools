package com.djtools.ayan.musictagger.domain.port.out;

import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;

import java.util.Optional;

public interface AudioFileReader {

    Optional<MusicFileInfo> readTags(Filepath path);
}
