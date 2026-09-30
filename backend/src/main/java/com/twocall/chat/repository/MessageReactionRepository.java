package com.twocall.chat.repository;

import com.twocall.chat.domain.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {
    Optional<MessageReaction> findByMessageIdAndDeviceId(UUID messageId, UUID deviceId);
}
