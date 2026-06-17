package com.kaif.chatapp.chat.service;

import com.kaif.chatapp.chat.dto.MessageDTO;
import com.kaif.chatapp.chat.model.ConversationParticipant;
import com.kaif.chatapp.chat.repository.ConversationParticipantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Subscribes to Redis pub/sub channels and broadcasts messages
 * to WebSocket clients via STOMP.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationParticipantRepository participantRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody());
            MessageDTO messageDTO = objectMapper.readValue(payload, MessageDTO.class);

            if (messageDTO.getId() == null || messageDTO.getConversationId() == null) {
                log.warn("Ignoring Redis message with missing id or conversationId");
                return;
            }

            // Broadcast to the conversation topic
            String destination = "/topic/conversation." + messageDTO.getConversationId();
            messagingTemplate.convertAndSend(destination, messageDTO);

            // Also send to individual user queues for private delivery
            List<ConversationParticipant> participants = participantRepository
                    .findByConversationId(messageDTO.getConversationId());

            for (ConversationParticipant participant : participants) {
                if (!participant.getUserId().equals(messageDTO.getSenderId())) {
                    messagingTemplate.convertAndSendToUser(
                            participant.getUserId().toString(),
                            "/queue/messages",
                            messageDTO);
                }
            }

            log.debug("Message broadcast from Redis to WebSocket: conversation={}", messageDTO.getConversationId());
        } catch (Exception e) {
            log.error("Error processing Redis message", e);
        }
    }
}
