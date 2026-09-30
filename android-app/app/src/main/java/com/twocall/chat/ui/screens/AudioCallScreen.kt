package com.twocall.chat.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.ui.components.AnimatedGlassBackground
import com.twocall.chat.ui.components.GlassCard
import com.twocall.chat.ui.theme.*
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

    // Dynamic wave pulse animation for audio visualizer
    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )

    AnimatedGlassBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp)
            ) {
                // Header Call Info
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GlassCard(
                        shape = RoundedCornerShape(20.dp),
                        borderColor = NeonCyan.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "End-to-End Encrypted Call",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Partner",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 34.sp
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val statusText = when (callState) {
                        CallState.OUTGOING_RINGING -> "Ringing..."
                        CallState.INCOMING_RINGING -> "Incoming Audio Call..."
                        CallState.CONNECTING -> "Establishing Secure Channel..."
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
                        color = if (callState == CallState.CONNECTED) NeonEmerald else Color.LightGray,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Avatar Visualizer Rings
                Box(contentAlignment = Alignment.Center) {
                    if (callState == CallState.CONNECTED || callState == CallState.OUTGOING_RINGING || callState == CallState.INCOMING_RINGING) {
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .scale(pulseScale2)
                                .background(NeonCyan.copy(alpha = 0.08f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .scale(pulseScale1)
                                .background(NeonCyan.copy(alpha = 0.16f), CircleShape)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(NeonCyan, NeonIndigo)
                                )
                            )
                            .border(3.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Partner Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(72.dp)
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
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(CallRejectRed)
                        ) {
                            Icon(imageVector = Icons.Default.CallEnd, contentDescription = "Reject", tint = Color.White, modifier = Modifier.size(34.dp))
                        }

                        IconButton(
                            onClick = { viewModel.acceptCall() },
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(CallAcceptGreen)
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                    }
                } else {
                    // Active / Outgoing controls: Mute, End, Speaker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleMic() },
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(if (isMicMuted) Color.White else Color(0x33FFFFFF))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute",
                                tint = if (isMicMuted) Color.Black else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.endCall() },
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(CallRejectRed)
                        ) {
                            Icon(imageVector = Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        IconButton(
                            onClick = { viewModel.toggleSpeaker() },
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(if (isSpeakerOn) NeonCyan else Color(0x33FFFFFF))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                contentDescription = "Speaker",
                                tint = if (isSpeakerOn) Color.Black else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
