package com.twocall.chat.repository;

import com.twocall.chat.domain.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findAllByPairIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID pairId, Instant after);
    List<Message> findTop50ByPairIdOrderByCreatedAtDesc(UUID pairId);
    Optional<Message> findByIdAndPairId(UUID id, UUID pairId);
    Optional<Message> findByPairIdAndClientMessageId(UUID pairId, String clientMessageId);
    void deleteAllByPairId(UUID pairId);
}
