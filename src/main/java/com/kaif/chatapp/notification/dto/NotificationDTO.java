package com.kaif.chatapp.notification.dto;

import com.kaif.chatapp.notification.model.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotificationDTO {

    private Long id;
    private NotificationType type;
    private String title;
    private String body;
    private Long refId;
    private LocalDateTime createdAt;
    private boolean isRead;
}
