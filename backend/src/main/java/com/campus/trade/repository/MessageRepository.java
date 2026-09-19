package com.campus.trade.repository;

import com.campus.trade.entity.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m " +
           "JOIN FETCH m.fromUser " +
           "LEFT JOIN FETCH m.product " +
           "WHERE m.toUser.id = :userId ORDER BY m.createdAt DESC")
    List<Message> findInboxByUserId(@Param("userId") Long userId);

    long countByToUserIdAndIsReadFalse(Long toUserId);
}
