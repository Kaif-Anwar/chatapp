package com.kaif.chatapp.notification.service;

import com.kaif.chatapp.exception.ResourceNotFoundException;
import com.kaif.chatapp.notification.dto.NotificationDTO;
import com.kaif.chatapp.notification.model.Notification;
import com.kaif.chatapp.notification.model.NotificationType;
import com.kaif.chatapp.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Creates a notification and pushes it to the recipient via WebSocket.
     */
    public void createAndPush(Long recipientUserId, NotificationType type,
                              String title, String body, Long refId) {
        Notification notification = Notification.builder()
                .userId(recipientUserId)
                .type(type)
                .title(title)
                .body(body)
                .refId(refId)
                .build();

        notification = notificationRepository.save(notification);
        NotificationDTO dto = toDTO(notification);

        // Push via WebSocket to the specific user
        messagingTemplate.convertAndSendToUser(
                recipientUserId.toString(),
                "/queue/notifications",
                dto);

        log.debug("Notification pushed to user {}: {}", recipientUserId, title);
    }

    /**
     * Sends notifications to multiple recipients (e.g., all participants in a conversation except the sender).
     */
    public void notifyUsers(List<Long> recipientUserIds, NotificationType type,
                            String title, String body, Long refId) {
        for (Long userId : recipientUserIds) {
            createAndPush(userId, type, title, body, refId);
        }
    }

    public Page<NotificationDTO> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDTO);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    private NotificationDTO toDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .refId(notification.getRefId())
                .createdAt(notification.getCreatedAt())
                .isRead(notification.isRead())
                .build();
    }
}
