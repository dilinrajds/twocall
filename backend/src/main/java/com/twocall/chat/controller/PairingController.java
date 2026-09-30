package com.twocall.chat.controller;

import com.twocall.chat.dto.request.CreatePairRequest;
import com.twocall.chat.dto.request.JoinPairRequest;
import com.twocall.chat.dto.response.CreatePairResponse;
import com.twocall.chat.dto.response.JoinPairResponse;
import com.twocall.chat.service.PairingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pair")
public class PairingController {

    private final PairingService pairingService;

    public PairingController(PairingService pairingService) {
        this.pairingService = pairingService;
    }

    @PostMapping("/create")
    public ResponseEntity<CreatePairResponse> createPair(@Valid @RequestBody CreatePairRequest request,
                                                         HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        CreatePairResponse response = pairingService.createPair(request, clientIp);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/join")
    public ResponseEntity<JoinPairResponse> joinPair(@Valid @RequestBody JoinPairRequest request,
                                                     HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        JoinPairResponse response = pairingService.joinPair(request, clientIp);
        return ResponseEntity.ok(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
