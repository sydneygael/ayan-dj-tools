package com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence;

import com.djtools.ayan.musictagger.domain.model.*;
import com.djtools.ayan.musictagger.infrastructure.config.RedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {RedisPlanRepository.class, RedisConfig.class})
@Testcontainers
class RedisPlanRepositoryIT {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired
    RedisPlanRepository repository;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void cleanRedis() {
        var keys = redisTemplate.keys("plan:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void saveAndFindById() {
        var op = new TagOperation("/a.mp3", Map.of("artist", "Old"), Map.of("artist", "New", "genre", "Techno"),
                OperationStatus.APPROVED, null);
        var plan = new TaggingPlan("plan-1", List.of(op), LocalDateTime.now(),
                PlanStatus.APPROVED, 1, 1, OperatingMode.PLAN, 0);

        repository.save(plan);

        Optional<TaggingPlan> found = repository.findById("plan-1");
        assertThat(found).isPresent();
        assertThat(found.get().planId()).isEqualTo("plan-1");
        assertThat(found.get().status()).isEqualTo(PlanStatus.APPROVED);
        assertThat(found.get().operations()).hasSize(1);
        assertThat(found.get().operations().getFirst().filepath()).isEqualTo("/a.mp3");
    }

    @Test
    void findById_unknownReturnsEmpty() {
        assertThat(repository.findById("nope")).isEmpty();
    }

    @Test
    void delete_removesPlan() {
        var plan = new TaggingPlan("plan-2", List.of(), LocalDateTime.now(), PlanStatus.DRAFT, 0, 0);
        repository.save(plan);
        assertThat(repository.findById("plan-2")).isPresent();

        repository.delete("plan-2");

        assertThat(repository.findById("plan-2")).isEmpty();
    }
}
