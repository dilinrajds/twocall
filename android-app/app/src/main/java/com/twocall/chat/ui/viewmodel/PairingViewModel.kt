package com.twocall.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twocall.chat.crypto.KeyStoreManager
import com.twocall.chat.data.local.dao.ConversationDao
import com.twocall.chat.data.local.entity.ConversationEntity
import com.twocall.chat.data.remote.api.ChatApiService
import com.twocall.chat.data.remote.dto.CreatePairRequestDto
import com.twocall.chat.data.remote.dto.JoinPairRequestDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PairingUiState(
    val isLoading: Boolean = false,
    val pairingCode: String? = null,
    val secondsRemaining: Int = 300,
    val error: String? = null,
    val isPairedSuccessfully: Boolean = false
)

class PairingViewModel(
    private val apiService: ChatApiService,
    private val keyStoreManager: KeyStoreManager,
    private val conversationDao: ConversationDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun isAlreadyPaired(): Boolean {
        return keyStoreManager.isPaired()
    }

    suspend fun checkAndRefreshPairStatus(): Boolean {
        if (keyStoreManager.isPaired()) return true
        try {
            val res = apiService.getPairInfo()
            if (res.isSuccessful && res.body() != null) {
                val info = res.body()!!
                if (!info.partnerDeviceId.isNullOrBlank() && !info.partnerPublicKey.isNullOrBlank()) {
                    keyStoreManager.savePartnerInfo(info.partnerDeviceId, info.partnerPublicKey)
                    conversationDao.insertOrUpdate(
                        ConversationEntity(
                            pairId = info.pairId,
                            partnerDeviceId = info.partnerDeviceId
                        )
                    )
                    return true
                }
            }
        } catch (e: Exception) {
            // Server warm-up ping completed / ignore transient errors
        }
        return false
    }

    fun checkCurrentPairStatus() {
        viewModelScope.launch {
            try {
                val res = apiService.getPairInfo()
                if (res.isSuccessful && res.body() != null) {
                    val info = res.body()!!
                    if (!info.partnerDeviceId.isNullOrBlank() && !info.partnerPublicKey.isNullOrBlank()) {
                        keyStoreManager.savePartnerInfo(info.partnerDeviceId, info.partnerPublicKey)
                        conversationDao.insertOrUpdate(
                            ConversationEntity(
                                pairId = info.pairId,
                                partnerDeviceId = info.partnerDeviceId
                            )
                        )
                        timerJob?.cancel()
                        _uiState.value = _uiState.value.copy(
                            isPairedSuccessfully = true
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore transient errors
            }
        }
    }

    fun createPair(deviceLabel: String = "My Phone") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val fingerprint = keyStoreManager.getOrCreateDeviceFingerprint()
            val publicKey = keyStoreManager.getMyPublicKeyBase64()

            var attempts = 0
            var success = false

            while (attempts < 3 && !success) {
                attempts++
                try {
                    val res = apiService.createPair(
                        CreatePairRequestDto(
                            deviceFingerprint = fingerprint,
                            publicIdentityKey = publicKey,
                            deviceLabel = deviceLabel
                        )
                    )

                    if (res.isSuccessful && res.body() != null) {
                        val body = res.body()!!
                        keyStoreManager.savePairingSession(
                            pairId = body.pairId,
                            deviceId = body.deviceId,
                            accessToken = body.accessToken,
                            refreshToken = body.refreshToken
                        )

                        conversationDao.insertOrUpdate(
                            ConversationEntity(pairId = body.pairId)
                        )

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            pairingCode = body.pairingCode,
                            secondsRemaining = 300,
                            error = null
                        )
                        startCountdownTimer()
                        success = true
                    } else {
                        val errMsg = res.errorBody()?.string() ?: "Failed to generate pairing code"
                        _uiState.value = _uiState.value.copy(isLoading = false, error = errMsg)
                        break
                    }
                } catch (e: Exception) {
                    if (attempts < 3) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            error = "Waking up secure server... Attempt $attempts/3"
                        )
                        delay(2500)
                    } else {
                        val friendlyError = if (e is java.net.SocketTimeoutException || e.message?.contains("timeout", ignoreCase = true) == true) {
                            "Server connection timed out during cold start. Please tap 'Try Again'."
                        } else {
                            e.localizedMessage ?: "Network connection error"
                        }
                        _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError)
                    }
                }
            }
        }
    }

    fun joinPair(code: String, deviceLabel: String = "Partner Phone") {
        if (!code.matches(Regex("^[0-9]{6}$"))) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid 6-digit code")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val fingerprint = keyStoreManager.getOrCreateDeviceFingerprint()
            val publicKey = keyStoreManager.getMyPublicKeyBase64()

            var attempts = 0
            var success = false

            while (attempts < 3 && !success) {
                attempts++
                try {
                    val res = apiService.joinPair(
                        JoinPairRequestDto(
                            code = code.trim(),
                            deviceFingerprint = fingerprint,
                            publicIdentityKey = publicKey,
                            deviceLabel = deviceLabel
                        )
                    )

                    if (res.isSuccessful && res.body() != null) {
                        val body = res.body()!!
                        keyStoreManager.savePairingSession(
                            pairId = body.pairId,
                            deviceId = body.deviceId,
                            accessToken = body.accessToken,
                            refreshToken = body.refreshToken,
                            partnerDeviceId = body.partnerDeviceId,
                            partnerPublicKey = body.partnerPublicKey
                        )

                        conversationDao.insertOrUpdate(
                            ConversationEntity(
                                pairId = body.pairId,
                                partnerDeviceId = body.partnerDeviceId
                            )
                        )

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isPairedSuccessfully = true,
                            error = null
                        )
                        success = true
                    } else {
                        val errMsg = if (res.code() == 410) {
                            "Pairing code has expired. Request a new code from partner."
                        } else if (res.code() == 403) {
                            "Pair limit reached. A third device cannot join."
                        } else {
                            "Invalid pairing code or maximum attempts exceeded."
                        }
                        _uiState.value = _uiState.value.copy(isLoading = false, error = errMsg)
                        break
                    }
                } catch (e: Exception) {
                    if (attempts < 3) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            error = "Waking up secure server... Attempt $attempts/3"
                        )
                        delay(2500)
                    } else {
                        val friendlyError = if (e is java.net.SocketTimeoutException || e.message?.contains("timeout", ignoreCase = true) == true) {
                            "Server connection timed out during cold start. Please tap 'Connect Private Pair' again."
                        } else {
                            e.localizedMessage ?: "Network connection error"
                        }
                        _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError)
                    }
                }
            }
        }
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.secondsRemaining > 0) {
                delay(1000)
                val remaining = _uiState.value.secondsRemaining - 1
                _uiState.value = _uiState.value.copy(secondsRemaining = remaining)

                // Poll partner status every 2 seconds
                if (remaining % 2 == 0) {
                    try {
                        val res = apiService.getPairInfo()
                        if (res.isSuccessful && res.body() != null) {
                            val info = res.body()!!
                            if (!info.partnerDeviceId.isNullOrBlank() && !info.partnerPublicKey.isNullOrBlank()) {
                                keyStoreManager.savePartnerInfo(info.partnerDeviceId, info.partnerPublicKey)
                                conversationDao.insertOrUpdate(
                                    ConversationEntity(
                                        pairId = info.pairId,
                                        partnerDeviceId = info.partnerDeviceId
                                    )
                                )
                                _uiState.value = _uiState.value.copy(
                                    isPairedSuccessfully = true
                                )
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        // Transient network fluctuation during poll; continue countdown
                    }
                }
            }
            if (!_uiState.value.isPairedSuccessfully) {
                _uiState.value = _uiState.value.copy(error = "Pairing code expired. Please create a new pair.")
            }
        }
    }

    fun markPairingComplete() {
        _uiState.value = _uiState.value.copy(isPairedSuccessfully = true)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun reset() {
        timerJob?.cancel()
        _uiState.value = PairingUiState()
    }
}
