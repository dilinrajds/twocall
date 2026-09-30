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

    companion object {
        private const val KEY_PAIR_ID = "pair_id"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_PARTNER_DEVICE_ID = "partner_device_id"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_MY_PUBLIC_KEY = "my_public_key"
        private const val KEY_MY_PRIVATE_KEY = "my_private_key"
        private const val KEY_PARTNER_PUBLIC_KEY = "partner_public_key"
        private const val KEY_DEVICE_FINGERPRINT = "device_fingerprint"
    }

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

    fun savePairingSession(
        pairId: String,
        deviceId: String,
        accessToken: String,
        refreshToken: String,
        partnerDeviceId: String? = null,
        partnerPublicKey: String? = null
    ) {
        val editor = encryptedPrefs.edit()
            .putString(KEY_PAIR_ID, pairId)
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)

        partnerDeviceId?.let { editor.putString(KEY_PARTNER_DEVICE_ID, it) }
        partnerPublicKey?.let { editor.putString(KEY_PARTNER_PUBLIC_KEY, it) }

        editor.apply()
    }

    fun updateTokens(accessToken: String, refreshToken: String) {
        encryptedPrefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun savePartnerInfo(partnerDeviceId: String, partnerPublicKey: String) {
        encryptedPrefs.edit()
            .putString(KEY_PARTNER_DEVICE_ID, partnerDeviceId)
            .putString(KEY_PARTNER_PUBLIC_KEY, partnerPublicKey)
            .apply()
    }

    fun isPaired(): Boolean {
        return getPairId() != null && getAccessToken() != null && getPartnerPublicKey() != null
    }

    fun hasPendingPair(): Boolean {
        return getPairId() != null && getAccessToken() != null && getPartnerPublicKey() == null
    }

    fun getPairId(): String? = encryptedPrefs.getString(KEY_PAIR_ID, null)
    fun getDeviceId(): String? = encryptedPrefs.getString(KEY_DEVICE_ID, null)
    fun getPartnerDeviceId(): String? = encryptedPrefs.getString(KEY_PARTNER_DEVICE_ID, null)
    fun getAccessToken(): String? = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
    fun getPartnerPublicKey(): String? = encryptedPrefs.getString(KEY_PARTNER_PUBLIC_KEY, null)

    fun getSharedSessionAesKey(): SecretKeySpec? {
        val partnerPubBase64 = getPartnerPublicKey() ?: return null
        val myKeyPair = getOrCreateIdentityKeyPair()
        val partnerPublicKey = CryptoEngine.decodePublicKey(partnerPubBase64)
        val sharedSecret = CryptoEngine.computeSharedSecret(myKeyPair.private, partnerPublicKey)
        return CryptoEngine.deriveAesKey(sharedSecret)
    }

    fun clearAllCredentials() {
        encryptedPrefs.edit().clear().apply()
    }
}
