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
}
