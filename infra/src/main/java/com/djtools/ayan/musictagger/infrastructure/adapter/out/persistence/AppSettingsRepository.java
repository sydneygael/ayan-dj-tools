package com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class AppSettingsRepository {

    private final JdbcClient jdbcClient;

    public AppSettingsRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<String> get(String key) {
        return jdbcClient.sql("SELECT value FROM app_settings WHERE key = :key")
                .param("key", key)
                .query((rs, _) -> rs.getString("value"))
                .optional();
    }

    public void put(String key, String value) {
        jdbcClient.sql("""
                INSERT INTO app_settings (key, value) VALUES (:key, :value)
                ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value
                """)
                .param("key", key)
                .param("value", value)
                .update();
    }

    public Map<String, String> getAll() {
        return jdbcClient.sql("SELECT key, value FROM app_settings")
                .query((rs, _) -> Map.entry(rs.getString("key"), rs.getString("value")))
                .list()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
