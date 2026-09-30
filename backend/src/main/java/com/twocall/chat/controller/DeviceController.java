package com.twocall.chat.controller;

import com.twocall.chat.dto.request.RegisterPushTokenRequest;
import com.twocall.chat.security.DevicePrincipal;
import com.twocall.chat.security.PairAccessValidator;
import com.twocall.chat.service.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/device")
public class DeviceController {

    private final PushNotificationService pushNotificationService;
    private final PairAccessValidator accessValidator;

    public DeviceController(PushNotificationService pushNotificationService, PairAccessValidator accessValidator) {
        this.pushNotificationService = pushNotificationService;
        this.accessValidator = accessValidator;
    }

    @PostMapping("/push-token")
    public ResponseEntity<Map<String, String>> registerPushToken(@Valid @RequestBody RegisterPushTokenRequest request) {
        DevicePrincipal principal = accessValidator.getAuthenticatedPrincipal();
        pushNotificationService.registerPushToken(principal.getDeviceId(), request.getFcmToken());
        return ResponseEntity.ok(Map.of("message", "Push token registered"));
    }
}
