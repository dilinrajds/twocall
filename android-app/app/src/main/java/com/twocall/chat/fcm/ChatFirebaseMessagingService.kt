package com.twocall.chat.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.twocall.chat.ChatApplication
import com.twocall.chat.MainActivity

class ChatFirebaseMessagingService : FirebaseMessagingService() {

    private val tag = "FCMService"

    companion object {
        const val CHANNEL_MESSAGES = "twocall_messages"
        const val CHANNEL_INCOMING_CALLS = "twocall_incoming_calls"
        const val NOTIFICATION_ID_CALL = 201

        fun cancelIncomingCallNotification(context: Context) {
            try {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.cancel(NOTIFICATION_ID_CALL)
            } catch (ignored: Exception) {}
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(tag, "FCM token refreshed")
        val app = applicationContext as? ChatApplication ?: return
        app.registerPushToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(tag, "FCM Message received: ${remoteMessage.data}")

        val data = remoteMessage.data
        val type = data["type"] ?: return
        val pairId = data["pairId"]

        when (type) {
            "NEW_MESSAGE" -> {
                com.twocall.chat.worker.MessageSyncWorker.enqueue(this)
                val senderLabel = data["senderLabel"]
                showNewMessageNotification(pairId, senderLabel)
                // Trigger background message synchronization
                val app = applicationContext as? ChatApplication
                app?.syncMessagesInBackground()
            }
            "INCOMING_CALL" -> {
                val callId = data["callId"]
                val callType = data["callType"] ?: "AUDIO"
                val callerDeviceId = data["callerDeviceId"]
                val callerLabel = data["callerLabel"]
                showIncomingCallNotification(callId, pairId, callType, callerDeviceId, callerLabel)
            }
            "MISSED_CALL" -> {
                showMissedCallNotification(pairId)
            }
            "CALL_ENDED" -> {
                val activeId = getSharedPreferences("call_alert", MODE_PRIVATE).getString("callId", null)
                if (activeId == data["callId"]) cancelIncomingCallNotification(this)
            }
        }
    }

    private fun showNewMessageNotification(pairId: String?, senderLabel: String?) {
        createChannels()
        val title = if (!senderLabel.isNullOrBlank()) senderLabel else "ZippyCall"
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            pairId?.let { putExtra("openPairId", it) }
        }
        val reqCode = if (!pairId.isNullOrBlank()) pairId.hashCode() else 100
        val pendingIntent = PendingIntent.getActivity(
            this, reqCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(com.twocall.chat.R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("New encrypted message")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 200, 250))
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_SOUND)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = if (!pairId.isNullOrBlank()) Math.abs(pairId.hashCode()) % 100000 else (System.currentTimeMillis() % 100000).toInt()
        manager.notify(notifId, notification)
    }

    private fun showIncomingCallNotification(
        callId: String?,
        pairId: String?,
        callType: String,
        callerDeviceId: String?,
        callerLabel: String?
    ) {
        createChannels()
        val callerName = if (!callerLabel.isNullOrBlank()) callerLabel else "Partner"
        getSharedPreferences("call_alert", MODE_PRIVATE).edit().putString("callId", callId).apply()
        val isVideo = "VIDEO".equals(callType, ignoreCase = true)
        val callTypeText = if (isVideo) "Incoming Video Call" else "Incoming Audio Call"

        // Intent to open incoming call UI (Full Screen & Tap)
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("incomingCall", true)
            putExtra("callId", callId)
            putExtra("pairId", pairId)
            putExtra("callType", callType)
            putExtra("callerDeviceId", callerDeviceId)
            putExtra("callerLabel", callerName)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 200, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Accept Action Intent
        val acceptIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("incomingCall", true)
            putExtra("acceptCall", true)
            putExtra("callId", callId)
            putExtra("pairId", pairId)
            putExtra("callType", callType)
            putExtra("callerDeviceId", callerDeviceId)
            putExtra("callerLabel", callerName)
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            this, 202, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline Action Broadcast Intent
        val declineIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_DECLINE_CALL
            putExtra(CallActionReceiver.EXTRA_CALL_ID, callId)
            putExtra(CallActionReceiver.EXTRA_PAIR_ID, pairId)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            this, 203, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callerPerson = Person.Builder()
            .setName(callerName)
            .setImportant(true)
            .build()

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val builder = NotificationCompat.Builder(this, CHANNEL_INCOMING_CALLS)
            .setSmallIcon(com.twocall.chat.R.drawable.ic_notification)
            .setContentTitle(callerName)
            .setContentText(callTypeText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSound(ringtoneUri)
            .setAutoCancel(true)
            .setOngoing(true)
            .setTimeoutAfter(60000L)
            .addPerson(callerPerson)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    callerPerson,
                    declinePendingIntent,
                    acceptPendingIntent
                )
            )
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_CALL, builder.build())
    }

    private fun showMissedCallNotification(pairId: String?) {
        createChannels()
        val intent = Intent(this, MainActivity::class.java).apply {
            pairId?.let { putExtra("openPairId", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 300, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(com.twocall.chat.R.drawable.ic_notification)
            .setContentTitle("ZippyCall • Missed Call")
            .setContentText("You missed a private call")
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
                enableVibration(true)
                enableLights(true)
            }

            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val callChannel = NotificationChannel(
                CHANNEL_INCOMING_CALLS,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming voice and video call ring notifications"
                setSound(ringtoneUri, audioAttributes)
                enableVibration(true)
                enableLights(true)
            }

            manager.createNotificationChannel(msgChannel)
            manager.createNotificationChannel(callChannel)
        }
    }
}
