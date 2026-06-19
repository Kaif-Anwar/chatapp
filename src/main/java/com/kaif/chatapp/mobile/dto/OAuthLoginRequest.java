package com.kaif.chatapp.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OAuthLoginRequest {
    @NotBlank
    private String provider;
    @NotBlank
    private String idToken;
    private String email;
    private String displayName;
}
