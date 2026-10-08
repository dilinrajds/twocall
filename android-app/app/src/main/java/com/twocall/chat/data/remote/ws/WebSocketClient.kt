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
    private var connectedPairId: String? = null

    fun connectForPair(wsUrl: String, pairId: String) {
        if (connectedPairId != pairId) disconnect()
        keyStoreManager.setActivePairId(pairId)
        connect(wsUrl)
    }

    var onTokenRefreshRequired: (suspend () -> Boolean)? = null

    private var connectionWatchdogJob: Job? = null

    @Synchronized
    fun connect(wsUrl: String) {
        if (_connectionState.value == WsConnectionState.CONNECTED ||
            _connectionState.value == WsConnectionState.CONNECTING) {
            Log.d(tag, "WebSocket already connected or connecting, skipping connect()")
            return
        }
        reconnectJob?.cancel()
        connectionWatchdogJob?.cancel()
        shouldReconnect = true

        // Clean base URL by stripping existing query parameters
        val cleanBaseUrl = if (wsUrl.contains("?")) wsUrl.substringBefore("?") else wsUrl
        lastRawWsUrl = cleanBaseUrl

        val token = keyStoreManager.getAccessToken() 
            ?: keyStoreManager.getAllPairIds().firstOrNull()?.let { keyStoreManager.getAccessToken(it) }
            ?: run {
                Log.w(tag, "Cannot connect WebSocket: No access token found")
                return
            }

        val urlWithAuth = "$cleanBaseUrl?token=$token"
        connectedPairId = keyStoreManager.getActivePairId() ?: keyStoreManager.getAllPairIds().firstOrNull()
        val request = Request.Builder().url(urlWithAuth).build()

        // Cancel any stale socket before creating a new one
        try {
            webSocket?.cancel()
        } catch (ignored: Exception) {}
        webSocket = null

        _connectionState.value = WsConnectionState.CONNECTING
        Log.i(tag, "Connecting WebSocket to $cleanBaseUrl...")

        // Watchdog: If connection doesn't succeed within 10 seconds, retry
        connectionWatchdogJob = scope.launch {
            delay(10000L)
            if (_connectionState.value == WsConnectionState.CONNECTING) {
                Log.w(tag, "WebSocket connection attempt timed out after 10s, scheduling reconnect...")
                try {
                    webSocket?.cancel()
                } catch (ignored: Exception) {}
                webSocket = null
                _connectionState.value = WsConnectionState.DISCONNECTED
                scheduleReconnect(cleanBaseUrl)
            }
        }

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                connectionWatchdogJob?.cancel()
                Log.i(tag, "WebSocket connected successfully!")
                _connectionState.value = WsConnectionState.CONNECTED
                retryDelayMs = 1000L // Reset backoff
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.i(tag, "Received WebSocket message: $text")
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
                connectionWatchdogJob?.cancel()
                ws.close(1000, null)
                if (webSocket == ws) {
                    webSocket = null
                    _connectionState.value = WsConnectionState.DISCONNECTED
                }
                if (shouldReconnect) {
                    scheduleReconnect(cleanBaseUrl)
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                connectionWatchdogJob?.cancel()
                if (webSocket == ws) {
                    webSocket = null
                    _connectionState.value = WsConnectionState.DISCONNECTED
                }
                if (shouldReconnect) {
                    scheduleReconnect(cleanBaseUrl)
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                connectionWatchdogJob?.cancel()
                val msg = t.message ?: ""
                val statusCode = response?.code ?: 0
                Log.w(tag, "WebSocket failure (HTTP $statusCode): $msg")

                // If this failure was caused by our own intentional cancellation of a stale socket, ignore it!
                if (msg.contains("Canceled", ignoreCase = true) || msg.contains("Socket closed", ignoreCase = true)) {
                    Log.d(tag, "Ignoring intentional cancellation failure")
                    return
                }

                if (webSocket == ws) {
                    webSocket = null
                    _connectionState.value = WsConnectionState.DISCONNECTED
                }

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
                } else if (shouldReconnect) {
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
            retryDelayMs = (retryDelayMs + 1000L).coerceAtMost(5000L) // Quick retry: 1s, 2s, 3s, max 5s
            Log.i(tag, "Attempting WebSocket reconnect in ${retryDelayMs / 1000}s...")
            _connectionState.value = WsConnectionState.DISCONNECTED
            webSocket = null
            connect(wsUrl)
        }
    }

    fun reconnectNow() {
        shouldReconnect = true
        retryDelayMs = 1000L
        reconnectJob?.cancel()
        connectionWatchdogJob?.cancel()
        try {
            webSocket?.cancel()
        } catch (ignored: Exception) {}
        webSocket = null
        _connectionState.value = WsConnectionState.DISCONNECTED
        lastRawWsUrl?.let { connect(it) }
    }

    fun <T> sendEvent(eventType: String, payload: T, targetPairId: String? = null): Boolean {
        val pairId = targetPairId ?: keyStoreManager.getPairId() ?: run {
            Log.w(tag, "sendEvent: pairId is null")
            return false
        }
        val deviceId = keyStoreManager.getDeviceId(pairId) ?: keyStoreManager.getDeviceId() ?: run {
            Log.w(tag, "sendEvent: deviceId is null")
            return false
        }

        val event = WsEventDto(
            eventType = eventType,
            pairId = pairId,
            senderDeviceId = deviceId,
            recipientDeviceId = keyStoreManager.getPartnerDeviceId(pairId),
            timestamp = java.time.Instant.now().toString(),
            payload = payload
        )

        val json = gson.toJson(event)
        var ws = webSocket
        var success = ws?.send(json) ?: false
        Log.i(tag, "sendEvent: $eventType, sent=$success, wsConnected=${_connectionState.value == WsConnectionState.CONNECTED}")
        
        if (!success) {
            Log.w(tag, "sendEvent: failed to send $eventType! Triggering reconnectNow()...")
            reconnectNow()

            // If it's a critical signaling event, wait briefly and retry once
            if (eventType.startsWith("CALL_") || eventType == "ICE_CANDIDATE") {
                try {
                    val retryThread = Thread {
                        var waitCount = 0
                        while (waitCount < 8 && _connectionState.value != WsConnectionState.CONNECTED) {
                            Thread.sleep(250)
                            waitCount++
                        }
                        if (_connectionState.value == WsConnectionState.CONNECTED) {
                            val retried = webSocket?.send(json) ?: false
                            Log.i(tag, "Retried sendEvent $eventType: success=$retried")
                        }
                    }
                    retryThread.start()
                } catch (ignored: Exception) {}
            }
        }
        return success
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        connectionWatchdogJob?.cancel()
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        _connectionState.value = WsConnectionState.DISCONNECTED
    }
}
