package com.twocall.chat.repository;

import com.twocall.chat.domain.entity.CallSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CallSessionRepository extends JpaRepository<CallSession, UUID> {
    Optional<CallSession> findByIdAndPairId(UUID id, UUID pairId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from CallSession c where c.id = :id and c.pair.id = :pairId")
    Optional<CallSession> findLockedByIdAndPairId(@org.springframework.data.repository.query.Param("id") UUID id,
            @org.springframework.data.repository.query.Param("pairId") UUID pairId);
}
