# Security Architecture & End-to-End Encryption (E2EE) Specification

TwoCall is designed under the principle of **zero-knowledge plaintext storage**: the backend infrastructure never possesses the private cryptographic keys or the plaintext content of messages.

---

## 1. Cryptographic Primitives

TwoCall relies on established, standard cryptographic primitives provided natively by the Android platform and Java Cryptography Architecture (JCA):

1. **Key Agreement**: **ECDH (Elliptic Curve Diffie-Hellman)** over NIST curve **P-256 (`secp256r1`)**.
   - Keys are generated directly on each device.
   - Private keys are stored in `AndroidKeyStore` and `EncryptedSharedPreferences` backed by hardware security modules (TEE / StrongBox where available).
   - Private keys **never leave the device**.
2. **Key Derivation**: **HKDF-SHA256 (RFC 5869)**.
   - Derives a 256-bit symmetric encryption key from the ECDH shared secret using domain-separated context information (`TwoCall-E2EE-v1`).
3. **Authenticated Encryption**: **AES-256-GCM**.
   - Cryptographically random 12-byte initialization vector (IV / Nonce) generated per message via `SecureRandom`.
   - 128-bit authentication tag ensuring confidentiality, authenticity, and integrity.
   - Any tampering with ciphertext on the wire or in the database causes immediate `AEADBadTagException` rejection on the receiving device.
4. **Safety Fingerprint Verification**:
   - SHA-256 hash of the partner's public key formatted as readable hexadecimal octets.
   - Users can compare fingerprints in the Settings screen to verify protection against active MitM attacks.

---

## 2. Threat Model & Security Boundaries

### What the Server Sees:
- `pair_id` (UUID)
- `device_id` (UUID)
- `ciphertext_payload` (Base64-encoded encrypted bytes)
- `iv` (12-byte Base64 initialization vector)
- `message_type` (TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT)
- `created_at` timestamp
- Message delivery receipts (`DELIVERED`, `READ`)
- Encrypted file sizes and MIME types

### What the Server CANNOT See:
- **Plaintext message content**: The server cannot decrypt any text, voice note, photo, or document.
- **Private keys**: Private keys are generated and retained exclusively in device hardware storage.
- **Audio/Video call streams**: Audio and video call media travel directly P2P via WebRTC or via Coturn TURN relay using DTLS-SRTP encryption. The Spring Boot backend only relays signaling SDP offers/answers.

### Explicit Limitations & Honest Assessment:
- TwoCall uses **static-ephemeral/pairwise ECDH + AES-256-GCM**.
- **Important**: This implementation does **NOT** currently implement the full Signal Double-Ratchet protocol with pre-keys (X3DH). If a device's long-term identity key were compromised in the future, past messages could theoretically be decrypted if their ciphertexts were intercepted and recorded.
- We do **not** claim full Signal-grade forward secrecy across asynchronous offline ratchets, but rather robust pairwise authenticated symmetric encryption where the server is an untrusted ciphertext relay.

---

## 3. Data Deletion & Privacy Policy

When a user deletes a message or permanently deletes the pair:
1. **Single Message Deletion ("Delete for Everyone")**:
   - Server marks `is_deleted = true` and overwrites `ciphertext_payload` with an empty string.
   - Partner's local Room database marks the message deleted and clears the decrypted plaintext cache.
2. **Permanent Pair Deletion ("Delete Pair")**:
   - PostgreSQL cascades delete all records for that `pair_id`:
     - All device records
     - All sessions and refresh tokens
     - All messages and reactions
     - All delivery receipts
     - All attachments metadata
   - Physical media directory for that pair (`/uploads/media/{pairId}/`) is purged.
   - On the Android device: `keyStoreManager.clearAllCredentials()` wipes all tokens, keys, and Room database tables are dropped/cleared.
