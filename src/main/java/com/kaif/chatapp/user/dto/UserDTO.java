package com.kaif.chatapp.user.dto;

import com.kaif.chatapp.user.model.UserStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * User data transfer object. Never includes password.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserDTO {

    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private UserStatus status;
    private LocalDateTime createdAt;
}
