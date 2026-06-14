package com.kaif.chatapp.presence.controller;

import com.kaif.chatapp.presence.service.PresenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for HTTP-based presence queries.
 * STOMP heartbeat handler lives in {@link PresenceWebSocketController}.
 */
@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
@Tag(name = "Presence", description = "Online/offline user status tracking")
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping("/{userId}")
    @Operation(summary = "Check if a user is online")
    public ResponseEntity<Map<String, Object>> getUserPresence(@PathVariable Long userId) {
        boolean online = presenceService.isOnline(userId);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "status", online ? "ONLINE" : "OFFLINE"));
    }
}
