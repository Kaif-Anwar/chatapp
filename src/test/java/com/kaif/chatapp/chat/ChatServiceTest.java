package com.kaif.chatapp.chat;

import com.kaif.chatapp.chat.dto.CreateConversationRequest;
import com.kaif.chatapp.chat.dto.ConversationDTO;
import com.kaif.chatapp.chat.dto.AddParticipantsRequest;
import com.kaif.chatapp.chat.model.*;
import com.kaif.chatapp.chat.repository.*;
import com.kaif.chatapp.chat.service.ChatService;
import com.kaif.chatapp.chat.service.RedisSubscriptionManager;
import com.kaif.chatapp.exception.BadRequestException;
import com.kaif.chatapp.exception.ForbiddenException;
import com.kaif.chatapp.notification.service.NotificationService;
import com.kaif.chatapp.user.model.User;
import com.kaif.chatapp.user.repository.UserRepository;
import com.kaif.chatapp.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationParticipantRepository participantRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private NotificationService notificationService;
    @Mock private RedisSubscriptionManager redisSubscriptionManager;

    @InjectMocks private ChatService chatService;

    private User createUser(Long id, String username) {
        return User.builder().id(id).username(username).email(username + "@test.com").build();
    }

    @Test
    @DisplayName("Create private conversation - success")
    void createPrivateConversation_success() {
        User currentUser = createUser(1L, "kaif");
        User otherUser = createUser(2L, "ali");
        CreateConversationRequest request = new CreateConversationRequest(ConversationType.PRIVATE, null, List.of(2L));

        when(conversationRepository.findPrivateConversation(1L, 2L)).thenReturn(Collections.emptyList());
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });
        when(participantRepository.save(any(ConversationParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(participantRepository.findByConversationId(10L)).thenReturn(List.of(
                ConversationParticipant.builder().conversationId(10L).userId(1L).build(),
                ConversationParticipant.builder().conversationId(10L).userId(2L).build()
        ));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(userService.toDTO(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return com.kaif.chatapp.user.dto.UserDTO.builder()
                    .id(u.getId()).username(u.getDisplayUsername()).email(u.getEmail()).build();
        });

        ConversationDTO result = chatService.createConversation(currentUser, request);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getType()).isEqualTo(ConversationType.PRIVATE);
        verify(conversationRepository).save(any());
    }

    @Test
    @DisplayName("Create private conversation - too many participants throws BadRequest")
    void createPrivateConversation_tooManyParticipants() {
        User currentUser = createUser(1L, "kaif");
        CreateConversationRequest request = new CreateConversationRequest(
                ConversationType.PRIVATE, null, List.of(2L, 3L));

        assertThatThrownBy(() -> chatService.createConversation(currentUser, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exactly one other participant");
    }

    @Test
    @DisplayName("Create group conversation - success")
    void createGroupConversation_success() {
        User currentUser = createUser(1L, "kaif");
        CreateConversationRequest request = new CreateConversationRequest(
                ConversationType.GROUP, "Test Group", List.of(2L, 3L));

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(20L);
            return c;
        });
        when(participantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(2L)).thenReturn(Optional.of(createUser(2L, "ali")));
        when(userRepository.findById(3L)).thenReturn(Optional.of(createUser(3L, "sara")));
        when(participantRepository.findByConversationId(20L)).thenReturn(List.of(
                ConversationParticipant.builder().conversationId(20L).userId(1L).isAdmin(true).build(),
                ConversationParticipant.builder().conversationId(20L).userId(2L).build(),
                ConversationParticipant.builder().conversationId(20L).userId(3L).build()
        ));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(userService.toDTO(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return com.kaif.chatapp.user.dto.UserDTO.builder()
                    .id(u.getId()).username(u.getDisplayUsername()).email(u.getEmail()).build();
        });

        ConversationDTO result = chatService.createConversation(currentUser, request);

        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getType()).isEqualTo(ConversationType.GROUP);
        assertThat(result.getName()).isEqualTo("Test Group");
    }

    @Test
    @DisplayName("Add participant - non-admin throws ForbiddenException")
    void addParticipant_nonAdminForbidden() {
        User currentUser = createUser(1L, "kaif");
        Conversation conversation = Conversation.builder().id(20L).type(ConversationType.GROUP).name("Group").createdBy(2L).build();
        AddParticipantsRequest request = new AddParticipantsRequest(List.of(4L));

        when(conversationRepository.findById(20L)).thenReturn(Optional.of(conversation));
        when(participantRepository.findByConversationIdAndUserId(20L, 1L))
                .thenReturn(Optional.of(ConversationParticipant.builder()
                        .conversationId(20L).userId(1L).isAdmin(false).build()));

        assertThatThrownBy(() -> chatService.addParticipants(20L, currentUser, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only group admin");
    }
}
