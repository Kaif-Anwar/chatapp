package com.kaif.chatapp.presence;

import com.kaif.chatapp.presence.service.PresenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOps;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private PresenceService presenceService;

    @Test
    @DisplayName("Set user online - stores in Redis with TTL")
    void setOnline_storesInRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        presenceService.setOnline(1L);

        verify(valueOps).set("presence:user:1", "ONLINE", 30, TimeUnit.SECONDS);
        verify(messagingTemplate).convertAndSend(eq("/topic/presence.1"), anyMap());
    }

    @Test
    @DisplayName("Set user offline - deletes Redis key")
    void setOffline_deletesKey() {
        presenceService.setOffline(1L);

        verify(redisTemplate).delete("presence:user:1");
        verify(messagingTemplate).convertAndSend(eq("/topic/presence.1"), anyMap());
    }

    @Test
    @DisplayName("Check online status - returns true when key exists")
    void isOnline_returnsTrue() {
        when(redisTemplate.hasKey("presence:user:1")).thenReturn(true);

        boolean result = presenceService.isOnline(1L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Check online status - returns false when key absent")
    void isOnline_returnsFalse() {
        when(redisTemplate.hasKey("presence:user:99")).thenReturn(false);

        boolean result = presenceService.isOnline(99L);

        assertThat(result).isFalse();
    }
}
