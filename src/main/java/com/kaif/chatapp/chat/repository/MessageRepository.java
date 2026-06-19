package com.kaif.chatapp.chat.repository;

import com.kaif.chatapp.chat.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    Optional<Message> findTopByConversationIdOrderByCreatedAtDesc(Long conversationId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.createdAt > :lastReadAt AND m.senderId != :userId AND m.isDeleted = false")
    long countUnreadMessages(Long conversationId, Long userId, LocalDateTime lastReadAt);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.senderId != :userId AND m.isDeleted = false")
    long countAllUnreadMessages(Long conversationId, Long userId);

    @Query(value = "SELECT * FROM messages m WHERE m.conversation_id = :conversationId " +
           "AND m.is_deleted = 0 AND MATCH(m.content) AGAINST(:query IN NATURAL LANGUAGE MODE) " +
           "ORDER BY m.created_at DESC LIMIT 50", nativeQuery = true)
    List<Message> searchInConversation(@Param("conversationId") Long conversationId, @Param("query") String query);
}
