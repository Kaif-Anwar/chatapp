package com.kaif.chatapp.chat.controller;

import com.kaif.chatapp.chat.dto.MessageDTO;
import com.kaif.chatapp.chat.dto.SendMessageRequest;
import com.kaif.chatapp.chat.model.MessageType;
import com.kaif.chatapp.chat.service.MessageService;
import com.kaif.chatapp.chat.service.TypingService;
import com.kaif.chatapp.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket STOMP message controller.
 * Handles real-time message sending and read receipts via STOMP protocol.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;
    private final TypingService typingService;

    /**
     * Handles messages sent via STOMP to /app/chat.send
     * Payload: { "conversationId": 5, "content": "Hello!", "type": "TEXT" }
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) {
            log.warn("Unauthenticated WebSocket message attempt");
            return;
        }

        User user = (User) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
        Long conversationId = Long.valueOf(payload.get("conversationId").toString());
        String content = (String) payload.get("content");
        String typeStr = payload.getOrDefault("type", "TEXT").toString();

        SendMessageRequest request = new SendMessageRequest();
        request.setContent(content);
        request.setType(MessageType.valueOf(typeStr));

        MessageDTO result = messageService.sendMessage(conversationId, user, request);
        log.debug("WebSocket message sent: id={}, conversation={}", result.getId(), conversationId);
    }

    /**
     * Handles read receipts sent via STOMP to /app/chat.read
     * Payload: { "conversationId": 5, "messageId": 101 }
     */
    @MessageMapping("/chat.read")
    public void markAsRead(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) return;

        User user = (User) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
        Long conversationId = Long.valueOf(payload.get("conversationId").toString());
        Long messageId = Long.valueOf(payload.get("messageId").toString());

        messageService.markMessageAsRead(conversationId, messageId, user);
        log.debug("Message {} marked as read by user {} in conversation {}", messageId, user.getId(), conversationId);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) return;

        User user = (User) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
        Long conversationId = Long.valueOf(payload.get("conversationId").toString());
        boolean typing = Boolean.parseBoolean(payload.getOrDefault("typing", true).toString());
        typingService.setTyping(conversationId, user.getId(), user.getDisplayUsername(), typing);
    }
}
