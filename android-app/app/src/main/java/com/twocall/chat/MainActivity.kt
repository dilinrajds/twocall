package com.twocall.chat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.twocall.chat.ui.navigation.NavGraph
import com.twocall.chat.ui.navigation.Screen
import com.twocall.chat.ui.theme.TwoCallTheme
import com.twocall.chat.ui.viewmodel.CallViewModel
import com.twocall.chat.ui.viewmodel.ChatViewModel
import com.twocall.chat.ui.viewmodel.PairingViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
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
            app.voicePlayer
        )

        val callViewModel = CallViewModel(
            app.webRtcManager
        )

        setContent {
            TwoCallTheme {
                val navController = rememberNavController()

                // Check for incoming call intent from notification
                val isIncomingCall = intent?.getBooleanExtra("incomingCall", false) ?: false
                val isVideo = "VIDEO".equals(intent?.getStringExtra("callType"), ignoreCase = true)

                if (isIncomingCall) {
                    if (isVideo) {
                        navController.navigate(Screen.VideoCall.route)
                    } else {
                        navController.navigate(Screen.AudioCall.route)
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
