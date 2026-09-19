package com.campus.trade.repository;

import com.campus.trade.entity.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m " +
           "JOIN FETCH m.fromUser " +
           "LEFT JOIN FETCH m.product " +
           "WHERE m.toUser.id = :userId ORDER BY m.createdAt DESC")
    List<Message> findInboxByUserId(@Param("userId") Long userId);

    long countByToUserIdAndIsReadFalse(Long toUserId);

    @Query("SELECT m FROM Message m " +
           "JOIN FETCH m.fromUser " +
           "JOIN FETCH m.toUser " +
           "LEFT JOIN FETCH m.product " +
           "WHERE (m.fromUser.id = :u1 AND m.toUser.id = :u2) " +
           "   OR (m.fromUser.id = :u2 AND m.toUser.id = :u1) " +
           "ORDER BY m.createdAt ASC")
    List<Message> findConversation(@Param("u1") Long u1, @Param("u2") Long u2);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true " +
           "WHERE m.fromUser.id = :fromId AND m.toUser.id = :toId AND m.isRead = false")
    int markConversationRead(@Param("fromId") Long fromId, @Param("toId") Long toId);
}
