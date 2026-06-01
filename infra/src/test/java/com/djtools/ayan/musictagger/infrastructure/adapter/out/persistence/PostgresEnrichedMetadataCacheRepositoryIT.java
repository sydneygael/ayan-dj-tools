package com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class PostgresEnrichedMetadataCacheRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private PostgresEnrichedMetadataCacheRepository repository;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        dataSource.setDriverClassName("org.postgresql.Driver");
        var jdbcClient = JdbcClient.create(dataSource);

        jdbcClient.sql("""
                CREATE TABLE IF NOT EXISTS enriched_track_metadata (
                    artist_key     VARCHAR(512) NOT NULL,
                    title_key      VARCHAR(512) NOT NULL,
                    source_id      VARCHAR(255),
                    artist         VARCHAR(512),
                    title          VARCHAR(512),
                    album          VARCHAR(512),
                    genres         TEXT,
                    styles         TEXT,
                    label          VARCHAR(255),
                    country        VARCHAR(255),
                    isrc           VARCHAR(64),
                    tags           TEXT,
                    release_year   INTEGER,
                    popularity     INTEGER,
                    duration_ms    BIGINT,
                    audio_features TEXT,
                    fetched_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (artist_key, title_key)
                )
                """).update();
        jdbcClient.sql("TRUNCATE enriched_track_metadata").update();

        repository = new PostgresEnrichedMetadataCacheRepository(
                jdbcClient, JsonMapper.builder().findAndAddModules().build());
    }

    @Test
    void shouldPersistAndRetrieveFullMetadata() {
        var metadata = new EnrichedTrackMetadata(
                "sc:abc", "Daft Punk", "One More Time", "Discovery",
                List.of("House", "French House"), List.of("Electronic"),
                "Virgin", "FR", "GBDUW0000059", List.of("soundcharts"),
                2001, 85, 320357L,
                new AudioFeatures(0.8, 0.9, 0.7, 0.1, 0.0, 0.05, 123.0, "F#", "minor", 4));

        repository.put("Daft Punk", "One More Time", metadata);
        var found = repository.get("Daft Punk", "One More Time");

        assertThat(found).isPresent();
        var result = found.get();
        assertThat(result.sourceId()).isEqualTo("sc:abc");
        assertThat(result.artist()).isEqualTo("Daft Punk");
        assertThat(result.genres()).containsExactly("House", "French House");
        assertThat(result.styles()).containsExactly("Electronic");
        assertThat(result.tags()).containsExactly("soundcharts");
        assertThat(result.label()).isEqualTo("Virgin");
        assertThat(result.releaseYear()).isEqualTo(2001);
        assertThat(result.popularity()).isEqualTo(85);
        assertThat(result.durationMs()).isEqualTo(320357L);
        assertThat(result.audioFeatures()).isNotNull();
        assertThat(result.audioFeatures().bpm()).isEqualTo(123.0);
        assertThat(result.audioFeatures().musicalKey()).isEqualTo("F#");
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        assertThat(repository.get("Unknown", "Track")).isEmpty();
    }

    @Test
    void shouldMatchCaseInsensitivelyAndUpsert() {
        var first = new EnrichedTrackMetadata(
                "id1", "Artist", "Title", null, List.of("Pop"), List.of(),
                null, null, null, List.of(), null, null, null, null);
        repository.put("Artist", "Title", first);

        var updated = new EnrichedTrackMetadata(
                "id2", "Artist", "Title", "New Album", List.of("Rock"), List.of(),
                "Label", null, null, List.of(), 2020, null, null, null);
        repository.put("  ARTIST  ", "title", updated);

        var found = repository.get("artist", "TITLE");
        assertThat(found).isPresent();
        assertThat(found.get().sourceId()).isEqualTo("id2");
        assertThat(found.get().album()).isEqualTo("New Album");
        assertThat(found.get().genres()).containsExactly("Rock");
    }

    @Test
    void shouldHandleNullAudioFeaturesAndEmptyLists() {
        var metadata = new EnrichedTrackMetadata(
                "id", "Solo", "Song", null, null, null,
                null, null, null, null, null, null, null, null);
        repository.put("Solo", "Song", metadata);

        var found = repository.get("Solo", "Song");
        assertThat(found).isPresent();
        assertThat(found.get().genres()).isEmpty();
        assertThat(found.get().audioFeatures()).isNull();
    }
}
