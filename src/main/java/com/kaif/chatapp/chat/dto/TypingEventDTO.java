package com.kaif.chatapp.chat.dto;

import lombok.*;

import java.util.Set;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TypingEventDTO {
    private Long conversationId;
    private Long senderId;
    private Set<String> typingUsers;
}
