package com.kaif.chatapp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VerifyEmailRequest {
    @NotBlank
    private String token;
}
