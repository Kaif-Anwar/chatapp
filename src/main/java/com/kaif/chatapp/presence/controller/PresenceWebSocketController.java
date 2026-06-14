package com.kaif.chatapp.presence.controller;

import com.kaif.chatapp.presence.service.PresenceService;
import com.kaif.chatapp.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP controller for real-time presence heartbeats.
 *
 * <p>Must be a plain {@code @Controller} (not {@code @RestController}) so that
 * Spring's WebSocket message dispatcher registers the {@code @MessageMapping}
 * handler. Presence REST queries live in {@link PresenceController}.</p>
 *
 * <p>Clients should send a ping to {@code /app/presence.ping} every ~20 seconds
 * to renew the Redis TTL and stay marked as ONLINE.</p>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PresenceWebSocketController {

    private final PresenceService presenceService;

    /**
     * Handles STOMP heartbeat pings sent to {@code /app/presence.ping}.
     * Renews the Redis TTL so the user remains ONLINE.
     *
     * @param principal the authenticated principal injected by the STOMP interceptor
     */
    @MessageMapping("/presence.ping")
    public void ping(Principal principal) {
        if (principal == null) {
            log.warn("Unauthenticated presence ping ignored");
            return;
        }

        User user = (User) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
        presenceService.refreshPresence(user.getId());
        log.debug("Presence ping received from user {}", user.getId());
    }
}
