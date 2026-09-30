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

    public PushNotificationService(PushTokenRepository pushTokenRepository) {
        this.pushTokenRepository = pushTokenRepository;
    }

    public void registerPushToken(UUID deviceId, String fcmToken) {
        Optional<PushToken> existing = pushTokenRepository.findByDeviceId(deviceId);
        if (existing.isPresent()) {
            PushToken token = existing.get();
            token.setFcmToken(fcmToken);
            token.setUpdatedAt(java.time.Instant.now());
            pushTokenRepository.save(token);
        } else {
            PushToken token = new PushToken(UUID.randomUUID(), null, fcmToken);
            // Device reference will be set in DeviceAuthService or caller
            pushTokenRepository.save(token);
        }
    }

    public void sendNewMessageNotification(UUID recipientDeviceId, UUID messageId) {
        sendPush(recipientDeviceId, "NEW_MESSAGE", "New encrypted message received", "Tap to open",
                java.util.Map.of("messageId", messageId.toString(), "type", "NEW_MESSAGE"));
    }

    public void sendIncomingCallNotification(UUID recipientDeviceId, UUID callId, String callType, UUID callerDeviceId) {
        sendPush(recipientDeviceId, "INCOMING_CALL", "Incoming " + callType + " call", "Call from partner",
                java.util.Map.of(
                        "callId", callId.toString(),
                        "callType", callType,
                        "callerDeviceId", callerDeviceId.toString(),
                        "type", "INCOMING_CALL"
                ));
    }

    public void sendMissedCallNotification(UUID recipientDeviceId, String callType) {
        sendPush(recipientDeviceId, "MISSED_CALL", "Missed " + callType + " call", "Missed call from partner",
                java.util.Map.of("type", "MISSED_CALL"));
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
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .putAllData(data)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .setChannelId("calls".equals(data.get("type")) ? "call_channel" : "message_channel")
                                    .build())
                            .build())
                    .build();

            FirebaseMessaging.getInstance().send(message);
            log.info("Push notification sent to device {} (type: {})", recipientDeviceId, data.get("type"));
        } catch (Exception e) {
            log.error("Failed to send push notification to device {}: {}", recipientDeviceId, e.getMessage());
        }
    }
}
