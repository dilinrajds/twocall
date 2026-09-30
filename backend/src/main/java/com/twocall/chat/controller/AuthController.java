package com.twocall.chat.controller;

import com.twocall.chat.dto.request.RefreshTokenRequest;
import com.twocall.chat.dto.response.DevicePairInfoResponse;
import com.twocall.chat.dto.response.TokenResponse;
import com.twocall.chat.security.DevicePrincipal;
import com.twocall.chat.security.PairAccessValidator;
import com.twocall.chat.service.DeviceAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final DeviceAuthService authService;
    private final PairAccessValidator accessValidator;

    public AuthController(DeviceAuthService authService, PairAccessValidator accessValidator) {
        this.authService = authService;
        this.accessValidator = accessValidator;
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pair-info")
    public ResponseEntity<DevicePairInfoResponse> getPairInfo() {
        DevicePrincipal principal = accessValidator.getAuthenticatedPrincipal();
        DevicePairInfoResponse response = authService.getPairInfo(principal.getDeviceId(), principal.getPairId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, String>> disconnectDevice() {
        DevicePrincipal principal = accessValidator.getAuthenticatedPrincipal();
        authService.disconnectDevice(principal.getDeviceId(), principal.getPairId());
        return ResponseEntity.ok(Map.of("message", "Device disconnected successfully"));
    }

    @DeleteMapping("/pair")
    public ResponseEntity<Map<String, String>> deletePair() {
        DevicePrincipal principal = accessValidator.getAuthenticatedPrincipal();
        authService.deletePair(principal.getPairId(), principal.getDeviceId());
        return ResponseEntity.ok(Map.of("message", "Pair and all data deleted permanently"));
    }
}
