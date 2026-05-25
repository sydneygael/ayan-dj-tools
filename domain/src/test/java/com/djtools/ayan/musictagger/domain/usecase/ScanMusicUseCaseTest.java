package com.djtools.ayan.musictagger.domain.usecase;

import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.in.AudioFileReader;
import com.djtools.ayan.musictagger.domain.port.out.ScannedTrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanMusicUseCaseTest {

    @Mock
    private AudioFileReader audioFileReader;
    @Mock
    private ScannedTrackRepository scannedTrackRepository;

    private ScanMusicUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ScanMusicUseCase(audioFileReader, scannedTrackRepository);
    }

    @Test
    void shouldScanMultipleFiles() {
        var path1 = new Filepath("/music/song1.mp3");
        var path2 = new Filepath("/music/song2.mp3");

        var info1 = new MusicFileInfo(path1, "song1.mp3", "Artist1", "Title1", "Album1", "Pop", "120", "Am", 1000, 123456);
        var info2 = new MusicFileInfo(path2, "song2.mp3", "Artist2", "Title2", null, null, null, null, 2000, 654321);

        when(audioFileReader.readTags(path1)).thenReturn(Optional.of(info1));
        when(audioFileReader.readTags(path2)).thenReturn(Optional.of(info2));

        List<MusicFileInfo> results = useCase.execute(List.of(path1, path2));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).artist()).isEqualTo("Artist1");
        assertThat(results.get(1).artist()).isEqualTo("Artist2");
    }

    @Test
    void shouldSkipUnreadableFiles() {
        var path1 = new Filepath("/music/good.mp3");
        var path2 = new Filepath("/music/bad.mp3");

        var info = new MusicFileInfo(path1, "good.mp3", "Artist", "Title", null, null, null, null, 1000, 123456);

        when(audioFileReader.readTags(path1)).thenReturn(Optional.of(info));
        when(audioFileReader.readTags(path2)).thenReturn(Optional.empty());

        List<MusicFileInfo> results = useCase.execute(List.of(path1, path2));

        assertThat(results).hasSize(1);
    }

    @Test
    void shouldDetectMissingTags() {
        var path = new Filepath("/music/incomplete.mp3");
        var info = new MusicFileInfo(path, "incomplete.mp3", "Artist", "Title", null, null, null, null, 1000, 123456);

        when(audioFileReader.readTags(path)).thenReturn(Optional.of(info));

        var report = useCase.detectMissingTags(path);

        assertThat(report.hasMissingTags()).isTrue();
        assertThat(report.missingTags()).containsExactlyInAnyOrder("album", "genre", "bpm", "key");
        assertThat(report.missingCount()).isEqualTo(4);
    }

    @Test
    void shouldReportNoMissingTagsWhenComplete() {
        var path = new Filepath("/music/complete.mp3");
        var info = new MusicFileInfo(path, "complete.mp3", "Artist", "Title", "Album", "Pop", "128", "Cm", 1000, 123456);

        when(audioFileReader.readTags(path)).thenReturn(Optional.of(info));

        var report = useCase.detectMissingTags(path);

        assertThat(report.hasMissingTags()).isFalse();
        assertThat(report.missingCount()).isZero();
    }

    @Test
    void shouldReportAllTagsMissingWhenFileUnreadable() {
        var path = new Filepath("/music/missing.mp3");

        when(audioFileReader.readTags(path)).thenReturn(Optional.empty());

        var report = useCase.detectMissingTags(path);

        assertThat(report.hasMissingTags()).isTrue();
        assertThat(report.missingCount()).isEqualTo(6);
    }
}
