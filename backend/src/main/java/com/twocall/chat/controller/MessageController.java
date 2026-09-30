package com.twocall.chat.controller;

import com.twocall.chat.dto.request.ReactionRequest;
import com.twocall.chat.dto.request.ReceiptUpdateRequest;
import com.twocall.chat.dto.request.SendMessageRequest;
import com.twocall.chat.dto.response.MessageResponse;
import com.twocall.chat.security.DevicePrincipal;
import com.twocall.chat.security.PairAccessValidator;
import com.twocall.chat.service.MessagingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessagingService messagingService;
    private final PairAccessValidator accessValidator;

    public MessageController(MessagingService messagingService, PairAccessValidator accessValidator) {
        this.messagingService = messagingService;
        this.accessValidator = accessValidator;
    }

    @PostMapping("/send")
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        DevicePrincipal principal = accessValidator.getAuthenticatedPrincipal();
        MessageResponse response = messagingService.saveAndSendMessage(principal.getPairId(), principal.getDeviceId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sync")
    public ResponseEntity<List<MessageResponse>> syncMessages(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant after) {
        DevicePrincipal principal = accessValidator.getAuthenticatedPrincipal();
        List<MessageResponse> list = messagingService.syncMessages(principal.getPairId(), after);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/receipt")
    public ResponseEntity<Map<String, String>> updateReceipt(@Valid @RequestBody ReceiptUpdateRequest request) {
        DevicePrincipal principal = accessValidator.getAuthenticatedPrincipal();
        messagingService.updateDeliveryReceipt(principal.getPairId(), principal.getDeviceId(), request);
        return ResponseEntity.ok(Map.of("message", "Receipt updated"));
    }

    @PostMapping("/reaction")
    public ResponseEntity<Map<String, String>> addReaction(@Valid @RequestBody ReactionRequest request) {
        DevicePrincipal principal = accessValidator.getAuthenticatedPrincipal();
        messagingService.addOrUpdateReaction(principal.getPairId(), principal.getDeviceId(), request);
        return ResponseEntity.ok(Map.of("message", "Reaction recorded"));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Map<String, String>> deleteMessage(@PathVariable UUID messageId) {
        DevicePrincipal principal = accessValidator.getAuthenticatedPrincipal();
        messagingService.deleteMessage(principal.getPairId(), principal.getDeviceId(), messageId);
        return ResponseEntity.ok(Map.of("message", "Message deleted"));
    }
}
