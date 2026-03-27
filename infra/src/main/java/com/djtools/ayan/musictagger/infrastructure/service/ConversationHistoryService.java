package com.djtools.ayan.musictagger.infrastructure.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Service
public class ConversationHistoryService {

    private static final String KEY_PREFIX = "conversation:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public ConversationHistoryService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void saveMessage(String conversationId, ChatMessage message) {
        final var key = KEY_PREFIX + conversationId;
        redisTemplate.opsForList().rightPush(key, message);
        redisTemplate.expire(key, TTL);
    }

    public List<ChatMessage> getHistory(String conversationId) {
        final var key = KEY_PREFIX + conversationId;
        final var raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .map(obj -> objectMapper.convertValue(obj, ChatMessage.class))
                .toList();
    }

    public void clearHistory(String conversationId) {
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }

    public long getMessageCount(String conversationId) {
        final var size = redisTemplate.opsForList().size(KEY_PREFIX + conversationId);
        return size != null ? size : 0;
    }
}
