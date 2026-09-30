# Deployment & Operations Guide

This guide describes how to run TwoCall locally for development and how to deploy it to a production VPS.

---

## 1. Local Development Setup (Windows / Docker)

### Prerequisites:
- Java 21 LTS (Eclipse Temurin / OpenJDK)
- Docker Desktop or Docker Engine
- Android Studio with Android 14 SDK (API 34)

### Step 1: Start PostgreSQL and Coturn TURN Server
In PowerShell, navigate to the `infra` directory:
```powershell
cd d:\ChatApp\infra
docker compose up -d
```
Verify containers are healthy:
```powershell
docker compose ps
```

### Step 2: Start Spring Boot Backend
In PowerShell, navigate to the `backend` directory:
```powershell
cd d:\ChatApp\backend
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;D:\tools\gradle-8.10.2\bin;$env:Path"
gradle bootRun
```
The backend starts on `http://localhost:8080`.

---

## 2. Testing with Two Android Devices / Emulators

To simulate two partners communicating:

1. **Option A: Two Android Emulators (on same PC)**:
   - Emulator 1: Connects to `http://10.0.2.2:8080/`
   - Emulator 2: Connects to `http://10.0.2.2:8080/`
   - Open App on Emulator 1 -> tap **"1. Create Pair"** -> see 6-digit code (e.g. `583214`).
   - Open App on Emulator 2 -> tap **"2. Join Partner"** -> type `583214`.
   - Both devices immediately establish E2EE and transition to the chat conversation!

2. **Option B: Two Physical Android Phones (via Local Wi-Fi)**:
   - Find your PC's local IP address (e.g. `192.168.1.50` via `ipconfig`).
   - In `ApiClient.kt`, set `baseUrl = "http://192.168.1.50:8080/"`.
   - Build and install the APK on both phones:
     ```powershell
     cd d:\ChatApp\android-app
     gradle assembleDebug
     ```
   - Connect Phone A and Phone B over Wi-Fi, execute the 6-digit pairing flow, and begin chatting and calling.

---

## 3. Production VPS Deployment

### Architecture:
- Ubuntu 22.04 LTS or 24.04 LTS
- Domain name (e.g. `chat.yourdomain.com`) with Let's Encrypt SSL
- Nginx reverse proxy terminating TLS (port 443) and forwarding to Spring Boot (port 8080) with WebSocket upgrade headers
- PostgreSQL 16
- Coturn TURN server listening on port 3478 with UDP relay ports 49152-49200

### Coturn Setup on Ubuntu VPS:
```bash
sudo apt update && sudo apt install -y coturn
sudo nano /etc/turnserver.conf
```
Add:
```ini
listening-port=3478
tls-listening-port=5349
realm=yourdomain.com
use-auth-secret
static-auth-secret=YOUR_LONG_SECURE_COTURN_SECRET
external-ip=YOUR_PUBLIC_VPS_IP
min-port=49152
max-port=49200
fingerprint
lt-cred-mech
```
Restart coturn:
```bash
sudo systemctl restart coturn
```

### Nginx Reverse Proxy Configuration:
```nginx
server {
    server_name chat.yourdomain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 55M;
    }

    listen 443 ssl;
    ssl_certificate /etc/letsencrypt/live/chat.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/chat.yourdomain.com/privkey.pem;
}
```

### Systemd Backend Service (`/etc/systemd/system/twocall.service`):
```ini
[Unit]
Description=TwoCall Spring Boot Backend
After=network.target postgresql.service

[Service]
User=twocall
WorkingDirectory=/opt/twocall/backend
ExecStart=/usr/bin/java -jar /opt/twocall/backend/chatapp-backend-1.0.0-SNAPSHOT.jar
EnvironmentFile=/opt/twocall/.env
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now twocall
```
