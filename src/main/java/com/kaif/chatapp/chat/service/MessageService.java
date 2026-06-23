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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private static final Duration MESSAGE_EDIT_WINDOW = Duration.ofSeconds(30);

    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;
    private final NotificationService notificationService;
    private final RedisMessagePublisher redisMessagePublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageAttachmentService attachmentService;

    @Transactional
    public MessageDTO sendMessageWithAttachment(Long conversationId, User sender, MultipartFile file, String content) throws IOException {
        chatService.validateParticipant(conversationId, sender.getId());
        var stored = attachmentService.store(file, sender.getId());
        MessageType type = stored.image() ? MessageType.IMAGE : MessageType.FILE;

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(sender.getId())
                .content(content != null && !content.isBlank() ? content : stored.originalName())
                .type(type)
                .status(MessageStatus.SENT)
                .attachmentUrl(stored.url())
                .attachmentName(stored.originalName())
                .build();

        message = messageRepository.save(message);
        MessageDTO dto = toDTO(message);
        redisMessagePublisher.publish(conversationId, dto);
        notifyRecipients(conversationId, sender, dto.getContent());
        return dto;
    }

    private void notifyRecipients(Long conversationId, User sender, String contentPreview) {
        List<Long> recipientIds = participantRepository.findByConversationId(conversationId).stream()
                .map(ConversationParticipant::getUserId)
                .filter(id -> !id.equals(sender.getId()))
                .collect(Collectors.toList());

        notificationService.notifyUsers(recipientIds, NotificationType.NEW_MESSAGE,
                sender.getDisplayUsername() + " sent a message",
                contentPreview.length() > 100 ? contentPreview.substring(0, 100) + "..." : contentPreview,
                conversationId);
    }

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

        notifyRecipients(conversationId, sender, request.getContent());

        log.debug("Message {} sent by user {} in conversation {}", message.getId(), sender.getId(), conversationId);
        return dto;
    }

    public List<MessageDTO> searchMessages(Long conversationId, User currentUser, String query) {
        chatService.validateParticipant(conversationId, currentUser.getId());
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        return messageRepository.searchInConversation(conversationId, query.trim()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
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

        if (message.getCreatedAt() == null
                || message.getCreatedAt().plus(MESSAGE_EDIT_WINDOW).isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Messages can only be edited within 30 seconds of sending");
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

            ReadReceiptDTO receipt = ReadReceiptDTO.builder()
                    .conversationId(conversationId)
                    .messageId(messageId)
                    .readerId(currentUser.getId())
                    .status(MessageStatus.READ.name())
                    .build();
            messagingTemplate.convertAndSend("/topic/conversation." + conversationId + ".read", receipt);
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
                .attachmentUrl(message.getAttachmentUrl())
                .attachmentName(message.getAttachmentName())
                .build();
    }
}
