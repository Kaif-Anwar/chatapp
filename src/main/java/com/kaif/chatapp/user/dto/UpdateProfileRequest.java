package com.kaif.chatapp.user.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for updating user profile fields (display name only).
 *
 * <p>Note: avatar updates must go through POST /api/users/me/avatar
 * (multipart/form-data) — not this endpoint. Allowing avatarUrl here
 * would let clients bypass file validation and store arbitrary URLs.</p>
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 100, message = "Display name must be at most 100 characters")
    private String displayName;
}
