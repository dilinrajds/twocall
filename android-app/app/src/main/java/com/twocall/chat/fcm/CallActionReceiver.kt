package com.twocall.chat.fcm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.twocall.chat.ChatApplication
import kotlinx.coroutines.launch

class CallActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DECLINE_CALL = "com.twocall.chat.ACTION_DECLINE_CALL"
        const val EXTRA_CALL_ID = "callId"
        const val EXTRA_PAIR_ID = "pairId"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i("CallActionReceiver", "Received broadcast action: $action")

        if (action == ACTION_DECLINE_CALL) {
            val callId = intent.getStringExtra(EXTRA_CALL_ID)
            val pairId = intent.getStringExtra(EXTRA_PAIR_ID)

            // Cancel notification
            ChatFirebaseMessagingService.cancelIncomingCallNotification(context)

            val app = context.applicationContext as? ChatApplication
            if (app != null && !pairId.isNullOrBlank() && !callId.isNullOrBlank()) {
                val pending = goAsync()
                app.applicationScope.launch {
                    try {
                        kotlinx.coroutines.withTimeout(8000) { app.apiClient.apiService.rejectCall(callId, pairId) }
                    } catch (e: Exception) {
                        Log.w("CallActionReceiver", "Could not decline call: ${e.message}")
                    } finally { pending.finish() }
                }
            }
        }
    }
}
