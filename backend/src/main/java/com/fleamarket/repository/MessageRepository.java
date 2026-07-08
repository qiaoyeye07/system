package com.fleamarket.repository;

import com.fleamarket.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE " +
           "((m.sender.id = :userId1 AND m.receiver.id = :userId2) OR " +
           "(m.sender.id = :userId2 AND m.receiver.id = :userId1)) " +
           "AND (:productId IS NULL OR m.product.id = :productId) " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findConversation(@Param("userId1") Long userId1,
                                   @Param("userId2") Long userId2,
                                   @Param("productId") Long productId,
                                   Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.isRead = false")
    long countUnreadByReceiver(@Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE m.id IN " +
           "(SELECT MAX(m2.id) FROM Message m2 WHERE m2.sender.id = :userId OR m2.receiver.id = :userId GROUP BY " +
           "CASE WHEN m2.sender.id = :userId THEN m2.receiver.id ELSE m2.sender.id END, COALESCE(m2.product.id, 0)) " +
           "ORDER BY m.createdAt DESC")
    List<Message> findLastMessagesByUser(@Param("userId") Long userId);
}
