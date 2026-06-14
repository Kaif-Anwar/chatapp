package com.kaif.chatapp.chat.dto;

import com.kaif.chatapp.chat.model.MessageStatus;
import com.kaif.chatapp.chat.model.MessageType;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MessageDTO {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderUsername;
    private String content;
    private MessageType type;
    private MessageStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;
    private boolean deleted;
}
