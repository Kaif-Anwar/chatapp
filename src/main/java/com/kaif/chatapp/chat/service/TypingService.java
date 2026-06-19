package com.kaif.chatapp.chat.service;

import com.kaif.chatapp.chat.dto.TypingEventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TypingService {

    private static final Duration TYPING_TTL = Duration.ofSeconds(5);

    private final StringRedisTemplate redis;
    private final SimpMessagingTemplate messagingTemplate;

    public void setTyping(Long conversationId, Long userId, String username, boolean typing) {
        String key = "typing:" + conversationId + ":" + userId;
        if (typing) {
            redis.opsForValue().set(key, username, TYPING_TTL);
        } else {
            redis.delete(key);
        }
        broadcastTyping(conversationId, userId);
    }

    public Set<String> getTypingUsers(Long conversationId, Long excludeUserId) {
        Set<String> keys = redis.keys("typing:" + conversationId + ":*");
        if (keys == null || keys.isEmpty()) return Set.of();
        return keys.stream()
                .filter(k -> !k.endsWith(":" + excludeUserId))
                .map(k -> redis.opsForValue().get(k))
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.toSet());
    }

    private void broadcastTyping(Long conversationId, Long senderId) {
        TypingEventDTO event = TypingEventDTO.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .typingUsers(getTypingUsers(conversationId, null))
                .build();
        messagingTemplate.convertAndSend("/topic/conversation." + conversationId + ".typing", event);
    }
}
