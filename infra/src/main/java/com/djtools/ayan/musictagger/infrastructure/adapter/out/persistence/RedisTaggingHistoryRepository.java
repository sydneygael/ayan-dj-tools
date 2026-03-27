package com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence;

import com.djtools.ayan.musictagger.domain.model.TaggingHistoryEntry;
import com.djtools.ayan.musictagger.domain.port.out.TaggingHistoryRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Repository
public class RedisTaggingHistoryRepository implements TaggingHistoryRepository {

    private static final String KEY_PREFIX = "tagging-history:";
    private static final Duration TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisTaggingHistoryRepository(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(TaggingHistoryEntry entry) {
        final var key = KEY_PREFIX + entry.planId();
        redisTemplate.opsForList().rightPush(key, entry);
        redisTemplate.expire(key, TTL);
    }

    @Override
    public List<TaggingHistoryEntry> findByPlanId(String planId) {
        final var raw = redisTemplate.opsForList().range(KEY_PREFIX + planId, 0, -1);
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .map(obj -> objectMapper.convertValue(obj, TaggingHistoryEntry.class))
                .toList();
    }

    @Override
    public List<TaggingHistoryEntry> findByFilepath(String filepath) {
        return findAll().stream()
                .filter(entry -> filepath.equals(entry.filepath()))
                .toList();
    }

    @Override
    public List<TaggingHistoryEntry> findAll() {
        var keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null) {
            return List.of();
        }
        return keys.stream()
                .flatMap(key -> {
                    List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
                    if (raw == null) return java.util.stream.Stream.empty();
                    return raw.stream();
                })
                .map(obj -> objectMapper.convertValue(obj, TaggingHistoryEntry.class))
                .toList();
    }
}
