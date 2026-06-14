package com.kaif.chatapp.chat.dto;

import com.kaif.chatapp.chat.model.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class SendMessageRequest {

    @NotBlank(message = "Message content is required")
    @Size(max = 5000, message = "Message content must be at most 5000 characters")
    private String content;

    private MessageType type = MessageType.TEXT;
}
