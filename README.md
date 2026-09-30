# TwoCall — Private Two-Partner Messenger & WebRTC Calling App

[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen)]()
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Spring%20Boot-blue)]()
[![Security](https://img.shields.io/badge/E2EE-ECDH%20P--256%20%2B%20AES--256--GCM-success)]()

**TwoCall** is a private, minimalist Android messaging and voice/video calling application built exclusively for communication between **TWO paired users** (e.g., partners/couples).

### Core Highlights:
- 🚫 **Zero Account Registration**: No phone numbers, SMS, OTPs, emails, passwords, usernames, or public discovery.
- 🔑 **Cryptographic 6-Digit Pairing**: Secure, ephemeral, single-use 6-digit code with server-side rate-limiting and 5-minute auto-expiry.
- 🛡️ **Hardware-Backed Device Identity**: Generates EC key pairs in `AndroidKeyStore`; stores device tokens in `EncryptedSharedPreferences`.
- 🔒 **Zero-Knowledge Backend / E2EE**: All message contents, voice notes, and media metadata are end-to-end encrypted on device using **NIST P-256 ECDH** and **AES-256-GCM**. The backend only stores ciphertext and IVs.
- 👥 **Strict Two-Device Enforcement**: Exactly two devices per pair. A third device is rejected at both database and service layers.
- 💬 **Rich Realtime Chat**: Offline queue, automatic delivery & read receipts, live typing indicators, online presence, message replies, emoji reactions, and message deletion.
- 🎤 **Voice Messaging**: Hold/press to record AAC audio with live waveform amplitude, audio preview, and playback bar.
- 📞 **Native WebRTC Audio & Video Calling**: One-to-one P2P calling with camera switcher, video toggle, mic mute, speaker control, SurfaceViewRenderer video streaming, and Coturn STUN/TURN integration.
- 🔔 **Privacy-Preserving FCM Push**: Wakeup notifications with metadata only—no plaintext is ever leaked in push payloads.

---

## Project Structure

```
d:/ChatApp/
├── android-app/                 # Native Android Application (Kotlin, Jetpack Compose, Material 3, Room, WebRTC)
│   ├── app/src/main/
│   │   ├── java/com/twocall/chat/
│   │   │   ├── crypto/         # ECDH P-256, AES-256-GCM, KeyStoreManager
│   │   │   ├── data/           # Room DB, Retrofit API, WebSocket client, Repository
│   │   │   ├── webrtc/         # WebRtcManager, PeerConnection, CallService
│   │   │   ├── audio/          # VoiceRecorder, VoicePlayer
│   │   │   ├── fcm/            # ChatFirebaseMessagingService
│   │   │   └── ui/             # Jetpack Compose Screens, Components, ViewModels, Theme
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
│
├── backend/                     # Spring Boot 3.3 Backend (Java 21, Spring Security, WebSocket, JPA, Flyway)
│   ├── src/main/java/com/twocall/chat/
│   │   ├── config/             # Security, WebSocket, RateLimiting, TurnConfig, Firebase
│   │   ├── domain/             # JPA Entities (Pair, Device, Message, Attachment, CallSession)
│   │   ├── repository/         # Spring Data Repositories
│   │   ├── service/            # PairingService, MessagingService, WebRtcSignalingService
│   │   └── controller/         # REST Controllers & WebSocket Handler
│   ├── src/main/resources/     # application.yml, Flyway V1 schema migration
│   └── build.gradle
│
├── infra/                       # Docker infrastructure
│   ├── docker-compose.yml       # PostgreSQL 16 & Coturn TURN container
│   ├── turnserver.conf          # Coturn configuration with HMAC-SHA1 secret
│   └── init-db.sql              # Database extensions
│
├── docs/                        # In-depth technical guides
│   ├── ARCHITECTURE.md          # System design & protocol specification
│   ├── SECURITY_AND_E2EE.md     # Cryptographic primitives, threat model & limitations
│   ├── WEBRTC_SIGNALING.md      # WebRTC signaling lifecycle & TURN tokens
│   └── DEPLOYMENT.md            # Local Docker setup & VPS deployment guide
│
├── .env.example                 # Environment configuration template
└── README.md                    # Root project documentation
```

---

## Quick Start (Local Development)

### 1. Requirements:
- Windows 10/11 or Linux / macOS
- **Java 21 LTS** (Installed at `C:\Program Files\Eclipse Adoptium\jdk-21...`)
- **Gradle 8.10+** (Installed at `D:\tools\gradle-8.10.2`)
- **Android SDK 34** (Installed at `D:\tools\AndroidSdk`)
- **Docker Desktop** (for PostgreSQL & Coturn)

---

### 2. Start PostgreSQL & Coturn Containers
```powershell
cd d:\ChatApp\infra
docker compose up -d
```
This starts:
- PostgreSQL on `localhost:5432` (`chatapp_db`)
- Coturn TURN/STUN on `localhost:3478`

---

### 3. Run the Backend
```powershell
cd d:\ChatApp\backend
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;D:\tools\gradle-8.10.2\bin;$env:Path"

# Run tests
gradle test

# Start the Spring Boot application
gradle bootRun
```
The backend initializes Flyway schema migrations and listens on `http://localhost:8080`.

---

### 4. Build and Run the Android App

To test pairing and conversation between two users:

#### In Android Studio:
1. Open `d:\ChatApp\android-app` in Android Studio.
2. Launch **Emulator 1** (Phone A):
   - Choose **"1. Create Pair"** -> App displays 6-digit code (e.g. `583214`) and starts 5-minute countdown.
3. Launch **Emulator 2** (Phone B):
   - Choose **"2. Join Partner"** -> Enter `583214`.
4. Both devices connect immediately, verify ECDH public keys, and open the private conversation screen.

#### Via Command Line (Gradle Debug APK):
```powershell
cd d:\ChatApp\android-app
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:ANDROID_HOME = "D:\tools\AndroidSdk"
$env:Path = "$env:JAVA_HOME\bin;D:\tools\gradle-8.10.2\bin;$env:Path"

gradle assembleDebug
```
The APK is generated at:
`d:\ChatApp\android-app\app\build\outputs\apk\debug\app-debug.apk`

---

## End-to-End Encryption Flow

```
[ Sender Device ]                                      [ Server ]                      [ Receiver Device ]
       │                                                   │                                    │
  Generate Message                                         │                                    │
       │                                                   │                                    │
  AES-256-GCM Encrypt                                      │                                    │
  (Key: ECDH shared secret)                                │                                    │
       │                                                   │                                    │
  POST Ciphertext + IV ────────────────────────────► Store Ciphertext                           │
                                                    (Zero Plaintext)                            │
                                                           │                                    │
                                                WebSocket / FCM Push ─────────────────► Receive Ciphertext
                                                                                                │
                                                                                        AES-256-GCM Decrypt
                                                                                        (Key: ECDH shared secret)
                                                                                                │
                                                                                        Render in Compose UI
```

---

## WebRTC Audio & Video Calling

- **Signaling**: JSON messages (`CALL_OFFER`, `CALL_ANSWER`, `ICE_CANDIDATE`, `CALL_END`, `CALL_REJECT`) exchange SDP descriptors via the authenticated WebSocket.
- **Audio/Video Media**: Streams directly P2P using SRTP over UDP. If direct P2P connection fails due to symmetric NAT/firewalls, media automatically relays through the Coturn TURN server.
- **TURN Credentials**: Dynamically issued via `GET /api/v1/webrtc/turn-credentials` using time-windowed HMAC-SHA1 tokens. No static passwords are ever embedded in the Android APK.

---

## Firebase Cloud Messaging (FCM) Setup

1. Create a project in [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name `com.twocall.chat`.
3. Download `google-services.json` and place it in `android-app/app/`.
4. Download service account private key from **Project Settings > Service Accounts**, and place it in `backend/config/firebase-service-account.json`.

*(Note: During local development, if Firebase credentials are omitted, the backend runs in fallback mode and delivers real-time notifications via WebSocket when devices are online).*

---

## Troubleshooting

| Issue | Cause | Solution |
| :--- | :--- | :--- |
| `Connection Refused` on Android | Emulator using localhost | Use `10.0.2.2` (Android emulator's alias for host loopback) or your PC's LAN IP (`192.168.x.x`). |
| `Pair Limit Exceeded` (HTTP 403) | A 3rd device tried to join | TwoCall strictly enforces 2 devices per pair. To re-pair, choose "Delete Pair" in Settings. |
| `Pairing Code Expired` (HTTP 410) | 5 minutes passed | Create a fresh pair from Phone A to generate a new 6-digit code. |
| `Rate Limit Exceeded` (HTTP 429) | > 10 attempts in 1 min | Wait 60 seconds before submitting a new pairing attempt. |

---

## Documentation Links
- [Complete Architecture & Data Flow](file:///d:/ChatApp/docs/ARCHITECTURE.md)
- [Security Model & Cryptographic Primitives](file:///d:/ChatApp/docs/SECURITY_AND_E2EE.md)
- [WebRTC Signaling & Coturn Setup](file:///d:/ChatApp/docs/WEBRTC_SIGNALING.md)
- [Production VPS Deployment Guide](file:///d:/ChatApp/docs/DEPLOYMENT.md)
