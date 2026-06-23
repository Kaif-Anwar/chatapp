package com.kaif.chatapp.chat;

import com.kaif.chatapp.chat.dto.EditMessageRequest;
import com.kaif.chatapp.chat.dto.MessageDTO;
import com.kaif.chatapp.chat.dto.SendMessageRequest;
import com.kaif.chatapp.chat.model.*;
import com.kaif.chatapp.chat.repository.*;
import com.kaif.chatapp.chat.service.ChatService;
import com.kaif.chatapp.chat.service.MessageService;
import com.kaif.chatapp.chat.service.RedisMessagePublisher;
import com.kaif.chatapp.chat.service.MessageAttachmentService;
import com.kaif.chatapp.exception.BadRequestException;
import com.kaif.chatapp.exception.ForbiddenException;
import com.kaif.chatapp.notification.service.NotificationService;
import com.kaif.chatapp.user.model.User;
import com.kaif.chatapp.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private ConversationParticipantRepository participantRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChatService chatService;
    @Mock private NotificationService notificationService;
    @Mock private RedisMessagePublisher redisMessagePublisher;
    @Mock private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    @Mock private MessageAttachmentService attachmentService;

    @InjectMocks private MessageService messageService;

    private User createUser(Long id, String username) {
        return User.builder().id(id).username(username).email(username + "@test.com").build();
    }

    @Test
    @DisplayName("Send message - success")
    void sendMessage_success() {
        User sender = createUser(1L, "kaif");
        SendMessageRequest request = new SendMessageRequest("Hello!", MessageType.TEXT);

        doNothing().when(chatService).validateParticipant(5L, 1L);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(100L);
            m.setCreatedAt(LocalDateTime.now());
            return m;
        });
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(participantRepository.findByConversationId(5L)).thenReturn(List.of(
                ConversationParticipant.builder().userId(1L).build(),
                ConversationParticipant.builder().userId(2L).build()
        ));

        MessageDTO result = messageService.sendMessage(5L, sender, request);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getContent()).isEqualTo("Hello!");
        assertThat(result.getSenderUsername()).isEqualTo("kaif");
        verify(redisMessagePublisher).publish(eq(5L), any(MessageDTO.class));
    }

    @Test
    @DisplayName("Send message - non-participant throws ForbiddenException")
    void sendMessage_nonParticipant() {
        User sender = createUser(99L, "hacker");
        SendMessageRequest request = new SendMessageRequest("hack!", MessageType.TEXT);

        doThrow(new ForbiddenException("You are not a participant"))
                .when(chatService).validateParticipant(5L, 99L);

        assertThatThrownBy(() -> messageService.sendMessage(5L, sender, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Edit message - sender can edit own message")
    void editMessage_senderCanEdit() {
        User sender = createUser(1L, "kaif");
        Message existingMessage = Message.builder()
                .id(100L).conversationId(5L).senderId(1L)
                .content("Old content").type(MessageType.TEXT)
                .status(MessageStatus.SENT).isDeleted(false)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC)).build();

        doNothing().when(chatService).validateParticipant(5L, 1L);
        when(messageRepository.findById(100L)).thenReturn(Optional.of(existingMessage));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));

        MessageDTO result = messageService.editMessage(5L, 100L, sender, new EditMessageRequest("New content"));

        assertThat(result.getContent()).isEqualTo("New content");
        assertThat(result.getEditedAt()).isNotNull();
    }

    @Test
    @DisplayName("Edit message - other user cannot edit")
    void editMessage_otherUserForbidden() {
        User otherUser = createUser(2L, "ali");
        Message existingMessage = Message.builder()
                .id(100L).conversationId(5L).senderId(1L)
                .content("Original").type(MessageType.TEXT)
                .status(MessageStatus.SENT).isDeleted(false).build();

        doNothing().when(chatService).validateParticipant(5L, 2L);
        when(messageRepository.findById(100L)).thenReturn(Optional.of(existingMessage));

        assertThatThrownBy(() -> messageService.editMessage(5L, 100L, otherUser, new EditMessageRequest("Hacked")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("only edit your own");
    }

    @Test
    @DisplayName("Edit message - rejected after 30 second window")
    void editMessage_afterEditWindow() {
        User sender = createUser(1L, "kaif");
        Message existingMessage = Message.builder()
                .id(100L).conversationId(5L).senderId(1L)
                .content("Old content").type(MessageType.TEXT)
                .status(MessageStatus.SENT).isDeleted(false)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(31)).build();

        doNothing().when(chatService).validateParticipant(5L, 1L);
        when(messageRepository.findById(100L)).thenReturn(Optional.of(existingMessage));

        assertThatThrownBy(() -> messageService.editMessage(5L, 100L, sender, new EditMessageRequest("Too late")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("30 seconds");
    }

    @Test
    @DisplayName("Delete message - soft deletes correctly")
    void deleteMessage_softDeletes() {
        User sender = createUser(1L, "kaif");
        Message message = Message.builder()
                .id(100L).conversationId(5L).senderId(1L)
                .content("Delete me").isDeleted(false).build();

        doNothing().when(chatService).validateParticipant(5L, 1L);
        when(messageRepository.findById(100L)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        messageService.deleteMessage(5L, 100L, sender);

        assertThat(message.isDeleted()).isTrue();
        verify(messageRepository).save(message);
    }
}
