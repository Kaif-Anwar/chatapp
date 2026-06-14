package com.kaif.chatapp.chat.dto;

import com.kaif.chatapp.chat.model.ConversationType;
import com.kaif.chatapp.user.dto.UserDTO;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ConversationDTO {

    private Long id;
    private String name;
    private ConversationType type;
    private Long createdBy;
    private LocalDateTime createdAt;
    private List<UserDTO> participants;
    private MessageDTO lastMessage;
    private long unreadCount;
}
