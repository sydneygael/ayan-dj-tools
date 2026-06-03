package com.djtools.ayan.musictagger.infrastructure.adapter.out.persistence;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Component
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String KEY_PREFIX = "chat-memory:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryRepository(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<String> findConversationIds() {
        return List.of();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        final var key = KEY_PREFIX + conversationId;
        final var raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) return List.of();
        return raw.stream()
                .map(obj -> objectMapper.convertValue(obj, StoredMessage.class))
                .map(this::toMessage)
                .toList();
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        final var key = KEY_PREFIX + conversationId;
        // Delete first — saveAll is a full replace, not an append.
        // Without this, every turn duplicates the entire history (O(N²) growth).
        redisTemplate.delete(key);
        // Only persist user and non-empty assistant messages.
        // Tool call requests (AssistantMessage with getText()=null) and ToolResponseMessage
        // are ephemeral: they serve one round-trip and must not pollute long-term memory.
        final var storable = messages.stream().filter(this::isStorable).toList();
        if (storable.isEmpty()) return;
        storable.forEach(m -> redisTemplate.opsForList().rightPush(key, toStored(m)));
        redisTemplate.expire(key, TTL);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }

    private boolean isStorable(Message message) {
        if (message instanceof UserMessage) return true;
        if (message instanceof AssistantMessage) {
            final var text = message.getText();
            return text != null && !text.isBlank();
        }
        return false;
    }

    private StoredMessage toStored(Message message) {
        return new StoredMessage(message.getMessageType().getValue(), message.getText());
    }

    private Message toMessage(StoredMessage stored) {
        return "user".equals(stored.role())
                ? new UserMessage(stored.content())
                : new AssistantMessage(stored.content());
    }

    public record StoredMessage(String role, String content) {}
}
