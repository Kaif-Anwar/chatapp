package com.kaif.chatapp.auth;

import com.kaif.chatapp.auth.dto.LoginRequest;
import com.kaif.chatapp.auth.dto.RegisterRequest;
import com.kaif.chatapp.auth.dto.AuthResponse;
import com.kaif.chatapp.auth.dto.RefreshTokenRequest;
import com.kaif.chatapp.auth.model.RefreshToken;
import com.kaif.chatapp.auth.repository.RefreshTokenRepository;
import com.kaif.chatapp.auth.service.AuthService;
import com.kaif.chatapp.auth.service.PasswordResetService;
import com.kaif.chatapp.exception.DuplicateResourceException;
import com.kaif.chatapp.exception.UnauthorizedException;
import com.kaif.chatapp.security.JwtUtil;
import com.kaif.chatapp.user.model.User;
import com.kaif.chatapp.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordResetService passwordResetService;

    @InjectMocks private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpiryMs", 604800000L);
        ReflectionTestUtils.setField(authService, "requireEmailVerification", false);
    }

    @Test
    @DisplayName("Register - success")
    void register_success() {
        RegisterRequest request = new RegisterRequest("kaif", "kaif@test.com", "password123", "Kaif");

        when(userRepository.existsByEmail("kaif@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("kaif")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtil.generateAccessToken(eq(1L), eq("kaif"), eq("kaif@test.com"))).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(eq(1L), eq("kaif@test.com"))).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUsername()).isEqualTo("kaif");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register - duplicate email throws DuplicateResourceException")
    void register_duplicateEmail() {
        RegisterRequest request = new RegisterRequest("kaif", "kaif@test.com", "password123", null);
        when(userRepository.existsByEmail("kaif@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    @DisplayName("Register - duplicate username throws DuplicateResourceException")
    void register_duplicateUsername() {
        RegisterRequest request = new RegisterRequest("kaif", "new@test.com", "password123", null);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("kaif")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    @DisplayName("Login - success")
    void login_success() {
        LoginRequest request = new LoginRequest("kaif@test.com", "password123");
        User user = User.builder().id(1L).username("kaif").email("kaif@test.com").password("$2a$12$hashed").build();

        when(userRepository.findByEmail("kaif@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$12$hashed")).thenReturn(true);
        when(jwtUtil.generateAccessToken(1L, "kaif", "kaif@test.com")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(1L, "kaif@test.com")).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Login - wrong password throws UnauthorizedException")
    void login_wrongPassword() {
        LoginRequest request = new LoginRequest("kaif@test.com", "wrongpassword");
        User user = User.builder().id(1L).email("kaif@test.com").password("$2a$12$hashed").build();

        when(userRepository.findByEmail("kaif@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "$2a$12$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Login - non-existent email throws UnauthorizedException")
    void login_nonExistentEmail() {
        LoginRequest request = new LoginRequest("nonexistent@test.com", "password123");
        when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }
}
