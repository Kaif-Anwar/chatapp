package com.kaif.chatapp.chat.service;

import com.kaif.chatapp.chat.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * Manages dynamic Redis channel subscriptions.
 * Subscribes to conversation channels as they are created.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriptionManager {

    private final RedisMessageListenerContainer listenerContainer;
    private final RedisMessageSubscriber subscriber;
    private final ConversationRepository conversationRepository;

    @EventListener(ContextRefreshedEvent.class)
    public void subscribeToExistingConversations() {
        conversationRepository.findAll().forEach(conversation -> {
            subscribeToConversation(conversation.getId());
        });
        log.info("Subscribed to Redis channels for all existing conversations");
    }

    public void subscribeToConversation(Long conversationId) {
        String channel = "chat:conversation:" + conversationId;
        listenerContainer.addMessageListener(subscriber, new ChannelTopic(channel));
        log.debug("Subscribed to Redis channel: {}", channel);
    }
}
