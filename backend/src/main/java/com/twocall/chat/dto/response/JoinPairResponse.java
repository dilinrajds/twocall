package com.twocall.chat.dto.response;

import java.util.UUID;

public class JoinPairResponse {
    private UUID pairId;
    private UUID deviceId;
    private String accessToken;
    private String refreshToken;
    private UUID partnerDeviceId;
    private String partnerPublicKey;

    public JoinPairResponse() {}

    public JoinPairResponse(UUID pairId, UUID deviceId, String accessToken, String refreshToken, UUID partnerDeviceId, String partnerPublicKey) {
        this.pairId = pairId;
        this.deviceId = deviceId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.partnerDeviceId = partnerDeviceId;
        this.partnerPublicKey = partnerPublicKey;
    }

    public UUID getPairId() {
        return pairId;
    }

    public void setPairId(UUID pairId) {
        this.pairId = pairId;
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

    public UUID getPartnerDeviceId() {
        return partnerDeviceId;
    }

    public void setPartnerDeviceId(UUID partnerDeviceId) {
        this.partnerDeviceId = partnerDeviceId;
    }

    public String getPartnerPublicKey() {
        return partnerPublicKey;
    }

    public void setPartnerPublicKey(String partnerPublicKey) {
        this.partnerPublicKey = partnerPublicKey;
    }
}
