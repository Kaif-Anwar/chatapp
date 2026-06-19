package com.kaif.chatapp.mobile.service;

import com.kaif.chatapp.auth.dto.AuthResponse;
import com.kaif.chatapp.auth.service.AuthService;
import com.kaif.chatapp.exception.BadRequestException;
import com.kaif.chatapp.mobile.dto.OAuthLoginRequest;
import com.kaif.chatapp.mobile.model.OAuthAccount;
import com.kaif.chatapp.mobile.repository.OAuthAccountRepository;
import com.kaif.chatapp.user.model.User;
import com.kaif.chatapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OAuth linking for mobile apps. Provider token verification should be added
 * per provider (Google/Apple) when mobile clients are ready.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService {

    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse loginWithOAuth(OAuthLoginRequest request) {
        String provider = request.getProvider().toUpperCase();
        if (!provider.equals("GOOGLE") && !provider.equals("APPLE")) {
            throw new BadRequestException("Unsupported OAuth provider");
        }

        // TODO: verify idToken with Google/Apple SDK when mobile app integrates.
        String providerUserId = hashProviderSubject(request.getIdToken());

        OAuthAccount existing = oauthAccountRepository
                .findByProviderAndProviderUserId(provider, providerUserId)
                .orElse(null);

        User user;
        if (existing != null) {
            user = userRepository.findById(existing.getUserId())
                    .orElseThrow(() -> new BadRequestException("Linked user not found"));
        } else {
            String email = request.getEmail() != null ? request.getEmail()
                    : provider.toLowerCase() + "_" + providerUserId.substring(0, 8) + "@oauth.local";
            user = userRepository.findByEmail(email).orElseGet(() -> {
                User u = User.builder()
                        .username("oauth_" + UUID.randomUUID().toString().substring(0, 8))
                        .email(email)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .displayName(request.getDisplayName() != null ? request.getDisplayName() : email)
                        .emailVerified(true)
                        .build();
                return userRepository.save(u);
            });

            oauthAccountRepository.save(OAuthAccount.builder()
                    .userId(user.getId())
                    .provider(provider)
                    .providerUserId(providerUserId)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        log.info("OAuth login via {} for user id={}", provider, user.getId());
        return authService.loginExistingUser(user);
    }

    private String hashProviderSubject(String idToken) {
        return Integer.toHexString(idToken.hashCode());
    }
}
