package com.kaif.chatapp.chat.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AddParticipantsRequest {

    @NotEmpty(message = "At least one user ID is required")
    private List<Long> userIds;
}
