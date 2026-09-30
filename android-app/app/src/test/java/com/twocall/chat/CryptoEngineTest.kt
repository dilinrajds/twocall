package com.twocall.chat

import com.twocall.chat.crypto.CryptoEngine
import org.junit.Assert.*
import org.junit.Test
import javax.crypto.AEADBadTagException

class CryptoEngineTest {

    @Test
    fun testE2EESharedSecretAndAesGcmRoundtrip() {
        // Device A generates EC key pair
        val keyPairA = CryptoEngine.generateKeyPair()
        val pubBase64A = CryptoEngine.encodePublicKey(keyPairA.public)

        // Device B generates EC key pair
        val keyPairB = CryptoEngine.generateKeyPair()
        val pubBase64B = CryptoEngine.encodePublicKey(keyPairB.public)

        // Decode public keys
        val decodedPubA = CryptoEngine.decodePublicKey(pubBase64A)
        val decodedPubB = CryptoEngine.decodePublicKey(pubBase64B)

        // Compute ECDH shared secret on both devices independently
        val sharedSecretA = CryptoEngine.computeSharedSecret(keyPairA.private, decodedPubB)
        val sharedSecretB = CryptoEngine.computeSharedSecret(keyPairB.private, decodedPubA)

        // Verify shared secrets match identically
        assertArrayEquals("ECDH shared secrets must be identical on both devices", sharedSecretA, sharedSecretB)

        // Derive AES-256 keys using HKDF-SHA256
        val aesKeyA = CryptoEngine.deriveAesKey(sharedSecretA)
        val aesKeyB = CryptoEngine.deriveAesKey(sharedSecretB)

        assertArrayEquals("Derived AES keys must be identical", aesKeyA.encoded, aesKeyB.encoded)

        // Device A encrypts a private message
        val originalPlaintext = "Hello my partner! This is 100% private and encrypted."
        val encryptedPayload = CryptoEngine.encrypt(originalPlaintext, aesKeyA)

        assertNotNull(encryptedPayload.ciphertext)
        assertNotNull(encryptedPayload.iv)
        assertNotEquals(originalPlaintext, encryptedPayload.ciphertext)

        // Device B decrypts the ciphertext
        val decryptedText = CryptoEngine.decrypt(encryptedPayload.ciphertext, encryptedPayload.iv, aesKeyB)
        assertEquals("Decrypted message must match original plaintext exactly", originalPlaintext, decryptedText)
    }

    @Test
    fun testKeyFingerprintGeneration() {
        val keyPair = CryptoEngine.generateKeyPair()
        val pubBase64 = CryptoEngine.encodePublicKey(keyPair.public)

        val fingerprint = CryptoEngine.computeKeyFingerprint(pubBase64)
        assertNotNull(fingerprint)
        assertTrue("Fingerprint must contain hex pairs", fingerprint.matches(Regex("^([0-9A-F]{2}\\s?)+$")))
    }
}
