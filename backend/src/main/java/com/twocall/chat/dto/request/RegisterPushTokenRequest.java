package com.twocall.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

public class RegisterPushTokenRequest {

    @NotBlank(message = "fcmToken cannot be blank")
    private String fcmToken;

    public RegisterPushTokenRequest() {}

    public RegisterPushTokenRequest(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
