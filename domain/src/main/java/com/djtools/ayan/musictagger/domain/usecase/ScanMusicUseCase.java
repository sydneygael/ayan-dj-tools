package com.djtools.ayan.musictagger.domain.usecase;

import com.djtools.ayan.musictagger.domain.model.MissingTagsReport;
import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.out.AudioFileReader;

import java.util.ArrayList;
import java.util.List;

public class ScanMusicUseCase {

    private static final List<String> ALL_TAGS = List.of("artist", "title", "album", "genre", "bpm", "key");

    private final AudioFileReader audioFileReader;

    public ScanMusicUseCase(AudioFileReader audioFileReader) {
        this.audioFileReader = audioFileReader;
    }

    public List<MusicFileInfo> execute(List<Filepath> paths) {
        return paths.stream()
                .map(audioFileReader::readTags)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    public MissingTagsReport detectMissingTags(Filepath path) {
        return audioFileReader.readTags(path)
                .map(info -> {
                    List<String> missing = ALL_TAGS.stream()
                            .filter(info::isMissingTag)
                            .toList();
                    return new MissingTagsReport(path, missing);
                })
                .orElse(new MissingTagsReport(path, ALL_TAGS));
    }
}
