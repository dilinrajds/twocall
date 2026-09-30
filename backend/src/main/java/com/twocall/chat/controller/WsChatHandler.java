package com.twocall.chat.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twocall.chat.domain.enums.CallType;
import com.twocall.chat.domain.enums.WsEventType;
import com.twocall.chat.dto.ws.WsEvent;
import com.twocall.chat.dto.ws.WsPresencePayload;
import com.twocall.chat.dto.ws.WsSignalingPayload;
import com.twocall.chat.dto.ws.WsTypingPayload;
import com.twocall.chat.service.WebRtcSignalingService;
import com.twocall.chat.service.WsSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class WsChatHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WsChatHandler.class);

    private final ObjectMapper objectMapper;
    private final WsSessionManager sessionManager;
    private final WebRtcSignalingService signalingService;

    public WsChatHandler(ObjectMapper objectMapper, WsSessionManager sessionManager, WebRtcSignalingService signalingService) {
        this.objectMapper = objectMapper;
        this.sessionManager = sessionManager;
        this.signalingService = signalingService;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        UUID deviceId = (UUID) session.getAttributes().get("deviceId");
        UUID pairId = (UUID) session.getAttributes().get("pairId");

        if (deviceId != null && pairId != null) {
            sessionManager.registerSession(pairId, deviceId, session);

            // Notify partner that this device came online
            sessionManager.sendToPartner(pairId, deviceId, new WsEvent<>(
                    WsEventType.PRESENCE,
                    pairId,
                    deviceId,
                    null,
                    new WsPresencePayload(true, Instant.now())
            ));
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        UUID deviceId = (UUID) session.getAttributes().get("deviceId");
        UUID pairId = (UUID) session.getAttributes().get("pairId");

        if (deviceId == null || pairId == null) {
            return;
        }

        try {
            Map<String, Object> rawMap = objectMapper.readValue(message.getPayload(), new TypeReference<>() {});
            String eventTypeStr = (String) rawMap.get("eventType");
            if (eventTypeStr == null) return;

            WsEventType eventType = WsEventType.valueOf(eventTypeStr);

            switch (eventType) {
                case TYPING_START -> {
                    sessionManager.sendToPartner(pairId, deviceId, new WsEvent<>(
                            WsEventType.TYPING_START, pairId, deviceId, null, new WsTypingPayload(true)
                    ));
                }
                case TYPING_STOP -> {
                    sessionManager.sendToPartner(pairId, deviceId, new WsEvent<>(
                            WsEventType.TYPING_STOP, pairId, deviceId, null, new WsTypingPayload(false)
                    ));
                }
                case CALL_OFFER -> {
                    String payloadJson = objectMapper.writeValueAsString(rawMap.get("payload"));
                    WsSignalingPayload payload = objectMapper.readValue(payloadJson, WsSignalingPayload.class);
                    CallType callType = "VIDEO".equalsIgnoreCase(payload.getCallType()) ? CallType.VIDEO : CallType.AUDIO;
                    signalingService.initiateCall(pairId, deviceId, callType, payload.getSdp());
                }
                case CALL_ANSWER -> {
                    String payloadJson = objectMapper.writeValueAsString(rawMap.get("payload"));
                    WsSignalingPayload payload = objectMapper.readValue(payloadJson, WsSignalingPayload.class);
                    signalingService.handleAnswer(pairId, deviceId, payload.getCallId(), payload.getSdp());
                }
                case ICE_CANDIDATE -> {
                    String payloadJson = objectMapper.writeValueAsString(rawMap.get("payload"));
                    WsSignalingPayload payload = objectMapper.readValue(payloadJson, WsSignalingPayload.class);
                    int mLineIndex = payload.getSdpMLineIndex() != null ? payload.getSdpMLineIndex() : 0;
                    signalingService.handleIceCandidate(pairId, deviceId, payload.getCallId(), payload.getCandidate(), payload.getSdpMid(), mLineIndex);
                }
                case CALL_END, CALL_REJECT -> {
                    String payloadJson = objectMapper.writeValueAsString(rawMap.get("payload"));
                    WsSignalingPayload payload = objectMapper.readValue(payloadJson, WsSignalingPayload.class);
                    signalingService.endCall(pairId, deviceId, payload.getCallId(), payload.getReason() != null ? payload.getReason() : "ENDED");
                }
                default -> log.debug("Unhandled WS event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing WebSocket message from device {}", deviceId, e);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        UUID deviceId = (UUID) session.getAttributes().get("deviceId");
        UUID pairId = (UUID) session.getAttributes().get("pairId");

        if (deviceId != null && pairId != null) {
            sessionManager.removeSession(pairId, deviceId);

            // Notify partner that this device went offline
            sessionManager.sendToPartner(pairId, deviceId, new WsEvent<>(
                    WsEventType.PRESENCE,
                    pairId,
                    deviceId,
                    null,
                    new WsPresencePayload(false, Instant.now())
            ));
        }
    }
}
