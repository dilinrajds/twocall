package com.twocall.chat.service;

import com.twocall.chat.config.TurnConfig;
import com.twocall.chat.domain.entity.CallSession;
import com.twocall.chat.domain.entity.Device;
import com.twocall.chat.domain.entity.Pair;
import com.twocall.chat.domain.enums.CallStatus;
import com.twocall.chat.domain.enums.CallType;
import com.twocall.chat.domain.enums.WsEventType;
import com.twocall.chat.dto.response.TurnCredentialsResponse;
import com.twocall.chat.dto.ws.WsEvent;
import com.twocall.chat.dto.ws.WsSignalingPayload;
import com.twocall.chat.exception.ResourceNotFoundException;
import com.twocall.chat.repository.CallSessionRepository;
import com.twocall.chat.repository.DeviceRepository;
import com.twocall.chat.repository.PairRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WebRtcSignalingService {

    private static final Logger log = LoggerFactory.getLogger(WebRtcSignalingService.class);

    private final CallSessionRepository callSessionRepository;
    private final PairRepository pairRepository;
    private final DeviceRepository deviceRepository;
    private final WsSessionManager wsSessionManager;
    private final PushNotificationService pushNotificationService;
    private final TurnConfig turnConfig;

    public WebRtcSignalingService(
            CallSessionRepository callSessionRepository,
            PairRepository pairRepository,
            DeviceRepository deviceRepository,
            WsSessionManager wsSessionManager,
            PushNotificationService pushNotificationService,
            TurnConfig turnConfig) {
        this.callSessionRepository = callSessionRepository;
        this.pairRepository = pairRepository;
        this.deviceRepository = deviceRepository;
        this.wsSessionManager = wsSessionManager;
        this.pushNotificationService = pushNotificationService;
        this.turnConfig = turnConfig;
    }

    @Transactional
    public CallSession initiateCall(UUID pairId, UUID callerDeviceId, UUID callId, CallType callType, String sdpOffer) {
        Pair pair = pairRepository.findById(pairId)
                .orElseThrow(() -> new ResourceNotFoundException("Pair not found"));
        Device caller = deviceRepository.findById(callerDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        UUID sessionId = (callId != null) ? callId : UUID.randomUUID();
        CallSession session = new CallSession(sessionId, pair, caller, callType);
        session.setOfferSdp(sdpOffer);
        session.setIceCandidates("[]");
        session = callSessionRepository.save(session);

        UUID partnerId = findPartnerDeviceId(pairId, callerDeviceId);
        if (partnerId != null) {
            WsSignalingPayload payload = WsSignalingPayload.offer(sessionId, callType.name(), sdpOffer);
            WsEvent<WsSignalingPayload> event = new WsEvent<>(
                    WsEventType.CALL_OFFER,
                    pairId,
                    callerDeviceId,
                    partnerId,
                    payload
            );

            wsSessionManager.sendToDevice(partnerId, event);
            // Always dispatch FCM push notification as well, so incoming call rings even if app is in background/asleep
            pushNotificationService.sendIncomingCallNotification(
                    partnerId,
                    pairId,
                    sessionId,
                    callType.name(),
                    callerDeviceId,
                    caller.getDeviceLabel()
            );
        }

        return session;
    }

    @Transactional
    public void handleAnswer(UUID pairId, UUID answeringDeviceId, UUID callId, String sdpAnswer) {
        if (callId != null) {
            callSessionRepository.findByIdAndPairId(callId, pairId).ifPresent(session -> {
                session.setStatus(CallStatus.CONNECTED);
                callSessionRepository.save(session);
            });
        }

        UUID callerDeviceId = findPartnerDeviceId(pairId, answeringDeviceId);
        if (callerDeviceId != null) {
            WsSignalingPayload payload = WsSignalingPayload.answer(callId, sdpAnswer);

            wsSessionManager.sendToDevice(callerDeviceId, new WsEvent<>(
                    WsEventType.CALL_ANSWER,
                    pairId,
                    answeringDeviceId,
                    callerDeviceId,
                    payload
            ));
            log.info("CALL_ANSWER delivered from {} to caller {}", answeringDeviceId, callerDeviceId);
            pushNotificationService.sendCallEndedNotification(answeringDeviceId, callId);
        }
    }

    @Transactional
    public void handleIceCandidate(UUID pairId, UUID senderDeviceId, UUID callId, String candidate, String sdpMid, int sdpMLineIndex) {
        callSessionRepository.findLockedByIdAndPairId(callId, pairId).ifPresent(call -> {
            if (call.getStatus() == CallStatus.RINGING && call.getCallerDevice().getId().equals(senderDeviceId)) {
                try {
                    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    var candidates = (com.fasterxml.jackson.databind.node.ArrayNode) mapper.readTree(
                            call.getIceCandidates() == null ? "[]" : call.getIceCandidates());
                    if (candidates.size() < 100 && candidate.length() < 4096) {
                        candidates.add(mapper.valueToTree(java.util.Map.of("candidate", candidate,
                                "sdpMid", sdpMid == null ? "" : sdpMid, "sdpMLineIndex", sdpMLineIndex)));
                        call.setIceCandidates(candidates.toString());
                        callSessionRepository.save(call);
                    }
                } catch (java.io.IOException e) { throw new IllegalStateException("Invalid stored ICE candidates", e); }
            }
        });
        UUID partnerId = findPartnerDeviceId(pairId, senderDeviceId);
        if (partnerId != null) {
            WsSignalingPayload payload = WsSignalingPayload.iceCandidate(callId, candidate, sdpMid, sdpMLineIndex);
            wsSessionManager.sendToDevice(partnerId, new WsEvent<>(
                    WsEventType.ICE_CANDIDATE,
                    pairId,
                    senderDeviceId,
                    partnerId,
                    payload
            ));
        }
    }

    @Transactional
    public void endCall(UUID pairId, UUID deviceId, UUID callId, String reason) {
        callSessionRepository.findByIdAndPairId(callId, pairId).ifPresent(session -> {
            session.setStatus("REJECTED".equalsIgnoreCase(reason) ? CallStatus.REJECTED : CallStatus.ENDED);
            session.setEndedAt(Instant.now());
            callSessionRepository.save(session);
        });

        UUID partnerId = findPartnerDeviceId(pairId, deviceId);
        if (partnerId != null) {
            WsSignalingPayload payload = WsSignalingPayload.end(callId, reason);
            WsEventType type = "REJECTED".equalsIgnoreCase(reason) ? WsEventType.CALL_REJECT : WsEventType.CALL_END;

            wsSessionManager.sendToDevice(partnerId, new WsEvent<>(
                    type,
                    pairId,
                    deviceId,
                    partnerId,
                    payload
            ));
            pushNotificationService.sendCallEndedNotification(partnerId, callId);
        }
    }

    public TurnCredentialsResponse getTurnCredentials(UUID deviceId) {
        return turnConfig.generateCredentials(deviceId.toString());
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> recoverIncomingCall(UUID pairId, UUID deviceId, UUID callId) {
        CallSession call = callSessionRepository.findByIdAndPairId(callId, pairId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found"));
        if (call.getCallerDevice().getId().equals(deviceId) || call.getStatus() != CallStatus.RINGING
                || call.getStartedAt().isBefore(Instant.now().minusSeconds(60))) {
            throw new ResourceNotFoundException("Call is no longer ringing");
        }
        try {
            return java.util.Map.of("callId", call.getId(), "pairId", pairId,
                    "callerDeviceId", call.getCallerDevice().getId(), "callType", call.getCallType(),
                    "sdp", call.getOfferSdp() == null ? "" : call.getOfferSdp(), "iceCandidates",
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(call.getIceCandidates() == null ? "[]" : call.getIceCandidates()));
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }

    private UUID findPartnerDeviceId(UUID pairId, UUID myDeviceId) {
        List<Device> devices = deviceRepository.findAllByPairId(pairId);
        for (Device d : devices) {
            if (!d.getId().equals(myDeviceId)) {
                return d.getId();
            }
        }
        return null;
    }
}
