package com.kaif.chatapp.chat.service;

import com.kaif.chatapp.chat.dto.MessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes chat messages to Redis pub/sub channels.
 * Channel naming: chat:conversation:{conversationId}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMessagePublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(Long conversationId, MessageDTO messageDTO) {
        try {
            String channel = "chat:conversation:" + conversationId;
            String payload = objectMapper.writeValueAsString(messageDTO);
            stringRedisTemplate.convertAndSend(channel, payload);
            log.debug("Published message to Redis channel: {}", channel);
        } catch (Exception e) {
            log.error("Failed to publish message to Redis", e);
        }
    }
}
