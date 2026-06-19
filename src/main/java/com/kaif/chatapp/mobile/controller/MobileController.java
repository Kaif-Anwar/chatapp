package com.kaif.chatapp.mobile.controller;

import com.kaif.chatapp.auth.dto.AuthResponse;
import com.kaif.chatapp.mobile.dto.OAuthLoginRequest;
import com.kaif.chatapp.mobile.dto.RegisterDeviceRequest;
import com.kaif.chatapp.mobile.service.DeviceTokenService;
import com.kaif.chatapp.mobile.service.OAuthService;
import com.kaif.chatapp.user.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Mobile", description = "Device tokens and OAuth for mobile clients")
public class MobileController {

    private final DeviceTokenService deviceTokenService;
    private final OAuthService oauthService;

    @PostMapping("/devices/register")
    public ResponseEntity<Void> registerDevice(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RegisterDeviceRequest request) {
        deviceTokenService.registerDevice(user, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/devices/{token}")
    public ResponseEntity<Void> unregisterDevice(
            @AuthenticationPrincipal User user,
            @PathVariable String token) {
        deviceTokenService.unregisterDevice(user, token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/oauth")
    public ResponseEntity<AuthResponse> oauthLogin(@Valid @RequestBody OAuthLoginRequest request) {
        return ResponseEntity.ok(oauthService.loginWithOAuth(request));
    }
}
