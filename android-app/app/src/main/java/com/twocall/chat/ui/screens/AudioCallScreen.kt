package com.twocall.chat.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.ui.components.*
import com.twocall.chat.ui.theme.*
import com.twocall.chat.ui.viewmodel.CallViewModel
import com.twocall.chat.webrtc.CallState
import kotlinx.coroutines.delay

@Composable
fun AudioCallScreen(
    viewModel: CallViewModel,
    onCallEnded: () -> Unit
) {
    val callState by viewModel.callState.collectAsState()
    val isMicMuted by viewModel.isMicMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val durationSeconds by viewModel.callDurationSeconds.collectAsState()
    val view = LocalView.current
    val profile by viewModel.partnerProfile.collectAsState()
    val autoAccept by viewModel.autoAccept.collectAsState()
    LaunchedEffect(autoAccept, callState) {
        if (autoAccept && callState == CallState.INCOMING_RINGING) viewModel.acceptCall()
    }

    LaunchedEffect(Unit) {
        viewModel.remoteCallEndedEvent.collect {
            onCallEnded()
        }
    }

    LaunchedEffect(callState) {
        if (callState == CallState.ENDED || callState == CallState.DECLINED || callState == CallState.FAILED) {
            onCallEnded()
        }
    }

    val isConnected = callState == CallState.CONNECTED
    val isIncoming = callState == CallState.INCOMING_RINGING
    val isOutgoing = callState == CallState.OUTGOING_CALLING || callState == CallState.CONNECTING || callState == CallState.ACCEPTING

    ZippyAquaticBackground {
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
                    .padding(vertical = 36.dp)
            ) {
                // Header Status Section
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ZippyNeumorphicCard(
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = AquaticSurface.copy(alpha = 0.5f),
                        borderStroke = 1.dp,
                        highlightColor = AquaCyan.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) LuminousTeal else AquaCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isConnected) "Encrypted Voice Channel" else "Private Call",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isConnected) LuminousTeal else AquaCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    ZippyAvatar(name = profile?.partnerDisplayName ?: "Partner", imageBase64 = profile?.partnerImageBase64,
                        size = 64.dp, showOnlineDot = false)

                    Text(
                        text = profile?.partnerDisplayName?.takeIf { it.isNotBlank() } ?: "Partner",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status / Timer Bar
                    AnimatedContent(
                        targetState = callState,
                        label = "call_state_text"
                    ) { state ->
                        val (statusText, statusColor) = when (state) {
                            CallState.OUTGOING_CALLING -> "Calling sanctuary..." to AquaCyan
                            CallState.INCOMING_RINGING -> "Incoming Private Call" to BiolumPink
                            CallState.ACCEPTING -> "Accepting private call..." to OceanIndigo
                            CallState.CONNECTING -> "Converging audio..." to OceanIndigo
                            CallState.CONNECTED -> {
                                val min = durationSeconds / 60
                                val sec = durationSeconds % 60
                                "%02d:%02d".format(min, sec) to LuminousTeal
                            }
                            CallState.DECLINED -> "Call Declined" to AquaticDecline
                            CallState.FAILED -> "Connection Failed" to AquaticDecline
                            CallState.RECONNECTING -> "Reconnecting..." to BettaViolet
                            CallState.ENDED -> "Call Ended" to TextMutedDark
                            else -> "" to TextMutedDark
                        }

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleMedium,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (state == CallState.CONNECTED) 20.sp else 15.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Center Aquatic Visualization Area
                Box(
                    modifier = Modifier.size(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isConnected -> {
                            // ACTIVE VOICE CALL: Beautiful alive Betta fish + concentric water ripples
                            ZippyConcentricRipples(
                                modifier = Modifier.fillMaxSize(),
                                baseColor = LuminousTeal,
                                rippleCount = 3,
                                maxRadius = 120.dp
                            )
                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(AquaticSurface.copy(alpha = 0.85f), NeumorphicBaseDark)
                                        )
                                    )
                                    .border(2.dp, Brush.linearGradient(listOf(LuminousTeal, AquaCyan)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                ZippyBettaFish(
                                    modifier = Modifier.size(150.dp),
                                    primaryColor = AquaCyan,
                                    secondaryColor = OceanIndigo,
                                    accentColor = BiolumPink,
                                    audioLevel = 0.45f
                                )
                            }
                        }
                        isIncoming -> {
                            // INCOMING CALL: Concentric pulsing ripples + glowing partner Betta halo
                            ZippyConcentricRipples(
                                modifier = Modifier.fillMaxSize(),
                                baseColor = AquaCyan,
                                rippleCount = 3,
                                maxRadius = 125.dp
                            )
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(AquaCyan.copy(alpha = 0.3f), NeumorphicBaseDark)
                                        )
                                    )
                                    .border(2.dp, Brush.linearGradient(listOf(AquaCyan, BettaViolet)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                ZippyBettaFish(
                                    modifier = Modifier.size(135.dp),
                                    primaryColor = AquaCyan,
                                    secondaryColor = BiolumPink
                                )
                            }
                        }
                        isOutgoing -> {
                            // OUTGOING CALL: Twin Betta fish orbiting & swimming towards each other
                            ZippyConcentricRipples(
                                modifier = Modifier.fillMaxSize(),
                                baseColor = BettaViolet,
                                rippleCount = 2,
                                maxRadius = 115.dp
                            )
                            ZippyTwinBettaEncounter(
                                modifier = Modifier.size(240.dp),
                                progress = 0.5f
                            )
                        }
                        else -> {
                            // Call Ended / Idle
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(NeumorphicBaseDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = null,
                                    tint = TextMutedDark,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom Action Controls Container
                if (isIncoming) {
                    // Incoming Call: Decline & Accept Neumorphic Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decline Button (Soft Red Neumorphic)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ZippyCallButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                    viewModel.rejectCall()
                                },
                                icon = Icons.Default.CallEnd,
                                contentDescription = "Decline Call",
                                backgroundColor = AquaticDecline,
                                size = 74.dp,
                                iconSize = 34.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Decline", color = TextSecondaryDark, fontSize = 12.sp)
                        }

                        // Accept Button (Glowing Aqua / Green Neumorphic)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ZippyCallButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    viewModel.acceptCall()
                                },
                                icon = Icons.Default.Call,
                                contentDescription = "Accept Call",
                                backgroundColor = LuminousTeal,
                                size = 74.dp,
                                iconSize = 34.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Accept", color = LuminousTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Active Voice Call / Outgoing Controls: Mute, End Call, Speaker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute Mic Toggle
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ZippyNeumorphicIconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    viewModel.toggleMic()
                                },
                                icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute",
                                size = 58.dp,
                                iconSize = 24.dp,
                                iconTint = if (isMicMuted) Color.White else AquaCyan,
                                isSelected = isMicMuted,
                                glowColor = BiolumPink.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isMicMuted) "Muted" else "Mute",
                                color = if (isMicMuted) Color.White else TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }

                        // End Call Button (Unmistakably Red)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ZippyCallButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                    viewModel.endCall()
                                    onCallEnded()
                                },
                                icon = Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                backgroundColor = AquaticDecline,
                                size = 76.dp,
                                iconSize = 36.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "End Call", color = AquaticDecline, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Speakerphone Toggle
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ZippyNeumorphicIconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    viewModel.toggleSpeaker()
                                },
                                icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                contentDescription = "Speaker",
                                size = 58.dp,
                                iconSize = 24.dp,
                                iconTint = if (isSpeakerOn) Color.White else AquaCyan,
                                isSelected = isSpeakerOn,
                                glowColor = AquaCyan.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isSpeakerOn) "Speaker" else "Earpiece",
                                color = if (isSpeakerOn) AquaCyan else TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
