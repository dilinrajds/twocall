package com.twocall.chat.dto.response;

import java.time.Instant;
import java.util.UUID;

public class CreatePairResponse {
    private UUID pairId;
    private String pairingCode;
    private Instant expiresAt;
    private UUID deviceId;
    private String accessToken;
    private String refreshToken;

    public CreatePairResponse() {}

    public CreatePairResponse(UUID pairId, String pairingCode, Instant expiresAt, UUID deviceId, String accessToken, String refreshToken) {
        this.pairId = pairId;
        this.pairingCode = pairingCode;
        this.expiresAt = expiresAt;
        this.deviceId = deviceId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public UUID getPairId() {
        return pairId;
    }

    public void setPairId(UUID pairId) {
        this.pairId = pairId;
    }

    public String getPairingCode() {
        return pairingCode;
    }

    public void setPairingCode(String pairingCode) {
        this.pairingCode = pairingCode;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
