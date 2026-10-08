package com.twocall.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.twocall.chat.ui.navigation.NavGraph
import com.twocall.chat.ui.navigation.Screen
import com.twocall.chat.ui.theme.TwoCallTheme
import com.twocall.chat.ui.viewmodel.CallViewModel
import com.twocall.chat.ui.viewmodel.ChatViewModel
import com.twocall.chat.ui.viewmodel.PairingViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {

    private val intentFlow = MutableSharedFlow<Intent>(extraBufferCapacity = 1)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentFlow.tryEmit(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ChatApplication

        requestRequiredPermissions()

        val pairingViewModel = PairingViewModel(
            app.apiClient.apiService,
            app.keyStoreManager,
            app.database.conversationDao()
        )

        val chatViewModel = ChatViewModel(
            app.chatRepository,
            app.keyStoreManager,
            app.voiceRecorder,
            app.voicePlayer,
            onConversationSelected = { app.connectWebSocket() }
        )

        val callViewModel = CallViewModel(
            app.webRtcManager,
            app.chatRepository,
            app.keyStoreManager
        )

        setContent {
            TwoCallTheme {
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    handleIntent(intent, navController, chatViewModel, callViewModel)
                }

                LaunchedEffect(Unit) {
                    intentFlow.collect { newIntent ->
                        handleIntent(newIntent, navController, chatViewModel, callViewModel)
                    }
                }

                LaunchedEffect(Unit) {
                    callViewModel.callState.collect { state ->
                        val incoming = state == com.twocall.chat.webrtc.CallState.INCOMING_RINGING
                        setShowWhenLocked(incoming)
                        setTurnScreenOn(incoming)
                    }
                }

                NavGraph(
                    navController = navController,
                    pairingViewModel = pairingViewModel,
                    chatViewModel = chatViewModel,
                    callViewModel = callViewModel,
                    app = app
                )
            }
        }
    }

    private suspend fun handleIntent(
        intent: Intent?,
        navController: NavHostController,
        chatViewModel: ChatViewModel,
        callViewModel: CallViewModel
    ) {
        if (intent == null) return

        val openPairId = intent.getStringExtra("openPairId")
        if (!openPairId.isNullOrBlank()) {
            chatViewModel.selectConversation(openPairId)
            if (navController.currentDestination?.route != Screen.Conversation.route) {
                navController.navigate(Screen.Conversation.route)
            }
        }

        val isIncomingCall = intent.getBooleanExtra("incomingCall", false)
        if (isIncomingCall) {
            val callPairId = intent.getStringExtra("pairId")
            val callId = intent.getStringExtra("callId")
            val app = application as ChatApplication
            if (callPairId.isNullOrBlank() || callId.isNullOrBlank() || callPairId !in app.keyStoreManager.getAllPairIds()) return
            chatViewModel.selectConversation(callPairId)
            app.connectWebSocket()
            val connected = withTimeoutOrNull(15000) {
                app.webSocketClient.connectionState.first { it == com.twocall.chat.data.remote.ws.WsConnectionState.CONNECTED }
            }
            val recovered = try { connected != null && app.webRtcManager.recoverIncomingCall(callId, callPairId) }
                catch (e: Exception) { false }
            if (!recovered) {
                android.widget.Toast.makeText(this, "Call is no longer available. Please ask your partner to call again.", android.widget.Toast.LENGTH_LONG).show()
                com.twocall.chat.fcm.ChatFirebaseMessagingService.cancelIncomingCallNotification(this)
                return
            }
            try { app.chatRepository.syncProfile(callPairId) } catch (ignored: Exception) {}
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val acceptCall = intent.getBooleanExtra("acceptCall", false)
            if (acceptCall) {
                callViewModel.requestAutoAccept()
            }
            val isVideo = "VIDEO".equals(intent.getStringExtra("callType"), ignoreCase = true)
            val target = if (isVideo) Screen.VideoCall.route else Screen.AudioCall.route
            if (navController.currentDestination?.route != target) {
                navController.navigate(target)
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }
}
