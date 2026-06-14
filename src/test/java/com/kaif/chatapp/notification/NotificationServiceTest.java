package com.kaif.chatapp.notification;

import com.kaif.chatapp.notification.dto.NotificationDTO;
import com.kaif.chatapp.notification.model.Notification;
import com.kaif.chatapp.notification.model.NotificationType;
import com.kaif.chatapp.notification.repository.NotificationRepository;
import com.kaif.chatapp.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private NotificationService notificationService;

    @Test
    @DisplayName("Create notification - saves and pushes via WebSocket")
    void createAndPush_savesAndPushes() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            n.setCreatedAt(LocalDateTime.now());
            return n;
        });

        notificationService.createAndPush(2L, NotificationType.NEW_MESSAGE,
                "kaif sent a message", "Hello!", 5L);

        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/notifications"), any(NotificationDTO.class));
    }

    @Test
    @DisplayName("Mark notification as read")
    void markAsRead_updatesNotification() {
        Notification notification = Notification.builder()
                .id(1L).userId(2L).type(NotificationType.NEW_MESSAGE)
                .title("Test").isRead(false).build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markAsRead(1L, 2L);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("Get unread count")
    void getUnreadCount_returnsCorrectCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(2L)).thenReturn(5L);

        long count = notificationService.getUnreadCount(2L);

        assertThat(count).isEqualTo(5);
    }
}
