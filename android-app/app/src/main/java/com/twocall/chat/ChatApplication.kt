package com.twocall.chat

import android.app.Application
import android.util.Log
import com.twocall.chat.audio.VoicePlayer
import com.twocall.chat.audio.VoiceRecorder
import com.twocall.chat.crypto.KeyStoreManager
import com.twocall.chat.data.local.AppDatabase
import com.twocall.chat.data.remote.api.ApiClient
import com.twocall.chat.data.remote.dto.RegisterPushTokenRequestDto
import com.twocall.chat.data.remote.ws.WebSocketClient
import com.twocall.chat.data.repository.ChatRepository
import com.twocall.chat.webrtc.WebRtcManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ChatApplication : Application() {

    private val tag = "ChatApplication"
    val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    lateinit var keyStoreManager: KeyStoreManager
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var apiClient: ApiClient
        private set
    lateinit var webSocketClient: WebSocketClient
        private set
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var webRtcManager: WebRtcManager
        private set
    lateinit var voiceRecorder: VoiceRecorder
        private set
    lateinit var voicePlayer: VoicePlayer
        private set

    override fun onCreate() {
        super.onCreate()

        keyStoreManager = KeyStoreManager(this)
        database = AppDatabase.getInstance(this)
        apiClient = ApiClient(keyStoreManager)
        webSocketClient = WebSocketClient(apiClient.okHttpClient, keyStoreManager)
        chatRepository = ChatRepository(
            database.messageDao(),
            database.conversationDao(),
            apiClient.apiService,
            webSocketClient,
            keyStoreManager
        )
        webRtcManager = WebRtcManager(this, apiClient.apiService, webSocketClient)
        voiceRecorder = VoiceRecorder(this)
        voicePlayer = VoicePlayer()

        // Auto-connect WebSocket if paired
        if (keyStoreManager.isPaired()) {
            connectWebSocket()
            syncMessagesInBackground()
        }
    }

    fun connectWebSocket() {
        // Form WebSocket URL from HTTP base URL (wss://.../ws)
        val cleanUrl = apiClient.baseUrl.trimEnd('/')
        val wsUrl = (if (cleanUrl.startsWith("https://")) {
            cleanUrl.replaceFirst("https://", "wss://")
        } else {
            cleanUrl.replaceFirst("http://", "ws://")
        }) + "/ws"
        webSocketClient.connect(wsUrl)
    }

    fun registerPushToken(token: String) {
        applicationScope.launch {
            try {
                if (keyStoreManager.isPaired()) {
                    apiClient.apiService.registerPushToken(RegisterPushTokenRequestDto(token))
                    Log.i(tag, "FCM Push token registered with backend")
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to register push token: ${e.message}")
            }
        }
    }

    fun syncMessagesInBackground() {
        applicationScope.launch {
            try {
                chatRepository.syncMessages()
            } catch (e: Exception) {
                Log.w(tag, "Background sync failed: ${e.message}")
            }
        }
    }

    suspend fun disconnectDevice() {
        try {
            apiClient.apiService.disconnect()
        } catch (ignored: Exception) {}
        webSocketClient.disconnect()
        keyStoreManager.clearAllCredentials()
        database.messageDao().deleteAll()
        database.conversationDao().deleteAll()
    }

    suspend fun deletePairPermanently() {
        try {
            apiClient.apiService.deletePair()
        } catch (ignored: Exception) {}
        webSocketClient.disconnect()
        keyStoreManager.clearAllCredentials()
        database.messageDao().deleteAll()
        database.conversationDao().deleteAll()
    }
}
