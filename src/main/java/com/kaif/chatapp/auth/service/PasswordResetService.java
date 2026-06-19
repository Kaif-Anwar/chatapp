package com.kaif.chatapp.auth.service;

import com.kaif.chatapp.auth.dto.ResetPasswordRequest;
import com.kaif.chatapp.auth.model.EmailVerificationToken;
import com.kaif.chatapp.auth.model.PasswordResetToken;
import com.kaif.chatapp.auth.repository.EmailVerificationTokenRepository;
import com.kaif.chatapp.auth.repository.PasswordResetTokenRepository;
import com.kaif.chatapp.auth.repository.RefreshTokenRepository;
import com.kaif.chatapp.exception.BadRequestException;
import com.kaif.chatapp.user.model.User;
import com.kaif.chatapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.auth.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /** Always succeeds from caller perspective — no email enumeration. */
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = TokenHashUtil.generateToken();
            PasswordResetToken entity = PasswordResetToken.builder()
                    .userId(user.getId())
                    .tokenHash(TokenHashUtil.hashToken(rawToken))
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .createdAt(LocalDateTime.now())
                    .build();
            resetTokenRepository.save(entity);
            emailService.sendPasswordResetEmail(user.getEmail(),
                    frontendUrl + "/reset-password?token=" + rawToken);
            log.info("Password reset requested for user id={}", user.getId());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String hash = TokenHashUtil.hashToken(request.getToken());
        PasswordResetToken token = resetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (token.isUsed() || token.isExpired()) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        resetTokenRepository.save(token);
        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("Password reset completed for user id={}", user.getId());
    }

    @Transactional
    public void sendVerificationEmail(User user) {
        String rawToken = TokenHashUtil.generateToken();
        EmailVerificationToken entity = EmailVerificationToken.builder()
                .userId(user.getId())
                .tokenHash(TokenHashUtil.hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();
        verificationTokenRepository.save(entity);
        emailService.sendVerificationEmail(user.getEmail(),
                frontendUrl + "/verify-email?token=" + rawToken);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        String hash = TokenHashUtil.hashToken(rawToken);
        EmailVerificationToken token = verificationTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        if (token.isUsed() || token.isExpired()) {
            throw new BadRequestException("Invalid or expired verification token");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        user.setEmailVerified(true);
        userRepository.save(user);
        token.setUsedAt(LocalDateTime.now());
        verificationTokenRepository.save(token);
        log.info("Email verified for user id={}", user.getId());
    }

    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                sendVerificationEmail(user);
            }
        });
    }
}
