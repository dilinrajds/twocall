package com.twocall.chat.crypto

import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedPayload(
    val ciphertext: String,
    val iv: String,
    val ephemeralPublicKey: String? = null
)

object CryptoEngine {

    private const val EC_CURVE = "secp256r1"
    private const val AES_GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12

    fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(ECGenParameterSpec(EC_CURVE))
        return keyPairGenerator.generateKeyPair()
    }

    fun encodePublicKey(publicKey: PublicKey): String {
        return Base64.getEncoder().encodeToString(publicKey.encoded)
    }

    fun decodePublicKey(base64PublicKey: String): PublicKey {
        val keyBytes = Base64.getDecoder().decode(base64PublicKey)
        val keyFactory = KeyFactory.getInstance("EC")
        return keyFactory.generatePublic(X509EncodedKeySpec(keyBytes))
    }

    fun computeSharedSecret(myPrivateKey: PrivateKey, partnerPublicKey: PublicKey): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(myPrivateKey)
        keyAgreement.doPhase(partnerPublicKey, true)
        return keyAgreement.generateSecret()
    }

    /**
     * HKDF-SHA256 (RFC 5869) to derive a 256-bit AES key from ECDH shared secret
     */
    fun deriveAesKey(sharedSecret: ByteArray, salt: ByteArray? = null, info: String = "TwoCall-E2EE-v1"): SecretKeySpec {
        val mac = Mac.getInstance("HmacSHA256")
        val saltKey = salt ?: "TwoCallDefaultSalt2026".toByteArray(StandardCharsets.UTF_8)
        mac.init(SecretKeySpec(saltKey, "HmacSHA256"))
        val prk = mac.doFinal(sharedSecret)

        // Expand step
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(info.toByteArray(StandardCharsets.UTF_8))
        mac.update(0x01.toByte())
        val okm = mac.doFinal()

        val aesKeyBytes = ByteArray(32)
        System.arraycopy(okm, 0, aesKeyBytes, 0, 32)
        return SecretKeySpec(aesKeyBytes, "AES")
    }

    /**
     * Encrypts plaintext using AES-256-GCM with a secure 12-byte IV
     */
    fun encrypt(plaintext: String, aesKey: SecretKeySpec): EncryptedPayload {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(AES_GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec)

        val ciphertextBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))

        return EncryptedPayload(
            ciphertext = Base64.getEncoder().encodeToString(ciphertextBytes),
            iv = Base64.getEncoder().encodeToString(iv)
        )
    }

    /**
     * Decrypts ciphertext using AES-256-GCM
     */
    fun decrypt(ciphertextBase64: String, ivBase64: String, aesKey: SecretKeySpec): String {
        val ciphertextBytes = Base64.getDecoder().decode(ciphertextBase64)
        val ivBytes = Base64.getDecoder().decode(ivBase64)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(AES_GCM_TAG_LENGTH_BITS, ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec)

        val decryptedBytes = cipher.doFinal(ciphertextBytes)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    /**
     * Computes safety fingerprint for verifying identity out-of-band
     */
    fun computeKeyFingerprint(publicKeyBase64: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(publicKeyBase64.toByteArray(StandardCharsets.UTF_8))
        return hash.take(16).joinToString(" ") { "%02X".format(it) }
    }
}
