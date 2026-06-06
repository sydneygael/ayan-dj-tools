package com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.TrackThemes;
import com.djtools.ayan.musictagger.domain.port.out.EnrichedMetadataCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Persistance durable des métadonnées enrichies dans Postgres — survit aux redémarrages, pas d'expiration. */
@Repository
public class PostgresEnrichedMetadataCacheRepository implements EnrichedMetadataCacheRepository {

    private static final Logger log = LoggerFactory.getLogger(PostgresEnrichedMetadataCacheRepository.class);

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public PostgresEnrichedMetadataCacheRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<EnrichedTrackMetadata> get(String artist, String title) {
        return jdbcClient.sql("""
                SELECT * FROM enriched_track_metadata
                WHERE artist_key = :artistKey AND title_key = :titleKey
                """)
                .param("artistKey", normalize(artist))
                .param("titleKey", normalize(title))
                .query((rs, rowNum) -> mapRow(rs))
                .optional();
    }

    @Override
    public void put(String artist, String title, EnrichedTrackMetadata metadata) {
        jdbcClient.sql("""
                INSERT INTO enriched_track_metadata
                    (artist_key, title_key, source_id, artist, title, album, genres, styles,
                     label, country, isrc, tags, release_year, popularity, duration_ms, audio_features,
                     language_code, explicit, themes, fetched_at)
                VALUES
                    (:artistKey, :titleKey, :sourceId, :artist, :title, :album, :genres, :styles,
                     :label, :country, :isrc, :tags, :releaseYear, :popularity, :durationMs, :audioFeatures,
                     :languageCode, :explicit, :themes, CURRENT_TIMESTAMP)
                ON CONFLICT (artist_key, title_key) DO UPDATE SET
                    source_id      = EXCLUDED.source_id,
                    artist         = EXCLUDED.artist,
                    title          = EXCLUDED.title,
                    album          = EXCLUDED.album,
                    genres         = EXCLUDED.genres,
                    styles         = EXCLUDED.styles,
                    label          = EXCLUDED.label,
                    country        = EXCLUDED.country,
                    isrc           = EXCLUDED.isrc,
                    tags           = EXCLUDED.tags,
                    release_year   = EXCLUDED.release_year,
                    popularity     = EXCLUDED.popularity,
                    duration_ms    = EXCLUDED.duration_ms,
                    audio_features = EXCLUDED.audio_features,
                    language_code  = EXCLUDED.language_code,
                    explicit       = EXCLUDED.explicit,
                    themes         = EXCLUDED.themes,
                    fetched_at     = CURRENT_TIMESTAMP
                """)
                .param("artistKey", normalize(artist))
                .param("titleKey", normalize(title))
                .param("sourceId", metadata.sourceId())
                .param("artist", metadata.artist())
                .param("title", metadata.title())
                .param("album", metadata.album())
                .param("genres", toJson(metadata.genres()))
                .param("styles", toJson(metadata.styles()))
                .param("label", metadata.label())
                .param("country", metadata.country())
                .param("isrc", metadata.isrc())
                .param("tags", toJson(metadata.tags()))
                .param("releaseYear", metadata.releaseYear())
                .param("popularity", metadata.popularity())
                .param("durationMs", metadata.durationMs())
                .param("audioFeatures", toJson(metadata.audioFeatures()))
                .param("languageCode", metadata.languageCode())
                .param("explicit", metadata.explicit())
                .param("themes", toJson(metadata.themes()))
                .update();
        log.debug("Persisted enriched metadata for '{} – {}'", artist, title);
    }

    private EnrichedTrackMetadata mapRow(ResultSet rs) throws SQLException {
        return new EnrichedTrackMetadata(
                rs.getString("source_id"),
                rs.getString("artist"),
                rs.getString("title"),
                rs.getString("album"),
                fromJsonList(rs.getString("genres")),
                fromJsonList(rs.getString("styles")),
                rs.getString("label"),
                rs.getString("country"),
                rs.getString("isrc"),
                fromJsonList(rs.getString("tags")),
                (Integer) rs.getObject("release_year"),
                (Integer) rs.getObject("popularity"),
                (Long) rs.getObject("duration_ms"),
                fromJsonAudioFeatures(rs.getString("audio_features")),
                rs.getString("language_code"),
                rs.getObject("explicit", Boolean.class),
                fromJsonThemes(rs.getString("themes"))
        );
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize {}: {}", value.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return List.of(objectMapper.readValue(json, String[].class));
        } catch (Exception e) {
            log.warn("Failed to deserialize list from '{}': {}", json, e.getMessage());
            return List.of();
        }
    }

    private TrackThemes fromJsonThemes(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, TrackThemes.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize themes from '{}': {}", json, e.getMessage());
            return null;
        }
    }

    private AudioFeatures fromJsonAudioFeatures(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, AudioFeatures.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize audio features from '{}': {}", json, e.getMessage());
            return null;
        }
    }

    private static String normalize(String value) {
        return value != null ? value.trim().toLowerCase(Locale.ROOT) : "";
    }
}
