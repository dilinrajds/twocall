package com.twocall.chat.data.remote.ws

import android.util.Log
import com.google.gson.Gson
import com.twocall.chat.crypto.KeyStoreManager
import com.twocall.chat.data.remote.dto.WsEventDto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

enum class WsConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

class WebSocketClient(
    private val okHttpClient: OkHttpClient,
    private val keyStoreManager: KeyStoreManager,
    private val gson: Gson = Gson()
) {

    private val tag = "WebSocketClient"

    private var webSocket: WebSocket? = null
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow(WsConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _incomingEvents = MutableSharedFlow<WsEventDto<Any>>(extraBufferCapacity = 64)
    val incomingEvents = _incomingEvents.asSharedFlow()

    private var reconnectJob: Job? = null
    private var shouldReconnect = true
    private var retryDelayMs = 2000L
    private var lastRawWsUrl: String? = null

    var onTokenRefreshRequired: (suspend () -> Boolean)? = null

    fun connect(wsUrl: String) {
        shouldReconnect = true

        // Clean base URL by stripping existing query parameters
        val cleanBaseUrl = if (wsUrl.contains("?")) wsUrl.substringBefore("?") else wsUrl
        lastRawWsUrl = cleanBaseUrl

        val token = keyStoreManager.getAccessToken() ?: run {
            Log.w(tag, "Cannot connect WebSocket: No access token found")
            return
        }

        val urlWithAuth = "$cleanBaseUrl?token=$token"
        val request = Request.Builder().url(urlWithAuth).build()

        // Close any stale socket before creating a new one
        try {
            webSocket?.close(1000, "Reconnecting")
        } catch (ignored: Exception) {}

        _connectionState.value = WsConnectionState.CONNECTING
        Log.i(tag, "Connecting WebSocket to $cleanBaseUrl...")

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.i(tag, "WebSocket connected successfully!")
                _connectionState.value = WsConnectionState.CONNECTED
                retryDelayMs = 2000L // Reset backoff
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val event = gson.fromJson(text, WsEventDto::class.java) as WsEventDto<Any>
                    scope.launch {
                        _incomingEvents.emit(event)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to parse incoming WebSocket message: $text", e)
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
                _connectionState.value = WsConnectionState.DISCONNECTED
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _connectionState.value = WsConnectionState.DISCONNECTED
                scheduleReconnect(cleanBaseUrl)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                val statusCode = response?.code ?: 0
                Log.w(tag, "WebSocket failure (HTTP $statusCode): ${t.message}")
                _connectionState.value = WsConnectionState.DISCONNECTED

                if (statusCode == 401 || statusCode == 403) {
                    Log.i(tag, "WebSocket auth failed (HTTP $statusCode). Attempting token refresh...")
                    scope.launch {
                        val refreshed = onTokenRefreshRequired?.invoke() ?: false
                        if (refreshed) {
                            Log.i(tag, "Token refreshed successfully. Reconnecting WebSocket immediately...")
                            connect(cleanBaseUrl)
                        } else {
                            scheduleReconnect(cleanBaseUrl)
                        }
                    }
                } else {
                    scheduleReconnect(cleanBaseUrl)
                }
            }
        })
    }

    private fun scheduleReconnect(wsUrl: String) {
        if (!shouldReconnect) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(retryDelayMs)
            retryDelayMs = (retryDelayMs * 2).coerceAtMost(30000L) // Exponential backoff up to 30s
            Log.i(tag, "Attempting WebSocket reconnect in ${retryDelayMs / 1000}s...")
            connect(wsUrl)
        }
    }

    fun <T> sendEvent(eventType: String, payload: T): Boolean {
        val pairId = keyStoreManager.getPairId() ?: return false
        val deviceId = keyStoreManager.getDeviceId() ?: return false

        val event = WsEventDto(
            eventType = eventType,
            pairId = pairId,
            senderDeviceId = deviceId,
            recipientDeviceId = keyStoreManager.getPartnerDeviceId(),
            timestamp = java.time.Instant.now().toString(),
            payload = payload
        )

        val json = gson.toJson(event)
        return webSocket?.send(json) ?: false
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        _connectionState.value = WsConnectionState.DISCONNECTED
    }
}
