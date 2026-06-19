package com.kaif.chatapp.auth.service;

import com.kaif.chatapp.auth.dto.*;
import com.kaif.chatapp.auth.model.RefreshToken;
import com.kaif.chatapp.auth.repository.RefreshTokenRepository;
import com.kaif.chatapp.exception.BadRequestException;
import com.kaif.chatapp.exception.DuplicateResourceException;
import com.kaif.chatapp.exception.UnauthorizedException;
import com.kaif.chatapp.security.JwtUtil;
import com.kaif.chatapp.user.model.User;
import com.kaif.chatapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PasswordResetService passwordResetService;

    @Value("${jwt.refresh-expiry-ms}")
    private long refreshExpiryMs;

    @Value("${app.auth.require-email-verification:false}")
    private boolean requireEmailVerification;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken: " + request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername())
                .build();

        user = userRepository.save(user);
        log.info("User registered: {} ({})", user.getDisplayUsername(), user.getEmail());

        passwordResetService.sendVerificationEmail(user);

        return generateAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (requireEmailVerification && !user.isEmailVerified()) {
            throw new UnauthorizedException("Please verify your email before logging in");
        }

        log.info("User logged in: {}", user.getDisplayUsername());
        return generateAuthResponse(user);
    }

    public AuthResponse loginExistingUser(User user) {
        return generateAuthResponse(user);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request.getEmail());
    }

    public void resetPassword(ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
    }

    public void verifyEmail(VerifyEmailRequest request) {
        passwordResetService.verifyEmail(request.getToken());
    }

    public void resendVerification(ForgotPasswordRequest request) {
        passwordResetService.resendVerification(request.getEmail());
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }
        if (refreshToken.isExpired()) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        // Revoke old token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return generateAuthResponse(user);
    }

    @Transactional
    public void logout(User user) {
        refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("User logged out: {}", user.getDisplayUsername());
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getDisplayUsername(), user.getEmail());
        String refreshTokenValue = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .userId(user.getId())
                .expiryDate(Instant.now().plusMillis(refreshExpiryMs))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .userId(user.getId())
                .username(user.getDisplayUsername())
                .email(user.getEmail())
                .build();
    }
}
