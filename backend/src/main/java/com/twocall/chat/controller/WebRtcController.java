package com.twocall.chat.controller;

import com.twocall.chat.dto.response.TurnCredentialsResponse;
import com.twocall.chat.security.DevicePrincipal;
import com.twocall.chat.security.PairAccessValidator;
import com.twocall.chat.service.WebRtcSignalingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webrtc")
public class WebRtcController {

    private final WebRtcSignalingService signalingService;
    private final PairAccessValidator accessValidator;

    public WebRtcController(WebRtcSignalingService signalingService, PairAccessValidator accessValidator) {
        this.signalingService = signalingService;
        this.accessValidator = accessValidator;
    }

    @GetMapping("/turn-credentials")
    public ResponseEntity<TurnCredentialsResponse> getTurnCredentials() {
        DevicePrincipal principal = accessValidator.getAuthenticatedPrincipal();
        TurnCredentialsResponse response = signalingService.getTurnCredentials(principal.getDeviceId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/calls/{callId}/incoming")
    public java.util.Map<String, Object> incoming(@org.springframework.web.bind.annotation.PathVariable java.util.UUID callId) {
        DevicePrincipal principal = accessValidator.getAuthenticatedPrincipal();
        return signalingService.recoverIncomingCall(principal.getPairId(), principal.getDeviceId(), callId);
    }

    @org.springframework.web.bind.annotation.PostMapping("/calls/{callId}/reject")
    public java.util.Map<String, String> reject(@org.springframework.web.bind.annotation.PathVariable java.util.UUID callId) {
        DevicePrincipal principal = accessValidator.getAuthenticatedPrincipal();
        signalingService.recoverIncomingCall(principal.getPairId(), principal.getDeviceId(), callId);
        signalingService.endCall(principal.getPairId(), principal.getDeviceId(), callId, "REJECTED");
        return java.util.Map.of("message", "Call rejected");
    }
}
