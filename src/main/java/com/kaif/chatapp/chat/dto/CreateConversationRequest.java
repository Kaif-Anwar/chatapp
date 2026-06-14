package com.kaif.chatapp.chat.dto;

import com.kaif.chatapp.chat.model.ConversationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CreateConversationRequest {

    @NotNull(message = "Conversation type is required")
    private ConversationType type;

    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotEmpty(message = "At least one participant is required")
    private List<Long> participantIds;
}
