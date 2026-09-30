package com.twocall.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.twocall.chat.ui.theme.DarkBackground
import com.twocall.chat.ui.theme.DarkPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashLoadingScreen(
    isPaired: Boolean,
    onNavigateToWelcome: () -> Unit,
    onNavigateToConversation: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(800)
        if (isPaired) {
            onNavigateToConversation()
        } else {
            onNavigateToWelcome()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "TwoCall Private",
                tint = DarkPrimary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "TwoCall",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Private Messenger for Two",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                color = DarkPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
