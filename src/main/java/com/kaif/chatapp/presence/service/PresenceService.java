package com.kaif.chatapp.presence.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manages user online/offline presence using Redis keys with TTL.
 * Key: presence:user:{userId}, Value: "ONLINE", TTL: 30 seconds.
 * Heartbeat pings renew the TTL every ~20 seconds.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

    private static final String PRESENCE_KEY_PREFIX = "presence:user:";
    private static final long PRESENCE_TTL_SECONDS = 30;

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public void setOnline(Long userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, "ONLINE", PRESENCE_TTL_SECONDS, TimeUnit.SECONDS);
        broadcastPresence(userId, "ONLINE");
        log.debug("User {} is now ONLINE", userId);
    }

    public void setOffline(Long userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        redisTemplate.delete(key);
        broadcastPresence(userId, "OFFLINE");
        log.debug("User {} is now OFFLINE", userId);
    }

    public void refreshPresence(Long userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            redisTemplate.expire(key, PRESENCE_TTL_SECONDS, TimeUnit.SECONDS);
        } else {
            setOnline(userId);
        }
    }

    public boolean isOnline(Long userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private void broadcastPresence(Long userId, String status) {
        messagingTemplate.convertAndSend(
                "/topic/presence." + userId,
                Map.of("userId", userId, "status", status));
    }
}
