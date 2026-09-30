package com.twocall.chat.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.twocall.chat.ChatApplication
import com.twocall.chat.MainActivity

class ChatFirebaseMessagingService : FirebaseMessagingService() {

    private val tag = "FCMService"

    companion object {
        const val CHANNEL_MESSAGES = "twocall_messages"
        const val CHANNEL_INCOMING_CALLS = "twocall_incoming_calls"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(tag, "FCM Token refreshed: $token")
        val app = applicationContext as? ChatApplication ?: return
        app.registerPushToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(tag, "FCM Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val type = data["type"] ?: return

        when (type) {
            "NEW_MESSAGE" -> {
                showNewMessageNotification()
                // Trigger background message synchronization
                val app = applicationContext as? ChatApplication
                app?.syncMessagesInBackground()
            }
            "INCOMING_CALL" -> {
                val callId = data["callId"]
                val callType = data["callType"] ?: "AUDIO"
                val callerDeviceId = data["callerDeviceId"]
                showIncomingCallNotification(callId, callType, callerDeviceId)
            }
            "MISSED_CALL" -> {
                showMissedCallNotification()
            }
        }
    }

    private fun showNewMessageNotification() {
        createChannels()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("New Encrypted Message")
            .setContentText("Tap to open conversation")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(101, notification)
    }

    private fun showIncomingCallNotification(callId: String?, callType: String, callerDeviceId: String?) {
        createChannels()
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("incomingCall", true)
            putExtra("callId", callId)
            putExtra("callType", callType)
            putExtra("callerDeviceId", callerDeviceId)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 200, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val notification = NotificationCompat.Builder(this, CHANNEL_INCOMING_CALLS)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentTitle("Incoming $callType Call")
            .setContentText("Partner is calling you...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSound(ringtoneUri)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(201, notification)
    }

    private fun showMissedCallNotification() {
        createChannels()
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 300, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.stat_notify_missed_call)
            .setContentTitle("Missed Call")
            .setContentText("You missed a call from your partner")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(301, notification)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val msgChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Encrypted private message notifications"
            }

            val callChannel = NotificationChannel(
                CHANNEL_INCOMING_CALLS,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming voice and video call ring notifications"
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), null)
            }

            manager.createNotificationChannel(msgChannel)
            manager.createNotificationChannel(callChannel)
        }
    }
}
