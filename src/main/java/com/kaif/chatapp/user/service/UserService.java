package com.kaif.chatapp.user.service;

import com.kaif.chatapp.exception.ResourceNotFoundException;
import com.kaif.chatapp.presence.service.PresenceService;
import com.kaif.chatapp.user.dto.UpdateProfileRequest;
import com.kaif.chatapp.user.dto.UserDTO;
import com.kaif.chatapp.user.model.User;
import com.kaif.chatapp.user.model.UserStatus;
import com.kaif.chatapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PresenceService presenceService;
    private final AvatarService avatarService;

    public UserDTO getCurrentUser(User user) {
        return toDTO(user);
    }

    public UserDTO updateProfile(User user, UpdateProfileRequest request) {
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        User saved = userRepository.save(user);
        return toDTO(saved);
    }


    /**
     * Stores the uploaded avatar file and updates the user's avatarUrl.
     * The old avatar file is deleted from disk after successful upload.
     */
    public UserDTO updateAvatar(User user, MultipartFile file) throws IOException {
        String oldAvatarUrl = user.getAvatarUrl();

        String newAvatarUrl = avatarService.storeAvatar(file, user.getId());
        user.setAvatarUrl(newAvatarUrl);
        User saved = userRepository.save(user);

        // Clean up old file only after the DB write succeeds
        avatarService.deleteAvatar(oldAvatarUrl);

        return toDTO(saved);
    }

    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return toDTO(user);
    }

    public List<UserDTO> searchUsers(String query) {
        return userRepository.findByUsernameContainingIgnoreCase(query).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UserDTO toDTO(User user) {
        UserStatus status = presenceService.isOnline(user.getId()) ? UserStatus.ONLINE : UserStatus.OFFLINE;
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getDisplayUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .status(status)
                .createdAt(user.getCreatedAt())
                .emailVerified(user.isEmailVerified())
                .build();
    }
}
