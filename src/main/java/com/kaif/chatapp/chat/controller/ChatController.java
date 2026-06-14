package com.kaif.chatapp.chat.controller;

import com.kaif.chatapp.chat.dto.*;
import com.kaif.chatapp.chat.service.ChatService;
import com.kaif.chatapp.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Create and manage chat conversations")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "Create a private or group conversation")
    public ResponseEntity<ConversationDTO> createConversation(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateConversationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.createConversation(user, request));
    }

    @GetMapping
    @Operation(summary = "Get all conversations for current user")
    public ResponseEntity<List<ConversationDTO>> getUserConversations(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getUserConversations(user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single conversation details")
    public ResponseEntity<ConversationDTO> getConversation(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(chatService.getConversation(id, user));
    }

    @PostMapping("/{id}/participants")
    @Operation(summary = "Add participants to a group conversation (admin only)")
    public ResponseEntity<ConversationDTO> addParticipants(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody AddParticipantsRequest request) {
        return ResponseEntity.ok(chatService.addParticipants(id, user, request));
    }

    @DeleteMapping("/{id}/participants/{userId}")
    @Operation(summary = "Remove a participant from a group conversation (admin only)")
    public ResponseEntity<Void> removeParticipant(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @PathVariable Long userId) {
        chatService.removeParticipant(id, userId, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Leave a conversation")
    public ResponseEntity<Void> leaveConversation(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        chatService.leaveConversation(id, user);
        return ResponseEntity.noContent().build();
    }
}
