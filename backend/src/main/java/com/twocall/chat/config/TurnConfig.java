package com.twocall.chat.config;

import com.twocall.chat.dto.response.TurnCredentialsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Component
public class TurnConfig {

    private final String stunUrl;
    private final String turnUrl;
    private final String turnRealm;
    private final String turnSharedSecret;
    private final long ttlSeconds;

    public TurnConfig(
            @Value("${app.webrtc.stun-url:stun:stun.l.google.com:19302}") String stunUrl,
            @Value("${app.webrtc.turn-url:turn:localhost:3478}") String turnUrl,
            @Value("${app.webrtc.turn-realm:chatapp.local}") String turnRealm,
            @Value("${app.webrtc.turn-shared-secret:coturn_shared_secret_secure_key_2026}") String turnSharedSecret,
            @Value("${app.webrtc.turn-ttl-seconds:86400}") long ttlSeconds) {
        this.stunUrl = stunUrl;
        this.turnUrl = turnUrl;
        this.turnRealm = turnRealm;
        this.turnSharedSecret = turnSharedSecret;
        this.ttlSeconds = ttlSeconds;
    }

    public TurnCredentialsResponse generateCredentials(String deviceId) {
        long expiryEpoch = Instant.now().getEpochSecond() + ttlSeconds;
        String username = expiryEpoch + ":" + deviceId;
        String password = computeHmacSha1(username, turnSharedSecret);

        List<String> urls = List.of(
                stunUrl,
                turnUrl + "?transport=udp",
                turnUrl + "?transport=tcp"
        );

        return new TurnCredentialsResponse(username, password, ttlSeconds, urls);
    }

    private String computeHmacSha1(String data, String key) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate TURN HMAC credentials", e);
        }
    }

    public String getStunUrl() {
        return stunUrl;
    }
}
