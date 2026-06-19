package com.kaif.chatapp.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterDeviceRequest {
    @NotBlank
    private String token;
    @NotBlank
    private String platform;
}
