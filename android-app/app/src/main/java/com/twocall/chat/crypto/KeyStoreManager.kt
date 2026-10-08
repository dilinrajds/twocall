package com.twocall.chat.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

class KeyStoreManager(context: Context) {

    fun getMyProfileName(): String = encryptedPrefs.getString("profile_name", "") ?: ""
    fun getMyProfileImage(): String? = encryptedPrefs.getString("profile_image", null)
    fun saveMyProfile(name: String, image: String?) {
        encryptedPrefs.edit().putString("profile_name", name).putString("profile_image", image).apply()
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "twocall_secure_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        // Preserve sessions when upgrading from the original single-pair release.
        val legacyPairId = encryptedPrefs.getString("pair_id", null)
        if (legacyPairId != null) {
            val editor = encryptedPrefs.edit()
            if (legacyPairId !in getAllPairIds()) {
                editor.putString(KEY_ALL_PAIR_IDS, (getAllPairIds() + legacyPairId).joinToString(","))
                val keys = mapOf("device_id" to keyPairDeviceId(legacyPairId),
                    "access_token" to keyAccessToken(legacyPairId), "refresh_token" to keyRefreshToken(legacyPairId),
                    "partner_device_id" to keyPartnerDeviceId(legacyPairId), "partner_public_key" to keyPartnerPublicKey(legacyPairId))
                for ((oldKey, newKey) in keys) encryptedPrefs.getString(oldKey, null)?.let { editor.putString(newKey, it) }
            }
            if (getActivePairId() == null) editor.putString(KEY_ACTIVE_PAIR_ID, legacyPairId)
            listOf("pair_id", "device_id", "access_token", "refresh_token", "partner_device_id", "partner_public_key")
                .forEach { editor.remove(it) }
            editor.commit()
        }
    }

    companion object {
        // Single identity keys (same across all pairs)
        private const val KEY_MY_PUBLIC_KEY = "my_public_key"
        private const val KEY_MY_PRIVATE_KEY = "my_private_key"
        private const val KEY_DEVICE_FINGERPRINT = "device_fingerprint"

        // Active pair context (which pair is currently "open")
        private const val KEY_ACTIVE_PAIR_ID = "active_pair_id"

        // All known pairIds (stored as comma-separated list)
        private const val KEY_ALL_PAIR_IDS = "all_pair_ids"

        // Per-pair keys: suffix is pairId
        private fun keyPairDeviceId(pairId: String) = "pair_device_id_$pairId"
        private fun keyAccessToken(pairId: String) = "access_token_$pairId"
        private fun keyRefreshToken(pairId: String) = "refresh_token_$pairId"
        private fun keyPartnerDeviceId(pairId: String) = "partner_device_id_$pairId"
        private fun keyPartnerPublicKey(pairId: String) = "partner_pub_key_$pairId"
    }

    // ── Identity & Fingerprint ──────────────────────────────────────────────

    fun getOrCreateDeviceFingerprint(): String {
        var fp = encryptedPrefs.getString(KEY_DEVICE_FINGERPRINT, null)
        if (fp == null) {
            fp = UUID.randomUUID().toString()
            encryptedPrefs.edit().putString(KEY_DEVICE_FINGERPRINT, fp).apply()
        }
        return fp
    }

    fun getOrCreateIdentityKeyPair(): KeyPair {
        val pubBase64 = encryptedPrefs.getString(KEY_MY_PUBLIC_KEY, null)
        val privBase64 = encryptedPrefs.getString(KEY_MY_PRIVATE_KEY, null)

        if (pubBase64 != null && privBase64 != null) {
            val keyFactory = KeyFactory.getInstance("EC")
            val pubBytes = Base64.getDecoder().decode(pubBase64)
            val privBytes = Base64.getDecoder().decode(privBase64)
            val pub = keyFactory.generatePublic(X509EncodedKeySpec(pubBytes))
            val priv = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privBytes))
            return KeyPair(pub, priv)
        }

        val newPair = CryptoEngine.generateKeyPair()
        encryptedPrefs.edit()
            .putString(KEY_MY_PUBLIC_KEY, Base64.getEncoder().encodeToString(newPair.public.encoded))
            .putString(KEY_MY_PRIVATE_KEY, Base64.getEncoder().encodeToString(newPair.private.encoded))
            .apply()
        return newPair
    }

    fun getMyPublicKeyBase64(): String {
        return CryptoEngine.encodePublicKey(getOrCreateIdentityKeyPair().public)
    }

    // ── Multi-Pair Session Management ───────────────────────────────────────

    fun getAllPairIds(): List<String> {
        val raw = encryptedPrefs.getString(KEY_ALL_PAIR_IDS, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
    }

    private fun addPairId(pairId: String) {
        val existing = getAllPairIds().toMutableList()
        if (!existing.contains(pairId)) {
            existing.add(pairId)
            encryptedPrefs.edit().putString(KEY_ALL_PAIR_IDS, existing.joinToString(",")).apply()
        }
    }

    fun savePairingSession(
        pairId: String,
        deviceId: String,
        accessToken: String,
        refreshToken: String,
        partnerDeviceId: String? = null,
        partnerPublicKey: String? = null
    ) {
        addPairId(pairId)
        val editor = encryptedPrefs.edit()
            .putString(keyPairDeviceId(pairId), deviceId)
            .putString(keyAccessToken(pairId), accessToken)
            .putString(keyRefreshToken(pairId), refreshToken)
            // Set as active pair when first saved
            .putString(KEY_ACTIVE_PAIR_ID, pairId)

        partnerDeviceId?.let { editor.putString(keyPartnerDeviceId(pairId), it) }
        partnerPublicKey?.let { editor.putString(keyPartnerPublicKey(pairId), it) }

        editor.apply()
    }

    fun savePartnerInfo(pairId: String, partnerDeviceId: String, partnerPublicKey: String) {
        encryptedPrefs.edit()
            .putString(keyPartnerDeviceId(pairId), partnerDeviceId)
            .putString(keyPartnerPublicKey(pairId), partnerPublicKey)
            .apply()
    }

    /** Compat: saves partner info for active pair */
    fun savePartnerInfo(partnerDeviceId: String, partnerPublicKey: String) {
        val pairId = getActivePairId() ?: return
        savePartnerInfo(pairId, partnerDeviceId, partnerPublicKey)
    }

    fun updateTokens(pairId: String, accessToken: String, refreshToken: String) {
        encryptedPrefs.edit()
            .putString(keyAccessToken(pairId), accessToken)
            .putString(keyRefreshToken(pairId), refreshToken)
            .apply()
    }

    /** Compat: updates tokens for active pair */
    fun updateTokens(accessToken: String, refreshToken: String) {
        val pairId = getActivePairId() ?: return
        updateTokens(pairId, accessToken, refreshToken)
    }

    // ── Active Pair ─────────────────────────────────────────────────────────

    fun setActivePairId(pairId: String) {
        encryptedPrefs.edit().putString(KEY_ACTIVE_PAIR_ID, pairId).apply()
    }

    fun getActivePairId(): String? = encryptedPrefs.getString(KEY_ACTIVE_PAIR_ID, null)

    // ── Per-Pair Credential Accessors ────────────────────────────────────────

    fun getDeviceId(pairId: String): String? = encryptedPrefs.getString(keyPairDeviceId(pairId), null)
    fun getAccessToken(pairId: String): String? = encryptedPrefs.getString(keyAccessToken(pairId), null)
    fun getRefreshToken(pairId: String): String? = encryptedPrefs.getString(keyRefreshToken(pairId), null)
    fun getPartnerDeviceId(pairId: String): String? = encryptedPrefs.getString(keyPartnerDeviceId(pairId), null)
    fun getPartnerPublicKey(pairId: String): String? = encryptedPrefs.getString(keyPartnerPublicKey(pairId), null)

    fun getSharedAesKey(pairId: String): SecretKeySpec? {
        return try {
            val partnerPubBase64 = getPartnerPublicKey(pairId) ?: return null
            val myKeyPair = getOrCreateIdentityKeyPair()
            val partnerPublicKey = CryptoEngine.decodePublicKey(partnerPubBase64)
            val sharedSecret = CryptoEngine.computeSharedSecret(myKeyPair.private, partnerPublicKey)
            CryptoEngine.deriveAesKey(sharedSecret)
        } catch (e: Exception) {
            android.util.Log.e("KeyStoreManager", "Error deriving AES key for pair $pairId: ${e.message}")
            null
        }
    }

    // ── Active-Pair Compat Accessors (used by legacy code) ──────────────────

    /** Returns true if there is at least one fully paired conversation */
    fun isPaired(): Boolean {
        return getAllPairIds().any { pairId ->
            getAccessToken(pairId) != null && getPartnerPublicKey(pairId) != null
        }
    }

    fun hasPendingPair(): Boolean {
        val active = getActivePairId() ?: return false
        return getAccessToken(active) != null && getPartnerPublicKey(active) == null
    }

    /** Active pair's device id — compat for legacy callers */
    fun getDeviceId(): String? {
        val active = getActivePairId() ?: return null
        return getDeviceId(active)
    }

    /** Active pair's pairId — compat */
    fun getPairId(): String? = getActivePairId()

    /** Active pair's access token — compat */
    fun getAccessToken(): String? {
        val active = getActivePairId() ?: return null
        return getAccessToken(active)
    }

    /** Active pair's refresh token — compat */
    fun getRefreshToken(): String? {
        val active = getActivePairId() ?: return null
        return getRefreshToken(active)
    }

    /** Active pair's partner device id — compat */
    fun getPartnerDeviceId(): String? {
        val active = getActivePairId() ?: return null
        return getPartnerDeviceId(active)
    }

    /** Active pair's partner public key — compat */
    fun getPartnerPublicKey(): String? {
        val active = getActivePairId() ?: return null
        return getPartnerPublicKey(active)
    }

    /** Active pair's shared AES key — compat */
    fun getSharedSessionAesKey(): SecretKeySpec? {
        val active = getActivePairId() ?: return null
        return getSharedAesKey(active)
    }

    // ── Clear Operations ────────────────────────────────────────────────────

    /** Remove a single pair from storage (disconnect/delete) */
    fun removePair(pairId: String) {
        val remaining = getAllPairIds().filter { it != pairId }
        val editor = encryptedPrefs.edit()
        editor.putString(KEY_ALL_PAIR_IDS, remaining.joinToString(","))
        // Remove per-pair keys
        editor.remove(keyPairDeviceId(pairId))
        editor.remove(keyAccessToken(pairId))
        editor.remove(keyRefreshToken(pairId))
        editor.remove(keyPartnerDeviceId(pairId))
        editor.remove(keyPartnerPublicKey(pairId))
        // Update active pair
        if (getActivePairId() == pairId) {
            editor.putString(KEY_ACTIVE_PAIR_ID, remaining.firstOrNull())
        }
        editor.apply()
    }

    /** Clear everything (full logout) */
    fun clearAllCredentials() {
        encryptedPrefs.edit().clear().commit()
    }
}
