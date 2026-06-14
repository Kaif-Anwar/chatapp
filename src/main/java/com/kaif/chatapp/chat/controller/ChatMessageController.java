package com.kaif.chatapp.chat.controller;

import com.kaif.chatapp.chat.dto.*;
import com.kaif.chatapp.chat.service.MessageService;
import com.kaif.chatapp.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Send, retrieve, edit, and delete messages")
public class ChatMessageController {

    private final MessageService messageService;

    @PostMapping
    @Operation(summary = "Send a message to a conversation")
    public ResponseEntity<MessageDTO> sendMessage(
            @AuthenticationPrincipal User user,
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.sendMessage(conversationId, user, request));
    }

    @GetMapping
    @Operation(summary = "Get paginated message history")
    public ResponseEntity<Page<MessageDTO>> getMessages(
            @AuthenticationPrincipal User user,
            @PathVariable Long conversationId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(messageService.getMessageHistory(conversationId, user, pageable));
    }

    @PutMapping("/{msgId}")
    @Operation(summary = "Edit a message (sender only)")
    public ResponseEntity<MessageDTO> editMessage(
            @AuthenticationPrincipal User user,
            @PathVariable Long conversationId,
            @PathVariable Long msgId,
            @Valid @RequestBody EditMessageRequest request) {
        return ResponseEntity.ok(messageService.editMessage(conversationId, msgId, user, request));
    }

    @DeleteMapping("/{msgId}")
    @Operation(summary = "Soft-delete a message (sender only)")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal User user,
            @PathVariable Long conversationId,
            @PathVariable Long msgId) {
        messageService.deleteMessage(conversationId, msgId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{msgId}/read")
    @Operation(summary = "Mark a message as read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable Long conversationId,
            @PathVariable Long msgId) {
        messageService.markMessageAsRead(conversationId, msgId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread message count for this conversation")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal User user,
            @PathVariable Long conversationId) {
        long count = messageService.getUnreadCount(conversationId, user);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
