package com.kaif.chatapp.user.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

/**
 * User entity representing the 'users' table.
 * Implements {@link UserDetails} for native Spring Security integration.
 * Passwords are BCrypt-hashed; never exposed in DTOs.
 *
 * <p>Note on naming: {@link #getUsername()} satisfies {@code UserDetails} and
 * returns the unique <em>email</em> (used as the Spring Security principal name).
 * Use {@link #getDisplayUsername()} to get the user's chat handle / screen name.</p>
 */
@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The chat handle / screen name stored in the DB 'username' column.
     * Lombok's getter is suppressed to avoid clashing with
     * {@link UserDetails#getUsername()}; use {@link #getDisplayUsername()} instead.
     */
    @Getter(AccessLevel.NONE)
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "email_verified")
    @Builder.Default
    private boolean emailVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Convenience accessor ────────────────────────────────────────────────

    /**
     * Returns the user's chat handle (the 'username' column in the DB).
     * Use this everywhere you need the user's visible name / screen name.
     */
    public String getDisplayUsername() {
        return username;
    }

    /**
     * Setter for the 'username' column (Lombok @Setter is still active for the field).
     * Provided explicitly so callers have a clear contract.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    // ── UserDetails implementation ──────────────────────────────────────────

    /**
     * Returns the email address — used as the Spring Security principal identifier.
     * To get the visible chat username, call {@link #getDisplayUsername()}.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /** BCrypt-hashed password stored in the DB — returned as-is. */
    @Override
    public String getPassword() {
        return password;
    }

    /** No role-based permissions yet; returns an empty authority list. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
