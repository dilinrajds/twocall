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
    public CallSession initiateCall(UUID pairId, UUID callerDeviceId, CallType callType, String sdpOffer) {
        Pair pair = pairRepository.findById(pairId)
                .orElseThrow(() -> new ResourceNotFoundException("Pair not found"));
        Device caller = deviceRepository.findById(callerDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        CallSession session = new CallSession(UUID.randomUUID(), pair, caller, callType);
        session = callSessionRepository.save(session);

        UUID partnerId = findPartnerDeviceId(pairId, callerDeviceId);
        if (partnerId != null) {
            WsSignalingPayload payload = WsSignalingPayload.offer(session.getId(), callType.name(), sdpOffer);
            WsEvent<WsSignalingPayload> event = new WsEvent<>(
                    WsEventType.CALL_OFFER,
                    pairId,
                    callerDeviceId,
                    partnerId,
                    payload
            );

            boolean deliveredOnline = wsSessionManager.sendToDevice(partnerId, event);
            if (!deliveredOnline) {
                // Partner offline/backgrounded -> wake device via high-priority FCM call notification
                pushNotificationService.sendIncomingCallNotification(partnerId, session.getId(), callType.name(), callerDeviceId);
            }
        }

        return session;
    }

    @Transactional
    public void handleAnswer(UUID pairId, UUID answeringDeviceId, UUID callId, String sdpAnswer) {
        CallSession session = callSessionRepository.findByIdAndPairId(callId, pairId)
                .orElseThrow(() -> new ResourceNotFoundException("Call session not found"));

        session.setStatus(CallStatus.CONNECTED);
        callSessionRepository.save(session);

        UUID callerDeviceId = session.getCallerDevice().getId();
        WsSignalingPayload payload = WsSignalingPayload.answer(callId, sdpAnswer);

        wsSessionManager.sendToDevice(callerDeviceId, new WsEvent<>(
                WsEventType.CALL_ANSWER,
                pairId,
                answeringDeviceId,
                callerDeviceId,
                payload
        ));
    }

    public void handleIceCandidate(UUID pairId, UUID senderDeviceId, UUID callId, String candidate, String sdpMid, int sdpMLineIndex) {
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
        }
    }

    public TurnCredentialsResponse getTurnCredentials(UUID deviceId) {
        return turnConfig.generateCredentials(deviceId.toString());
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
