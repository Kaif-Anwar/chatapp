package com.kaif.chatapp.mobile.repository;

import com.kaif.chatapp.mobile.model.OAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {
    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
}
