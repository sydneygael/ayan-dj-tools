package com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

/**
 * Implémentation Redis de ChatMemoryStore (LangChain4j).
 * Stocke les messages USER et AI non-vides. Ignore les autres types (tool results éphémères).
 */
@Component
public class RedisChatMemoryRepository implements ChatMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemoryRepository.class);
    private static final String KEY_PREFIX = "chat-memory:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryRepository(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        final var key = KEY_PREFIX + memoryId;
        final var raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) return List.of();
        return raw.stream()
                .map(obj -> objectMapper.convertValue(obj, StoredMessage.class))
                .map(this::toMessage)
                .toList();
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        final var key = KEY_PREFIX + memoryId;
        redisTemplate.delete(key);
        final var storable = messages.stream().filter(this::isStorable).toList();
        if (storable.isEmpty()) return;
        storable.forEach(m -> redisTemplate.opsForList().rightPush(key, toStored(m)));
        redisTemplate.expire(key, TTL);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(KEY_PREFIX + memoryId);
    }

    private boolean isStorable(ChatMessage message) {
        if (message.type() == ChatMessageType.USER) return true;
        if (message.type() == ChatMessageType.AI) {
            final var text = ((AiMessage) message).text();
            return text != null && !text.isBlank();
        }
        return false;
    }

    private StoredMessage toStored(ChatMessage message) {
        final var role = message.type() == ChatMessageType.USER ? "user" : "assistant";
        final var text = message.type() == ChatMessageType.USER
                ? ((UserMessage) message).singleText()
                : ((AiMessage) message).text();
        return new StoredMessage(role, text);
    }

    private ChatMessage toMessage(StoredMessage stored) {
        return "user".equals(stored.role())
                ? UserMessage.from(stored.content())
                : AiMessage.from(stored.content());
    }

    public record StoredMessage(String role, String content) {}
}
