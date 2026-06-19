package com.kaif.chatapp.mobile.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_accounts",
       uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_user_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
