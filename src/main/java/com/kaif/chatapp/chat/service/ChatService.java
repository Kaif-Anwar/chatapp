package com.kaif.chatapp.chat.service;

import com.kaif.chatapp.chat.dto.*;
import com.kaif.chatapp.chat.model.*;
import com.kaif.chatapp.chat.repository.*;

import com.kaif.chatapp.exception.*;
import com.kaif.chatapp.notification.model.NotificationType;
import com.kaif.chatapp.notification.service.NotificationService;
import com.kaif.chatapp.user.dto.UserDTO;
import com.kaif.chatapp.user.model.User;
import com.kaif.chatapp.user.repository.UserRepository;
import com.kaif.chatapp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final RedisSubscriptionManager redisSubscriptionManager;

    @Transactional
    public ConversationDTO createConversation(User currentUser, CreateConversationRequest request) {
        if (request.getType() == ConversationType.PRIVATE) {
            return createPrivateConversation(currentUser, request);
        }
        return createGroupConversation(currentUser, request);
    }

    private ConversationDTO createPrivateConversation(User currentUser, CreateConversationRequest request) {
        if (request.getParticipantIds().size() != 1) {
            throw new BadRequestException("Private conversation must have exactly one other participant");
        }

        Long otherUserId = request.getParticipantIds().get(0);
        if (otherUserId.equals(currentUser.getId())) {
            throw new BadRequestException("Cannot create a conversation with yourself");
        }

        // Check if private conversation already exists
        List<Conversation> existing = conversationRepository.findPrivateConversation(
                currentUser.getId(), otherUserId);
        if (!existing.isEmpty()) {
            return toDTO(existing.get(0), currentUser.getId());
        }

        // Verify the other user exists
        userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + otherUserId));

        Conversation conversation = Conversation.builder()
                .type(ConversationType.PRIVATE)
                .createdBy(currentUser.getId())
                .build();
        conversation = conversationRepository.save(conversation);

        // Subscribe new conversation channel to Redis pub/sub for real-time delivery
        redisSubscriptionManager.subscribeToConversation(conversation.getId());

        // Add both participants
        addParticipantInternal(conversation.getId(), currentUser.getId(), false);
        addParticipantInternal(conversation.getId(), otherUserId, false);

        log.info("Private conversation {} created between {} and {}", conversation.getId(), currentUser.getId(), otherUserId);
        return toDTO(conversation, currentUser.getId());
    }

    private ConversationDTO createGroupConversation(User currentUser, CreateConversationRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Group conversation requires a name");
        }

        Conversation conversation = Conversation.builder()
                .name(request.getName())
                .type(ConversationType.GROUP)
                .createdBy(currentUser.getId())
                .build();
        conversation = conversationRepository.save(conversation);

        // Subscribe new conversation channel to Redis pub/sub for real-time delivery
        redisSubscriptionManager.subscribeToConversation(conversation.getId());

        // Add creator as admin
        addParticipantInternal(conversation.getId(), currentUser.getId(), true);

        // Add other participants
        for (Long userId : request.getParticipantIds()) {
            if (!userId.equals(currentUser.getId())) {
                userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                addParticipantInternal(conversation.getId(), userId, false);

                // Send group invite notification
                notificationService.createAndPush(userId, NotificationType.GROUP_INVITE,
                        currentUser.getDisplayUsername() + " added you to " + request.getName(),
                        "You were added to the group \"" + request.getName() + "\"",
                        conversation.getId());
            }
        }

        log.info("Group conversation '{}' (id={}) created by {}", request.getName(), conversation.getId(), currentUser.getDisplayUsername());
        return toDTO(conversation, currentUser.getId());
    }

    public List<ConversationDTO> getUserConversations(User currentUser) {
        return conversationRepository.findAllByParticipantUserId(currentUser.getId()).stream()
                .map(conv -> toDTO(conv, currentUser.getId()))
                .collect(Collectors.toList());
    }

    public ConversationDTO getConversation(Long conversationId, User currentUser) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        validateParticipant(conversationId, currentUser.getId());
        return toDTO(conversation, currentUser.getId());
    }

    @Transactional
    public ConversationDTO addParticipants(Long conversationId, User currentUser, AddParticipantsRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (conversation.getType() == ConversationType.PRIVATE) {
            throw new BadRequestException("Cannot add participants to a private conversation");
        }

        // Only admin can add participants
        ConversationParticipant currentParticipant = participantRepository
                .findByConversationIdAndUserId(conversationId, currentUser.getId())
                .orElseThrow(() -> new ForbiddenException("You are not a participant of this conversation"));

        if (!currentParticipant.isAdmin()) {
            throw new ForbiddenException("Only group admin can add participants");
        }

        for (Long userId : request.getUserIds()) {
            if (!participantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
                userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                addParticipantInternal(conversationId, userId, false);

                notificationService.createAndPush(userId, NotificationType.GROUP_INVITE,
                        currentUser.getDisplayUsername() + " added you to " + conversation.getName(),
                        "You were added to the group \"" + conversation.getName() + "\"",
                        conversationId);
            }
        }

        return toDTO(conversation, currentUser.getId());
    }

    @Transactional
    public void removeParticipant(Long conversationId, Long userIdToRemove, User currentUser) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (conversation.getType() == ConversationType.PRIVATE) {
            throw new BadRequestException("Cannot remove participants from a private conversation");
        }

        ConversationParticipant currentParticipant = participantRepository
                .findByConversationIdAndUserId(conversationId, currentUser.getId())
                .orElseThrow(() -> new ForbiddenException("You are not a participant of this conversation"));

        if (!currentParticipant.isAdmin() && !currentUser.getId().equals(userIdToRemove)) {
            throw new ForbiddenException("Only group admin can remove other participants");
        }

        participantRepository.deleteByConversationIdAndUserId(conversationId, userIdToRemove);
        log.info("User {} removed from conversation {}", userIdToRemove, conversationId);
    }

    @Transactional
    public void leaveConversation(Long conversationId, User currentUser) {
        validateParticipant(conversationId, currentUser.getId());
        participantRepository.deleteByConversationIdAndUserId(conversationId, currentUser.getId());
        log.info("User {} left conversation {}", currentUser.getId(), conversationId);
    }

    // --- Helper methods ---

    public void validateParticipant(Long conversationId, Long userId) {
        if (!participantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ForbiddenException("You are not a participant of this conversation");
        }
    }

    private void addParticipantInternal(Long conversationId, Long userId, boolean isAdmin) {
        ConversationParticipant participant = ConversationParticipant.builder()
                .conversationId(conversationId)
                .userId(userId)
                .isAdmin(isAdmin)
                .build();
        participantRepository.save(participant);
    }

    public ConversationDTO toDTO(Conversation conversation, Long currentUserId) {
        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversation.getId());

        List<UserDTO> participantDTOs = participants.stream()
                .map(p -> userRepository.findById(p.getUserId()).map(u -> {
                    UserDTO dto = userService.toDTO(u);
                    dto.setAdmin(p.isAdmin());
                    return dto;
                }).orElse(null))
                .filter(u -> u != null)
                .collect(Collectors.toList());

        // Get last message
        MessageDTO lastMessage = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversation.getId())
                .map(this::toMessageDTO)
                .orElse(null);

        // Calculate unread count
        long unreadCount = 0;
        ConversationParticipant currentParticipant = participants.stream()
                .filter(p -> p.getUserId().equals(currentUserId))
                .findFirst()
                .orElse(null);

        if (currentParticipant != null) {
            if (currentParticipant.getLastReadAt() != null) {
                unreadCount = messageRepository.countUnreadMessages(
                        conversation.getId(), currentUserId, currentParticipant.getLastReadAt());
            } else {
                unreadCount = messageRepository.countAllUnreadMessages(conversation.getId(), currentUserId);
            }
        }

        return ConversationDTO.builder()
                .id(conversation.getId())
                .name(conversation.getName())
                .type(conversation.getType())
                .createdBy(conversation.getCreatedBy())
                .createdAt(conversation.getCreatedAt())
                .participants(participantDTOs)
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .build();
    }

    private MessageDTO toMessageDTO(Message message) {
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
