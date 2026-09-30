package com.twocall.chat.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.ui.theme.CallAcceptGreen
import com.twocall.chat.ui.theme.CallRejectRed
import com.twocall.chat.ui.theme.DarkBackground
import com.twocall.chat.ui.theme.DarkPrimary
import com.twocall.chat.ui.viewmodel.CallViewModel
import com.twocall.chat.webrtc.CallState

@Composable
fun AudioCallScreen(
    viewModel: CallViewModel,
    onCallEnded: () -> Unit
) {
    val callState by viewModel.callState.collectAsState()
    val isMicMuted by viewModel.isMicMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val durationSeconds by viewModel.callDurationSeconds.collectAsState()

    LaunchedEffect(callState) {
        if (callState == CallState.IDLE || callState == CallState.ENDED) {
            onCallEnded()
        }
    }

    // Pulse animation for ringing
    val infiniteTransition = rememberInfiniteTransition(label = "ringPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(vertical = 48.dp)
        ) {
            // Header Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Private Audio Call",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Partner",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))

                val statusText = when (callState) {
                    CallState.OUTGOING_RINGING -> "Ringing..."
                    CallState.INCOMING_RINGING -> "Incoming Call..."
                    CallState.CONNECTING -> "Connecting WebRTC..."
                    CallState.CONNECTED -> {
                        val min = durationSeconds / 60
                        val sec = durationSeconds % 60
                        "%02d:%02d".format(min, sec)
                    }
                    CallState.ENDED -> "Call Ended"
                    else -> ""
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (callState == CallState.CONNECTED) DarkPrimary else Color.LightGray
                )
            }

            // Avatar
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(if (callState == CallState.OUTGOING_RINGING || callState == CallState.INCOMING_RINGING) pulseScale else 1f)
                    .background(DarkPrimary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(DarkPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Partner Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            // Call Action Controls
            if (callState == CallState.INCOMING_RINGING) {
                // Incoming Call: Accept / Reject buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { viewModel.rejectCall() },
                        modifier = Modifier
                            .size(72.dp)
                            .background(CallRejectRed, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.CallEnd, contentDescription = "Reject", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    IconButton(
                        onClick = { viewModel.acceptCall() },
                        modifier = Modifier
                            .size(72.dp)
                            .background(CallAcceptGreen, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            } else {
                // In-Call / Outgoing controls: Mute, End, Speaker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.toggleMic() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(if (isMicMuted) Color.White else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = if (isMicMuted) Color.Black else Color.White
                        )
                    }

                    IconButton(
                        onClick = { viewModel.endCall() },
                        modifier = Modifier
                            .size(72.dp)
                            .background(CallRejectRed, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(36.dp))
                    }

                    IconButton(
                        onClick = { viewModel.toggleSpeaker() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(if (isSpeakerOn) DarkPrimary else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            contentDescription = "Speaker",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
