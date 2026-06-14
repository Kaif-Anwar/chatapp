package com.kaif.chatapp.chat.repository;

import com.kaif.chatapp.chat.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c JOIN ConversationParticipant cp ON c.id = cp.conversationId " +
           "WHERE cp.userId = :userId ORDER BY c.createdAt DESC")
    List<Conversation> findAllByParticipantUserId(Long userId);

    @Query("SELECT c FROM Conversation c JOIN ConversationParticipant cp1 ON c.id = cp1.conversationId " +
           "JOIN ConversationParticipant cp2 ON c.id = cp2.conversationId " +
           "WHERE c.type = 'PRIVATE' AND cp1.userId = :userId1 AND cp2.userId = :userId2")
    List<Conversation> findPrivateConversation(Long userId1, Long userId2);
}
