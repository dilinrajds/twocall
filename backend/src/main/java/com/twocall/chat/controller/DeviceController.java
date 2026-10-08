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
    private final com.twocall.chat.service.ProfileService profileService;

    public DeviceController(PushNotificationService pushNotificationService, PairAccessValidator accessValidator,
                            com.twocall.chat.service.ProfileService profileService) {
        this.pushNotificationService = pushNotificationService;
        this.accessValidator = accessValidator;
        this.profileService = profileService;
    }

    @org.springframework.web.bind.annotation.GetMapping("/profile")
    public Map<String, Map<String, String>> profile() {
        var principal = accessValidator.getAuthenticatedPrincipal();
        return profileService.get(principal.getPairId(), principal.getDeviceId());
    }

    @org.springframework.web.bind.annotation.PutMapping("/profile")
    public Map<String, String> updateProfile(@Valid @RequestBody com.twocall.chat.dto.request.UpdateProfileRequest request) {
        var principal = accessValidator.getAuthenticatedPrincipal();
        profileService.update(principal.getPairId(), principal.getDeviceId(), request);
        return Map.of("message", "Profile updated");
    }

    @PostMapping("/push-token")
    public ResponseEntity<Map<String, String>> registerPushToken(@Valid @RequestBody RegisterPushTokenRequest request) {
        DevicePrincipal principal = accessValidator.getAuthenticatedPrincipal();
        pushNotificationService.registerPushToken(principal.getDeviceId(), request.getFcmToken());
        return ResponseEntity.ok(Map.of("message", "Push token registered"));
    }
}
