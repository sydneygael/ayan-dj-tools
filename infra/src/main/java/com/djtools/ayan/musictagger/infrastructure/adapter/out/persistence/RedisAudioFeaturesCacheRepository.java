package com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.port.out.AudioFeaturesCacheRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Repository
public class RedisAudioFeaturesCacheRepository implements AudioFeaturesCacheRepository {

    private static final String KEY_PREFIX = "audio-features:";
    private static final Duration TTL = Duration.ofDays(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAudioFeaturesCacheRepository(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(String filepath, AudioFeatures features) {
        redisTemplate.opsForValue().set(KEY_PREFIX + filepath, features, TTL);
    }

    @Override
    public Optional<AudioFeatures> findByFilepath(String filepath) {
        final var raw = redisTemplate.opsForValue().get(KEY_PREFIX + filepath);
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.convertValue(raw, AudioFeatures.class));
    }

    @Override
    public List<AudioFeatures> findAll() {
        var keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null) {
            return List.of();
        }
        return keys.stream()
                .map(key -> redisTemplate.opsForValue().get(key))
                .filter(raw -> raw != null)
                .map(raw -> objectMapper.convertValue(raw, AudioFeatures.class))
                .toList();
    }
}
