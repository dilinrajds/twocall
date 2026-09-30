package com.twocall.chat.repository;

import com.twocall.chat.domain.entity.DeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceSessionRepository extends JpaRepository<DeviceSession, UUID> {
    Optional<DeviceSession> findByRefreshTokenHashAndRevokedFalse(String refreshTokenHash);
    void deleteAllByDeviceId(UUID deviceId);
}
