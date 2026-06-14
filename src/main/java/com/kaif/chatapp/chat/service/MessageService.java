package com.kaif.chatapp.chat.service;

import com.kaif.chatapp.chat.dto.*;
import com.kaif.chatapp.chat.model.*;
import com.kaif.chatapp.chat.repository.*;
import com.kaif.chatapp.exception.*;
import com.kaif.chatapp.notification.model.NotificationType;
import com.kaif.chatapp.notification.service.NotificationService;
import com.kaif.chatapp.user.model.User;
import com.kaif.chatapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;
    private final NotificationService notificationService;
    private final RedisMessagePublisher redisMessagePublisher;

    @Transactional
    public MessageDTO sendMessage(Long conversationId, User sender, SendMessageRequest request) {
        // Validate sender is a participant
        chatService.validateParticipant(conversationId, sender.getId());

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(sender.getId())
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : MessageType.TEXT)
                .status(MessageStatus.SENT)
                .build();

        message = messageRepository.save(message);
        MessageDTO dto = toDTO(message);

        // Publish to Redis for real-time delivery
        redisMessagePublisher.publish(conversationId, dto);

        // Send notifications to other participants
        List<Long> recipientIds = participantRepository.findByConversationId(conversationId).stream()
                .map(ConversationParticipant::getUserId)
                .filter(id -> !id.equals(sender.getId()))
                .collect(Collectors.toList());

        notificationService.notifyUsers(recipientIds, NotificationType.NEW_MESSAGE,
                sender.getDisplayUsername() + " sent a message",
                request.getContent().length() > 100 ? request.getContent().substring(0, 100) + "..." : request.getContent(),
                conversationId);

        log.debug("Message {} sent by user {} in conversation {}", message.getId(), sender.getId(), conversationId);
        return dto;
    }

    public Page<MessageDTO> getMessageHistory(Long conversationId, User currentUser, Pageable pageable) {
        chatService.validateParticipant(conversationId, currentUser.getId());
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(this::toDTO);
    }

    @Transactional
    public MessageDTO editMessage(Long conversationId, Long messageId, User currentUser, EditMessageRequest request) {
        chatService.validateParticipant(conversationId, currentUser.getId());

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getConversationId().equals(conversationId)) {
            throw new ResourceNotFoundException("Message not found in this conversation");
        }

        if (!message.getSenderId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only edit your own messages");
        }

        if (message.isDeleted()) {
            throw new BadRequestException("Cannot edit a deleted message");
        }

        message.setContent(request.getContent());
        message.setEditedAt(LocalDateTime.now());
        message = messageRepository.save(message);

        log.debug("Message {} edited by user {}", messageId, currentUser.getId());
        return toDTO(message);
    }

    @Transactional
    public void deleteMessage(Long conversationId, Long messageId, User currentUser) {
        chatService.validateParticipant(conversationId, currentUser.getId());

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getConversationId().equals(conversationId)) {
            throw new ResourceNotFoundException("Message not found in this conversation");
        }

        if (!message.getSenderId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only delete your own messages");
        }

        message.setDeleted(true);
        messageRepository.save(message);

        log.debug("Message {} soft-deleted by user {}", messageId, currentUser.getId());
    }

    @Transactional
    public void markMessageAsRead(Long conversationId, Long messageId, User currentUser) {
        chatService.validateParticipant(conversationId, currentUser.getId());

        // Update the participant's last_read_at
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, currentUser.getId())
                .orElseThrow(() -> new ForbiddenException("You are not a participant"));

        participant.setLastReadAt(LocalDateTime.now());
        participantRepository.save(participant);

        // Update message status if needed
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (message.getStatus() != MessageStatus.READ && !message.getSenderId().equals(currentUser.getId())) {
            message.setStatus(MessageStatus.READ);
            messageRepository.save(message);
        }
    }

    public long getUnreadCount(Long conversationId, User currentUser) {
        chatService.validateParticipant(conversationId, currentUser.getId());

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, currentUser.getId())
                .orElseThrow(() -> new ForbiddenException("You are not a participant"));

        if (participant.getLastReadAt() != null) {
            return messageRepository.countUnreadMessages(conversationId, currentUser.getId(), participant.getLastReadAt());
        }
        return messageRepository.countAllUnreadMessages(conversationId, currentUser.getId());
    }

    private MessageDTO toDTO(Message message) {
        String senderUsername = userRepository.findById(message.getSenderId())
                .map(User::getDisplayUsername).orElse("Unknown");

        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderUsername(senderUsername)
                .content(message.isDeleted() ? "This message was deleted" : message.getContent())
                .type(message.getType())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .editedAt(message.getEditedAt())
                .deleted(message.isDeleted())
                .build();
    }
}
