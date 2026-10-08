package com.twocall.chat.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.twocall.chat.ui.components.*
import com.twocall.chat.ui.theme.*
import com.twocall.chat.ui.viewmodel.CallViewModel
import com.twocall.chat.webrtc.CallState
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer

@Composable
fun VideoCallScreen(
    viewModel: CallViewModel,
    onCallEnded: () -> Unit
) {
    val callState by viewModel.callState.collectAsState()
    val isMicMuted by viewModel.isMicMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isVideoEnabled by viewModel.isVideoEnabled.collectAsState()
    val durationSeconds by viewModel.callDurationSeconds.collectAsState()
    val hasRemoteVideoFrame by viewModel.hasRemoteVideoFrame.collectAsState()
    val view = LocalView.current
    val profile by viewModel.partnerProfile.collectAsState()
    val autoAccept by viewModel.autoAccept.collectAsState()

    DisposableEffect(view, callState) {
        val previous = view.keepScreenOn
        view.keepScreenOn = callState !in listOf(CallState.IDLE, CallState.ENDED, CallState.DECLINED, CallState.FAILED)
        onDispose { view.keepScreenOn = previous }
    }

    var localRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    var remoteRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    LaunchedEffect(autoAccept, localRenderer, remoteRenderer, callState) {
        if (autoAccept && callState == CallState.INCOMING_RINGING && localRenderer != null && remoteRenderer != null) {
            viewModel.acceptCall(localRenderer, remoteRenderer)
        }
    }

    // Controls visibility auto-fade timer
    var areControlsVisible by remember { mutableStateOf(true) }
    var userInteractionTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

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

    // Auto-fade controls after 4 seconds of inactivity when connected
    LaunchedEffect(userInteractionTimestamp, callState) {
        if (callState == CallState.CONNECTED) {
            delay(4000)
            areControlsVisible = false
        } else {
            areControlsVisible = true
        }
    }

    // Auto initialize surface views once renderers are inflated.
    // BUG 1 FIX: Do NOT init (and thus do not start camera/mic) while the call is
    // still ringing. initSurfaceViews is only called after the user presses Accept
    // (state becomes ACCEPTING/CONNECTING/CONNECTED) or for outgoing calls.
    LaunchedEffect(localRenderer, remoteRenderer, callState) {
        val local = localRenderer
        val remote = remoteRenderer
        if (local != null && remote != null && callState != CallState.INCOMING_RINGING) {
            viewModel.webRtcManager.initSurfaceViews(local, remote)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                localRenderer?.release()
            } catch (ignored: Exception) {}
            try {
                remoteRenderer?.release()
            } catch (ignored: Exception) {}
        }
    }

    val isConnected = callState == CallState.CONNECTED
    val isIncoming = callState == CallState.INCOMING_RINGING

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                areControlsVisible = !areControlsVisible
                userInteractionTimestamp = System.currentTimeMillis()
            }
    ) {
        // 1. Remote Video Feed (Full Screen base)
        // BUG 2 FIX: Do NOT call setEnableHardwareScaler/setScalingType before init().
        // All renderer configuration is done inside WebRtcManager.initSurfaceViews()
        // after EGL context init. Calling them here (pre-init) is either a no-op or crash.
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).also { renderer ->
                    remoteRenderer = renderer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Pre-connection Betta Aquatic Visualizer
        // Smoothly fades away as soon as the first video frame renders!
        AnimatedVisibility(
            visible = !hasRemoteVideoFrame,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AbyssNavy),
                contentAlignment = Alignment.Center
            ) {
                ZippyConcentricRipples(
                    modifier = Modifier.size(260.dp),
                    baseColor = AquaCyan,
                    rippleCount = 3,
                    maxRadius = 120.dp
                )
                ZippyBettaFish(
                    modifier = Modifier.size(160.dp),
                    primaryColor = AquaCyan,
                    secondaryColor = OceanIndigo,
                    accentColor = BiolumPink
                )
            }
        }

        // 3. Floating Local Video Preview (Picture-in-Picture Neumorphic Card)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
                .size(118.dp, 168.dp)
                .shadow(elevation = 14.dp, shape = RoundedCornerShape(22.dp), spotColor = AquaCyan.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(22.dp))
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(AquaCyan, BettaViolet)),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            // BUG 2 FIX: Same as remote — no pre-init configuration calls.
            // setZOrderMediaOverlay/setMirror/setScalingType are applied in initSurfaceViews.
            AndroidView(
                factory = { context ->
                    SurfaceViewRenderer(context).also { renderer ->
                        localRenderer = renderer
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 4. Top Status Header (Fades with controls)
        AnimatedVisibility(
            visible = areControlsVisible || !isConnected,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ZippyAvatar(name = profile?.partnerDisplayName ?: "Partner", imageBase64 = profile?.partnerImageBase64,
                    size = 46.dp, showOnlineDot = false)
                Text(
                    text = profile?.partnerDisplayName?.takeIf { it.isNotBlank() } ?: "Partner",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                val statusText = when (callState) {
                    CallState.OUTGOING_CALLING -> "Calling sanctuary..."
                    CallState.INCOMING_RINGING -> "Incoming Video Call"
                    CallState.ACCEPTING -> "Accepting video call..."
                    CallState.CONNECTING -> "Establishing 1080p stream..."
                    CallState.CONNECTED -> {
                        val min = durationSeconds / 60
                        val sec = durationSeconds % 60
                        "%02d:%02d".format(min, sec)
                    }
                    CallState.DECLINED -> "Call Declined"
                    CallState.FAILED -> "Connection Failed"
                    CallState.RECONNECTING -> "Reconnecting..."
                    CallState.ENDED -> "Call Ended"
                    else -> ""
                }

                ZippyNeumorphicCard(
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = AquaticSurface.copy(alpha = 0.7f),
                    borderStroke = 1.dp
                ) {
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = when (callState) {
                            CallState.CONNECTED -> LuminousTeal
                            CallState.DECLINED, CallState.FAILED -> AquaticDecline
                            else -> AquaCyan
                        },
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 5. Bottom Controls Container (Translucent Neumorphic Toolbar)
        AnimatedVisibility(
            visible = areControlsVisible || isIncoming,
            enter = slideInVertically(tween(250)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            if (isIncoming) {
                // Incoming Call: Decline & Accept Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ZippyCallButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                viewModel.rejectCall()
                            },
                            icon = Icons.Default.CallEnd,
                            contentDescription = "Decline Call",
                            backgroundColor = AquaticDecline,
                            size = 72.dp,
                            iconSize = 34.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Decline", color = TextSecondaryDark, fontSize = 12.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ZippyCallButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                viewModel.acceptCall(localRenderer, remoteRenderer)
                            },
                            icon = Icons.Default.Videocam,
                            contentDescription = "Accept Video Call",
                            backgroundColor = LuminousTeal,
                            size = 72.dp,
                            iconSize = 34.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Accept", color = LuminousTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Active Call Neumorphic Glass Toolbar
                ZippyNeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    backgroundColor = NeumorphicBaseDark.copy(alpha = 0.88f),
                    borderStroke = 1.dp,
                    highlightColor = AquaCyan.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Switch Camera
                        ZippyNeumorphicIconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.switchCamera()
                                userInteractionTimestamp = System.currentTimeMillis()
                            },
                            icon = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            size = 48.dp,
                            iconSize = 22.dp,
                            iconTint = Color.White
                        )

                        // Video Toggle (Camera on/off)
                        ZippyNeumorphicIconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.toggleVideo()
                                userInteractionTimestamp = System.currentTimeMillis()
                            },
                            icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Toggle Video",
                            size = 48.dp,
                            iconSize = 22.dp,
                            iconTint = if (isVideoEnabled) AquaCyan else BiolumPink,
                            isSelected = !isVideoEnabled
                        )

                        // End Call
                        ZippyCallButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                viewModel.endCall()
                                onCallEnded()
                            },
                            icon = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            backgroundColor = AquaticDecline,
                            size = 64.dp,
                            iconSize = 30.dp
                        )

                        // Mute Mic Toggle
                        ZippyNeumorphicIconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.toggleMic()
                                userInteractionTimestamp = System.currentTimeMillis()
                            },
                            icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            size = 48.dp,
                            iconSize = 22.dp,
                            iconTint = if (isMicMuted) BiolumPink else Color.White,
                            isSelected = isMicMuted
                        )

                        // Speakerphone Toggle
                        ZippyNeumorphicIconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.toggleSpeaker()
                                userInteractionTimestamp = System.currentTimeMillis()
                            },
                            icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeDown,
                            contentDescription = "Speaker",
                            size = 48.dp,
                            iconSize = 22.dp,
                            iconTint = if (isSpeakerOn) LuminousTeal else Color.White,
                            isSelected = isSpeakerOn
                        )
                    }
                }
            }
        }
    }
}
