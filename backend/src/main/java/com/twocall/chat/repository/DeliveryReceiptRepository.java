package com.twocall.chat.repository;

import com.twocall.chat.domain.entity.DeliveryReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryReceiptRepository extends JpaRepository<DeliveryReceipt, UUID> {
    Optional<DeliveryReceipt> findByMessageIdAndDeviceId(UUID messageId, UUID deviceId);
}
