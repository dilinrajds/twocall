package com.twocall.chat.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.twocall.chat.domain.entity.PushToken;
import com.twocall.chat.repository.PushTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushTokenRepository pushTokenRepository;
    private final com.twocall.chat.repository.DeviceRepository deviceRepository;

    public PushNotificationService(PushTokenRepository pushTokenRepository,
                                   com.twocall.chat.repository.DeviceRepository deviceRepository) {
        this.pushTokenRepository = pushTokenRepository;
        this.deviceRepository = deviceRepository;
    }

    public void registerPushToken(UUID deviceId, String fcmToken) {
        Optional<PushToken> existing = pushTokenRepository.findByDeviceId(deviceId);
        if (existing.isPresent()) {
            PushToken token = existing.get();
            token.setFcmToken(fcmToken);
            token.setUpdatedAt(java.time.Instant.now());
            pushTokenRepository.save(token);
        } else {
            com.twocall.chat.domain.entity.Device device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new com.twocall.chat.exception.ResourceNotFoundException("Device not found: " + deviceId));
            PushToken token = new PushToken(UUID.randomUUID(), device, fcmToken);
            pushTokenRepository.save(token);
        }
    }

    public void sendNewMessageNotification(UUID recipientDeviceId, UUID pairId, UUID messageId, UUID senderDeviceId, String senderLabel) {
        String title = (senderLabel != null && !senderLabel.isBlank()) ? senderLabel : "ZippyCall";
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("messageId", messageId.toString());
        data.put("pairId", pairId != null ? pairId.toString() : "");
        data.put("senderDeviceId", senderDeviceId != null ? senderDeviceId.toString() : "");
        data.put("senderLabel", title);
        data.put("type", "NEW_MESSAGE");

        sendPush(recipientDeviceId, title, "New encrypted message received", "Tap to open", data);
    }

    public void sendNewMessageNotification(UUID recipientDeviceId, UUID messageId) {
        sendNewMessageNotification(recipientDeviceId, null, messageId, null, null);
    }

    public void sendIncomingCallNotification(UUID recipientDeviceId, UUID pairId, UUID callId, String callType, UUID callerDeviceId, String callerLabel) {
        String callerName = (callerLabel != null && !callerLabel.isBlank()) ? callerLabel : "Partner";
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("callId", callId.toString());
        data.put("pairId", pairId != null ? pairId.toString() : "");
        data.put("callType", callType);
        data.put("callerDeviceId", callerDeviceId != null ? callerDeviceId.toString() : "");
        data.put("callerLabel", callerName);
        data.put("type", "INCOMING_CALL");

        sendPush(recipientDeviceId, callerName, "Incoming " + callType + " call", "Call from " + callerName, data);
    }

    public void sendIncomingCallNotification(UUID recipientDeviceId, UUID callId, String callType, UUID callerDeviceId) {
        sendIncomingCallNotification(recipientDeviceId, null, callId, callType, callerDeviceId, null);
    }

    public void sendMissedCallNotification(UUID recipientDeviceId, String callType) {
        sendPush(recipientDeviceId, "ZippyCall • Missed Call", "Missed " + callType + " call", "Missed call from partner",
                java.util.Map.of("type", "MISSED_CALL"));
    }

    public void sendCallEndedNotification(UUID recipientDeviceId, UUID callId) {
        sendPush(recipientDeviceId, "Call ended", "Call ended", "", java.util.Map.of(
                "type", "CALL_ENDED", "callId", callId.toString()));
    }

    private void sendPush(UUID recipientDeviceId, String title, String body, String alert, java.util.Map<String, String> data) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.debug("Push notification skipped: Firebase not configured. Recipient: {} Event: {}", recipientDeviceId, data.get("type"));
            return;
        }

        Optional<PushToken> tokenOpt = pushTokenRepository.findByDeviceId(recipientDeviceId);
        if (tokenOpt.isEmpty()) {
            log.debug("Push token not found for device {}", recipientDeviceId);
            return;
        }

        String fcmToken = tokenOpt.get().getFcmToken();
        try {
            // Use DATA-ONLY message (no notification payload).
            // This lets the Android client handle background notifications. Force-stopped
            // applications still require the user to open them. If a notification payload is
            // included, Android auto-displays it and does NOT invoke onMessageReceived().
            java.util.Map<String, String> fullData = new java.util.HashMap<>(data);
            fullData.put("title", title);
            fullData.put("body", body);

            Message message = Message.builder()
                    .setToken(fcmToken)
                    .putAllData(fullData)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setTtl("INCOMING_CALL".equals(data.get("type")) ? 60000L : 86400000L)
                            .build())
                    .build();

            FirebaseMessaging.getInstance().send(message);
            log.info("Push notification sent to device {} (type: {})", recipientDeviceId, data.get("type"));
        } catch (Exception e) {
            log.error("Failed to send push notification to device {}: {}", recipientDeviceId, e.getMessage());
        }
    }
}
