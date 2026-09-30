package com.twocall.chat.repository;

import com.twocall.chat.domain.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    long countByPairId(UUID pairId);
    List<Device> findAllByPairId(UUID pairId);
    Optional<Device> findByPairIdAndDeviceFingerprint(UUID pairId, String fingerprint);
    Optional<Device> findByIdAndPairId(UUID deviceId, UUID pairId);
}
