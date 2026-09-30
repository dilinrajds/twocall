package com.twocall.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreatePairRequest {

    @NotBlank(message = "deviceFingerprint cannot be blank")
    @Size(max = 128)
    private String deviceFingerprint;

    @NotBlank(message = "publicIdentityKey cannot be blank")
    private String publicIdentityKey;

    @Size(max = 64)
    private String deviceLabel;

    public CreatePairRequest() {}

    public CreatePairRequest(String deviceFingerprint, String publicIdentityKey, String deviceLabel) {
        this.deviceFingerprint = deviceFingerprint;
        this.publicIdentityKey = publicIdentityKey;
        this.deviceLabel = deviceLabel;
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
}
