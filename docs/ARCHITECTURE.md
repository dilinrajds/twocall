# TwoCall Architecture & System Design

TwoCall is an exclusive, private messaging and calling system built strictly for **two paired users**.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Android Device A                    │
│  - Jetpack Compose + Material 3 UI                      │
│  - Room Local DB (Cached Messages, Delivery State)      │
│  - Android Keystore & EncryptedSharedPreferences        │
│  - CryptoEngine (ECDH P-256, HKDF, AES-256-GCM)        │
│  - Native WebRTC (Audio/Video Tracks, SurfaceViews)     │
└──────────▲──────────────────▲─────────────────────▲─────┘
           │                  │                     │
      REST (HTTP)       WebSocket (WSS)         WebRTC (UDP/TCP)
           │                  │                     │
┌──────────▼──────────────────▼──────────┐          │ (Direct P2P
│          Spring Boot Backend           │          │  or Relay)
│  - Pair & Device Auth Services         │          │
│  - 6-Digit Single-Use Pairing Engine   │          │
│  - WebSocket Signaling Broker          │          │
│  - Ciphertext Message Storage & Sync   │          │
│  - Ephemeral HMAC-SHA1 TURN Tokens     │          │
│  - FCM Push Dispatcher                 │          │
└──────────▲──────────────────▲──────────┘          │
           │                  │                     │
    PostgreSQL DB        Firebase FCM               │
   (Flyway Migrations) (Wakeup & Alert)             │
           │                                        │
┌──────────▼────────────────────────────────────────▼─────┐
│                     Android Device B                    │
│  - Jetpack Compose + Material 3 UI                      │
│  - Room Local DB (Cached Messages, Delivery State)      │
│  - Android Keystore & EncryptedSharedPreferences        │
│  - CryptoEngine (ECDH P-256, HKDF, AES-256-GCM)        │
│  - Native WebRTC (Audio/Video Tracks, SurfaceViews)     │
└─────────────────────────────────────────────────────────┘
```

---

## 1. The Two-Partner Pairing Lifecycle

1. **User A opens app for first time**:
   - Chooses **"Create Pair"**.
   - Device generates an EC identity key pair (`secp256r1`) stored in hardware Android Keystore.
   - Device sends public key + device fingerprint to backend: `POST /api/v1/pair/create`.
   - Backend generates a random 6-digit number (e.g., `583214`), hashes it with SHA-256, stores the record with a **5-minute expiration** and a **max 5 attempts** threshold.
   - Backend generates initial device-bound JWT access and refresh tokens.
   - User A sees `583214` and a live 5-minute countdown.

2. **User B opens app for first time**:
   - Chooses **"Join Partner"**.
   - User B enters `583214`.
   - Device sends public key + code to backend: `POST /api/v1/pair/join`.
   - Backend verifies hash, expiration, attempt count, and confirms pair currently has only 1 device.
   - **Immediately invalidates pairing code** (`used = true`).
   - Pairs Device B into the pair (`pairId`), enforces **2-device limit** (no 3rd device can ever join).
   - Returns Partner A's public identity key to Device B.
   - Relays Device B's public identity key to Device A via active WebSocket/FCM.
   - Both devices derive the shared E2EE key via ECDH + HKDF.
   - Both devices transition automatically to the conversation screen!

---

## 2. Realtime WebSocket Protocol

All events travel as structured JSON DTOs:

```json
{
  "eventType": "MESSAGE_SENT | MESSAGE_DELIVERED | MESSAGE_READ | TYPING_START | TYPING_STOP | PRESENCE | CALL_OFFER | CALL_ANSWER | ICE_CANDIDATE | CALL_END | CALL_REJECT",
  "pairId": "UUID",
  "senderDeviceId": "UUID",
  "recipientDeviceId": "UUID",
  "timestamp": "2026-10-01T00:00:00Z",
  "payload": { ... }
}
```

- **Authentication**: WebSocket handshake is authenticated using JWT Bearer token in query string or header.
- **Pair Isolation**: Backend routes events strictly between the two devices assigned to that `pairId`.
- **Presence**: When WebSocket connects or disconnects, the partner immediately receives `PRESENCE` events (`online=true/false`).

---

## 3. Local Room Database & Offline Queue

- **Immediate Cache Display**: When app opens, Room loads cached messages instantly.
- **Offline Sending**: Messages composed while disconnected are stored in Room with status `PENDING`.
- **Automatic Sync**: As soon as network connectivity is restored, pending messages are dispatched and `/api/v1/messages/sync` reconciles any messages sent while offline.
- **Delivery Ticks**:
  - `PENDING`: Clock icon
  - `SENT`: Single checkmark
  - `DELIVERED`: Double grey checkmarks
  - `READ`: Double emerald checkmarks
