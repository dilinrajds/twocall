# WebRTC Audio & Video Calling Architecture

TwoCall provides end-to-end peer-to-peer audio and video calling between the two paired devices.

---

## 1. Media Path vs Signaling Path

```
                    ┌───────────────────────────┐
                    │    Spring Boot Backend    │
                    │   (WebSocket Signaling)   │
                    └───────▲───────────▲───────┘
                            │           │
                 CALL_OFFER │           │ CALL_ANSWER
                 CANDIDATES │           │ CANDIDATES
                            │           │
     ┌──────────────────────▼───┐   ┌───▼──────────────────────┐
     │         Phone A          │   │         Phone B          │
     │  (Native WebRTC Android) │   │  (Native WebRTC Android) │
     └─────────────▲────────────┘   └────────────▲─────────────┘
                   │                             │
                   └─────────── Direct P2P ──────┘
                      (SRTP Audio / VP8/H.264 Video)
                                     OR
                   ┌─────────────────────────────┐
                   │    Coturn (TURN Relay)      │
                   │    (When NAT/Firewall P2P   │
                   │     traversal fails)        │
                   └─────────────────────────────┘
```

- **Audio & Video Media**: Never touches the Spring Boot backend. Transported directly via DTLS-SRTP.
- **Signaling**: Authenticated JSON messages over the existing persistent WebSocket connection.

---

## 2. Signaling Event Lifecycle

1. **User A presses "Audio Call" or "Video Call"**:
   - `WebRtcManager` on Phone A creates an `RTCPeerConnection` with STUN/TURN servers.
   - Phone A creates an SDP Offer and sends it through WebSocket:
     ```json
     {
       "eventType": "CALL_OFFER",
       "pairId": "UUID",
       "senderDeviceId": "UUID",
       "payload": {
         "callId": "UUID",
         "callType": "AUDIO | VIDEO",
         "sdp": "v=0\r\no=... (SDP content)"
       }
     }
     ```
2. **Backend routes offer**:
   - If Phone B is online on WebSocket: sends `CALL_OFFER` immediately.
   - If Phone B is backgrounded or screen locked: sends high-priority FCM notification with `callId` and `callType` to launch the incoming call activity and ringtone.
3. **User B presses "Accept"**:
   - Phone B initializes peer connection, applies Phone A's remote SDP offer, creates an SDP Answer, and sends it via WebSocket:
     ```json
     {
       "eventType": "CALL_ANSWER",
       "pairId": "UUID",
       "senderDeviceId": "UUID",
       "payload": {
         "callId": "UUID",
         "sdp": "v=0\r\no=... (SDP answer)"
       }
     }
     ```
4. **ICE Candidate Trickling**:
   - Both devices exchange ICE candidates as network interfaces are discovered:
     ```json
     {
       "eventType": "ICE_CANDIDATE",
       "payload": {
         "callId": "UUID",
         "candidate": "candidate:...",
         "sdpMid": "0",
         "sdpMLineIndex": 0
       }
     }
     ```
5. **Call Termination**:
   - Either user hanging up or rejecting sends `CALL_END` or `CALL_REJECT` which terminates media tracks and closes peer connections cleanly.

---

## 3. Coturn Ephemeral Credentials (RFC 5766)

To prevent hardcoded TURN credentials in the mobile APK:
1. Android app requests ephemeral TURN credentials from backend: `GET /api/v1/webrtc/turn-credentials`.
2. Backend computes:
   - `username = (epoch_now + ttl) + ":" + deviceId`
   - `password = Base64(HMAC-SHA1(username, turn_shared_secret))`
3. Coturn verifies the HMAC signature using the same shared secret. Credentials expire automatically when the time window passes.
