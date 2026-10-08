package com.twocall.chat.ui.components

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.twocall.chat.ChatApplication

@Composable
fun NotificationSettings(app: ChatApplication) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Calls & notifications", style = MaterialTheme.typography.titleMedium)
            val configured = com.google.firebase.FirebaseApp.getApps(app).isNotEmpty()
            Text(if (configured) "Allow notifications to receive message and call alerts."
                else "Background alerts need Firebase configuration in this app build and on the server.")
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, app.packageName))
            }) { Text("Notification settings") }
            if (Build.VERSION.SDK_INT >= 34) TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    android.net.Uri.parse("package:${app.packageName}")))
            }) { Text("Allow full-screen incoming calls") }
        }
    }
}
