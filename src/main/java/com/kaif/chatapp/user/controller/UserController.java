package com.kaif.chatapp.user.controller;

import com.kaif.chatapp.user.dto.UpdateProfileRequest;
import com.kaif.chatapp.user.dto.UserDTO;
import com.kaif.chatapp.user.model.User;
import com.kaif.chatapp.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated   // enables @RequestParam-level constraint validation
@Tag(name = "Users", description = "User profile and search endpoints")
public class UserController {


    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getCurrentUser(user));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile (display name)")
    public ResponseEntity<UserDTO> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(user, request));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a new profile avatar image")
    public ResponseEntity<UserDTO> uploadAvatar(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(userService.updateAvatar(user, file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get user public profile by username")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by username")
    public ResponseEntity<List<UserDTO>> searchUsers(
            @RequestParam("q")
            @NotBlank(message = "Search query is required")
            @Size(min = 1, max = 50, message = "Search query must be 1-50 characters")
            String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }


}
