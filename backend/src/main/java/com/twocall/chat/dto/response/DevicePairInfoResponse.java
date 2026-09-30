package com.twocall.chat.dto.response;

import com.twocall.chat.domain.enums.PairStatus;
import java.util.UUID;

public class DevicePairInfoResponse {
    private UUID pairId;
    private UUID deviceId;
    private UUID partnerDeviceId;
    private String partnerPublicKey;
    private PairStatus pairStatus;

    public DevicePairInfoResponse() {}

    public DevicePairInfoResponse(UUID pairId, UUID deviceId, UUID partnerDeviceId, String partnerPublicKey, PairStatus pairStatus) {
        this.pairId = pairId;
        this.deviceId = deviceId;
        this.partnerDeviceId = partnerDeviceId;
        this.partnerPublicKey = partnerPublicKey;
        this.pairStatus = pairStatus;
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

    public PairStatus getPairStatus() {
        return pairStatus;
    }

    public void setPairStatus(PairStatus pairStatus) {
        this.pairStatus = pairStatus;
    }
}
