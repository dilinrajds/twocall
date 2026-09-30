package com.twocall.chat.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pairing_codes")
public class PairingCode {

    @Id
    private UUID id;

    @Column(name = "code_hash", nullable = false, unique = true, length = 64)
    private String codeHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pair_id", nullable = false)
    private Pair pair;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_device_id", nullable = false)
    private Device creatorDevice;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempts_count", nullable = false)
    private int attemptsCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 5;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PairingCode() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    public PairingCode(UUID id, String codeHash, Pair pair, Device creatorDevice, Instant expiresAt, int maxAttempts) {
        this.id = id != null ? id : UUID.randomUUID();
        this.codeHash = codeHash;
        this.pair = pair;
        this.creatorDevice = creatorDevice;
        this.expiresAt = expiresAt;
        this.maxAttempts = maxAttempts;
        this.attemptsCount = 0;
        this.used = false;
        this.createdAt = Instant.now();
    }

    public boolean isValid() {
        return !used && attemptsCount < maxAttempts && Instant.now().isBefore(expiresAt);
    }

    public void incrementAttempts() {
        this.attemptsCount++;
    }

    public void markUsed() {
        this.used = true;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public Pair getPair() {
        return pair;
    }

    public void setPair(Pair pair) {
        this.pair = pair;
    }

    public Device getCreatorDevice() {
        return creatorDevice;
    }

    public void setCreatorDevice(Device creatorDevice) {
        this.creatorDevice = creatorDevice;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getAttemptsCount() {
        return attemptsCount;
    }

    public void setAttemptsCount(int attemptsCount) {
        this.attemptsCount = attemptsCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
