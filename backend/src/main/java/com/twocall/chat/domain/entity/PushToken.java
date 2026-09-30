package com.twocall.chat.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "push_tokens", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"device_id"}, name = "uq_device_push_token")
})
public class PushToken {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "fcm_token", nullable = false, columnDefinition = "TEXT")
    private String fcmToken;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PushToken() {
        this.id = UUID.randomUUID();
        this.updatedAt = Instant.now();
    }

    public PushToken(UUID id, Device device, String fcmToken) {
        this.id = id != null ? id : UUID.randomUUID();
        this.device = device;
        this.fcmToken = fcmToken;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
