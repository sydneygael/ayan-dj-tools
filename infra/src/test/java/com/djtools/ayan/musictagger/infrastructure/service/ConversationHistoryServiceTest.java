package com.djtools.ayan.musictagger.infrastructure.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationHistoryServiceTest {

    @Mock
    RedisTemplate<String, Object> redisTemplate;
    @Mock
    ListOperations<String, Object> listOperations;

    ConversationHistoryService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        service = new ConversationHistoryService(
                redisTemplate,
                JsonMapper.builder().findAndAddModules().build()
        );
    }

    @Test
    void saveMessage_pushesToRedisAndSetsTtl() {
        var message = new ChatMessage("user", "Salut", LocalDateTime.now());

        service.saveMessage("conv-1", message);

        verify(listOperations).rightPush("conversation:conv-1", message);
        verify(redisTemplate).expire("conversation:conv-1", Duration.ofHours(24));
    }

    @Test
    void getHistory_returnsMessages() {
        var map = new LinkedHashMap<String, Object>();
        map.put("role", "user");
        map.put("content", "Bonjour");
        map.put("timestamp", List.of(2026, 2, 26, 10, 0, 0));

        when(listOperations.range("conversation:conv-1", 0, -1))
                .thenReturn(List.of(map));

        List<ChatMessage> history = service.getHistory("conv-1");

        assertThat(history).hasSize(1);
        assertThat(history.getFirst().role()).isEqualTo("user");
        assertThat(history.getFirst().content()).isEqualTo("Bonjour");
    }

    @Test
    void getHistory_returnsEmptyListWhenNull() {
        when(listOperations.range("conversation:conv-1", 0, -1)).thenReturn(null);

        List<ChatMessage> history = service.getHistory("conv-1");

        assertThat(history).isEmpty();
    }

    @Test
    void clearHistory_deletesKey() {
        service.clearHistory("conv-1");

        verify(redisTemplate).delete("conversation:conv-1");
    }

    @Test
    void getMessageCount_returnsSize() {
        when(listOperations.size("conversation:conv-1")).thenReturn(5L);

        long count = service.getMessageCount("conv-1");

        assertThat(count).isEqualTo(5);
    }

    @Test
    void getMessageCount_returnsZeroWhenNull() {
        when(listOperations.size("conversation:conv-1")).thenReturn(null);

        long count = service.getMessageCount("conv-1");

        assertThat(count).isZero();
    }
}
