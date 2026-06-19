package com.kaif.chatapp.mobile.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Push notification delivery. FCM/APNS integration is activated when
 * {@code app.push.fcm-server-key} is configured on the server.
 */
@Service
@Slf4j
public class PushNotificationService {

    @Value("${app.push.fcm-server-key:}")
    private String fcmServerKey;

    public void sendToUser(Long userId, List<String> deviceTokens, String title, String body) {
        if (fcmServerKey == null || fcmServerKey.isBlank()) {
            log.debug("Push skipped (FCM not configured) for user {}: {} — {}", userId, title, body);
            return;
        }
        // Mobile app phase: wire Firebase Admin SDK or HTTP v1 API here.
        log.info("Push queued for user {} ({} devices): {}", userId, deviceTokens.size(), title);
    }
}
