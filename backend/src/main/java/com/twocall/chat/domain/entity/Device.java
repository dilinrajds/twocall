package com.twocall.chat.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"pair_id", "device_fingerprint"}, name = "uq_device_pair_fingerprint")
})
public class Device {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pair_id", nullable = false)
    private Pair pair;

    @Column(name = "device_fingerprint", nullable = false, length = 128)
    private String deviceFingerprint;

    @Column(name = "public_identity_key", columnDefinition = "TEXT")
    private String publicIdentityKey;

    @Column(name = "device_label", length = 64)
    private String deviceLabel;

    @Column(name = "profile_image", columnDefinition = "TEXT")
    private String profileImage;

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt;

    public Device() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.lastActiveAt = Instant.now();
    }

    public Device(UUID id, Pair pair, String deviceFingerprint, String publicIdentityKey, String deviceLabel) {
        this.id = id != null ? id : UUID.randomUUID();
        this.pair = pair;
        this.deviceFingerprint = deviceFingerprint;
        this.publicIdentityKey = publicIdentityKey;
        this.deviceLabel = deviceLabel;
        this.createdAt = Instant.now();
        this.lastActiveAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Pair getPair() {
        return pair;
    }

    public void setPair(Pair pair) {
        this.pair = pair;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public String getPublicIdentityKey() {
        return publicIdentityKey;
    }

    public void setPublicIdentityKey(String publicIdentityKey) {
        this.publicIdentityKey = publicIdentityKey;
    }

    public String getDeviceLabel() {
        return deviceLabel;
    }

    public void setDeviceLabel(String deviceLabel) {
        this.deviceLabel = deviceLabel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
}
