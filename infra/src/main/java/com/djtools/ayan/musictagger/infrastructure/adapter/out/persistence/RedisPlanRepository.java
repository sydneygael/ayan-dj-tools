package com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence;

import com.djtools.ayan.musictagger.domain.model.TaggingPlan;
import com.djtools.ayan.musictagger.domain.port.out.PlanRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;

@Repository
public class RedisPlanRepository implements PlanRepository {

    private static final String KEY_PREFIX = "plan:";
    private static final Duration TTL = Duration.ofHours(48);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisPlanRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = JsonMapper.builder().findAndAddModules().build();
    }

    @Override
    public void save(TaggingPlan plan) {
        String key = KEY_PREFIX + plan.planId();
        redisTemplate.opsForValue().set(key, plan, TTL);
    }

    @Override
    public Optional<TaggingPlan> findById(String planId) {
        Object raw = redisTemplate.opsForValue().get(KEY_PREFIX + planId);
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.convertValue(raw, TaggingPlan.class));
    }

    @Override
    public void delete(String planId) {
        redisTemplate.delete(KEY_PREFIX + planId);
    }
}
