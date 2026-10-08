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
import com.twocall.chat.data.remote.ws.WsConnectionState
import com.twocall.chat.data.repository.ChatRepository
import com.twocall.chat.webrtc.WebRtcManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ChatApplication : Application() {

    private val tag = "ChatApplication"
    val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _pairTerminatedFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val pairTerminatedFlow = _pairTerminatedFlow.asSharedFlow()

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

        createNotificationChannels()

        keyStoreManager = KeyStoreManager(this)
        database = AppDatabase.getInstance(this)
        apiClient = ApiClient(keyStoreManager)
        webSocketClient = WebSocketClient(apiClient.okHttpClient, keyStoreManager).apply {
            onTokenRefreshRequired = { apiClient.forceRefreshTokens() }
        }
        chatRepository = ChatRepository(
            this,
            database.messageDao(),
            database.conversationDao(),
            database.callLogDao(),
            apiClient.apiService,
            webSocketClient,
            keyStoreManager
        )
        chatRepository.onPairTerminated = {
            handlePartnerTerminated()
        }
        webRtcManager = WebRtcManager(this, apiClient.apiService, webSocketClient, keyStoreManager)
        voiceRecorder = VoiceRecorder(this)
        voicePlayer = VoicePlayer()

        // Schedule 12-minute periodic keep-alive worker to keep Render backend active 24/7
        com.twocall.chat.worker.RenderKeepAliveWorker.schedulePeriodicKeepAlive(this)

        // Auto-connect WebSocket if any pair exists
        if (keyStoreManager.isPaired()) {
            connectWebSocket()
            syncMessagesInBackground()
            registerFcmTokenProactively()
        }

        // Lifecycle observer: reconnect WebSocket when app comes to foreground
        registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
            private var startedActivities = 0

            override fun onActivityStarted(activity: android.app.Activity) {
                startedActivities++
                if (startedActivities == 1) {
                    // App entered foreground
                    if (keyStoreManager.isPaired()) {
                        if (webSocketClient.connectionState.value == WsConnectionState.DISCONNECTED) {
                            Log.i(tag, "App foregrounded and WebSocket disconnected — reconnecting")
                            connectWebSocket()
                        }
                        syncMessagesInBackground()
                    }
                }
            }

            override fun onActivityStopped(activity: android.app.Activity) {
                startedActivities--
            }

            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }

    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = getSystemService(android.app.NotificationManager::class.java)

            val msgChannel = android.app.NotificationChannel(
                com.twocall.chat.fcm.ChatFirebaseMessagingService.CHANNEL_MESSAGES,
                "Messages",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Encrypted private message notifications"
                enableVibration(true)
                enableLights(true)
            }

            val callChannel = android.app.NotificationChannel(
                com.twocall.chat.fcm.ChatFirebaseMessagingService.CHANNEL_INCOMING_CALLS,
                "Incoming Calls",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming voice and video call ring notifications"
                setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE), null)
                enableVibration(true)
                enableLights(true)
            }

            manager.createNotificationChannel(msgChannel)
            manager.createNotificationChannel(callChannel)
        }
    }

    private fun registerFcmTokenProactively() {
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isNotEmpty()) {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        if (!token.isNullOrBlank()) {
                            registerPushToken(token)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(tag, "Failed to get FCM token proactively: ${e.message}")
                    }
            } else {
                Log.i(tag, "Firebase is not initialized (no google-services.json). Skipping FCM token registration.")
            }
        } catch (t: Throwable) {
            Log.w(tag, "FCM registration skipped or unavailable: ${t.message}")
        }
    }

    fun connectWebSocket() {
        registerFcmTokenProactively()
        val cleanUrl = apiClient.baseUrl.trimEnd('/')
        val wsUrl = (if (cleanUrl.startsWith("https://")) {
            cleanUrl.replaceFirst("https://", "wss://")
        } else {
            cleanUrl.replaceFirst("http://", "ws://")
        }) + "/ws"
        val pairId = keyStoreManager.getActivePairId() ?: return
        webSocketClient.connectForPair(wsUrl, pairId)
    }

    fun registerPushToken(token: String) {
        applicationScope.launch {
            try {
                if (keyStoreManager.isPaired()) {
                    for (pairId in keyStoreManager.getAllPairIds()) {
                        try {
                            val result = apiClient.apiService.registerPushToken(RegisterPushTokenRequestDto(token), pairId)
                            if (!result.isSuccessful) Log.w(tag, "Push token registration failed: HTTP ${result.code()}")
                        } catch (e: Exception) { Log.w(tag, "Push token registration failed for pair: ${e.message}") }
                    }
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
                chatRepository.syncMessagesForAllPairs()
            } catch (e: Exception) {
                Log.w(tag, "Background sync failed: ${e.message}")
            }
        }
    }

    fun handlePartnerTerminated() {
        applicationScope.launch {
            try {
                webSocketClient.disconnect()
            } catch (ignored: Exception) {}
            keyStoreManager.clearAllCredentials()
            database.messageDao().deleteAll()
            database.conversationDao().deleteAll()
            database.callLogDao().deleteAll()
            _pairTerminatedFlow.tryEmit(Unit)
            Log.i(tag, "Pair terminated handled locally: data wiped, disconnected, event emitted")
        }
    }

    suspend fun disconnectDevice() {
        try { apiClient.apiService.disconnect() } catch (ignored: Exception) {}
        try { webSocketClient.disconnect() } catch (ignored: Exception) {}
        keyStoreManager.clearAllCredentials()
        database.messageDao().deleteAll()
        database.conversationDao().deleteAll()
        database.callLogDao().deleteAll()
    }

    suspend fun deletePairPermanently() {
        try { apiClient.apiService.deletePair() } catch (ignored: Exception) {}
        try { webSocketClient.disconnect() } catch (ignored: Exception) {}
        keyStoreManager.clearAllCredentials()
        database.messageDao().deleteAll()
        database.conversationDao().deleteAll()
        database.callLogDao().deleteAll()
    }
}
