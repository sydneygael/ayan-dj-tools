package com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence;

import com.djtools.ayan.musictagger.domain.model.MusicFileInfo;
import com.djtools.ayan.musictagger.domain.model.vo.Filepath;
import com.djtools.ayan.musictagger.domain.port.out.ScannedTrackRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class H2ScannedTrackRepository implements ScannedTrackRepository {

    private final JdbcClient jdbcClient;

    public H2ScannedTrackRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void save(MusicFileInfo track) {
        jdbcClient.sql("""
                MERGE INTO scanned_tracks (filepath, filename, artist, title, album, genre, bpm, key_name, file_size, last_modified)
                KEY (filepath)
                VALUES (:filepath, :filename, :artist, :title, :album, :genre, :bpm, :keyName, :fileSize, :lastModified)
                """)
                .param("filepath", track.filepath().value())
                .param("filename", track.filename())
                .param("artist", track.artist())
                .param("title", track.title())
                .param("album", track.album())
                .param("genre", track.genre())
                .param("bpm", track.bpm())
                .param("keyName", track.key())
                .param("fileSize", track.fileSize())
                .param("lastModified", track.lastModified())
                .update();
    }

    @Override
    public void saveAll(List<MusicFileInfo> tracks) {
        tracks.forEach(this::save);
    }

    @Override
    public Optional<MusicFileInfo> findByFilepath(String filepath) {
        return jdbcClient.sql("SELECT * FROM scanned_tracks WHERE filepath = :filepath")
                .param("filepath", filepath)
                .query((rs, rowNum) -> mapRow(rs))
                .optional();
    }

    @Override
    public List<MusicFileInfo> findAll() {
        return jdbcClient.sql("SELECT * FROM scanned_tracks ORDER BY scanned_at DESC")
                .query((rs, rowNum) -> mapRow(rs))
                .list();
    }

    @Override
    public void delete(String filepath) {
        jdbcClient.sql("DELETE FROM scanned_tracks WHERE filepath = :filepath")
                .param("filepath", filepath)
                .update();
    }

    private static MusicFileInfo mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new MusicFileInfo(
                new Filepath(rs.getString("filepath")),
                rs.getString("filename"),
                rs.getString("artist"),
                rs.getString("title"),
                rs.getString("album"),
                rs.getString("genre"),
                rs.getString("bpm"),
                rs.getString("key_name"),
                rs.getLong("file_size"),
                rs.getLong("last_modified")
        );
    }
}
