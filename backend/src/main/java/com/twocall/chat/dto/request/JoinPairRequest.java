package com.twocall.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class JoinPairRequest {

    @NotBlank(message = "code cannot be blank")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pairing code must be exactly 6 digits")
    private String code;

    @NotBlank(message = "deviceFingerprint cannot be blank")
    @Size(max = 128)
    private String deviceFingerprint;

    @NotBlank(message = "publicIdentityKey cannot be blank")
    private String publicIdentityKey;

    @Size(max = 64)
    private String deviceLabel;

    public JoinPairRequest() {}

    public JoinPairRequest(String code, String deviceFingerprint, String publicIdentityKey, String deviceLabel) {
        this.code = code;
        this.deviceFingerprint = deviceFingerprint;
        this.publicIdentityKey = publicIdentityKey;
        this.deviceLabel = deviceLabel;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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
