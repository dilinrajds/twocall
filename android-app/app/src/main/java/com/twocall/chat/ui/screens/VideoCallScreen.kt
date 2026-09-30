package com.twocall.chat.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.twocall.chat.ui.theme.*
import com.twocall.chat.ui.viewmodel.CallViewModel
import com.twocall.chat.webrtc.CallState
import org.webrtc.SurfaceViewRenderer

@Composable
fun VideoCallScreen(
    viewModel: CallViewModel,
    onCallEnded: () -> Unit
) {
    val callState by viewModel.callState.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val isMicMuted by viewModel.isMicMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isVideoEnabled by viewModel.isVideoEnabled.collectAsState()
    val durationSeconds by viewModel.callDurationSeconds.collectAsState()

    var localRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    var remoteRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    LaunchedEffect(callState) {
        if (callState == CallState.IDLE || callState == CallState.ENDED) {
            onCallEnded()
        }
    }

    // Auto initialize surface views and start video call once renderers are inflated
    LaunchedEffect(localRenderer, remoteRenderer) {
        val local = localRenderer
        val remote = remoteRenderer
        if (local != null && remote != null) {
            viewModel.webRtcManager.initSurfaceViews(local, remote)
            if (callState == CallState.IDLE) {
                viewModel.startVideoCall(local, remote)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Remote Video Feed (Full Screen)
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).also { renderer ->
                    remoteRenderer = renderer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Local Video Preview (Floating PiP window with rounded glass border)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 52.dp, end = 16.dp)
                .size(115.dp, 165.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, Brush.linearGradient(listOf(NeonCyan, NeonIndigo)), RoundedCornerShape(20.dp))
                .background(Color.DarkGray)
        ) {
            AndroidView(
                factory = { context ->
                    SurfaceViewRenderer(context).also { renderer ->
                        localRenderer = renderer
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Status & Timer Bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Partner",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )

            val statusText = when (callState) {
                CallState.OUTGOING_RINGING -> "Calling..."
                CallState.INCOMING_RINGING -> "Incoming Video Call..."
                CallState.CONNECTING -> "Connecting Video..."
                CallState.CONNECTED -> {
                    val min = durationSeconds / 60
                    val sec = durationSeconds % 60
                    "%02d:%02d".format(min, sec)
                }
                CallState.ENDED -> "Call Ended"
                else -> ""
            }

            Surface(
                color = Color(0x66000000),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Bottom Controls Container (Glassmorphic Toolbar)
        Surface(
            color = Color(0x40101828),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 32.dp)
                .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)), RoundedCornerShape(32.dp))
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                if (callState == CallState.INCOMING_RINGING) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = { viewModel.rejectCall() },
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(CallRejectRed)
                        ) {
                            Icon(imageVector = Icons.Default.CallEnd, contentDescription = "Reject", tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        IconButton(
                            onClick = { viewModel.acceptCall(localRenderer, remoteRenderer) },
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(CallAcceptGreen)
                        ) {
                            Icon(imageVector = Icons.Default.Videocam, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.switchCamera() },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(imageVector = Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
                        }

                        IconButton(
                            onClick = { viewModel.toggleVideo() },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (!isVideoEnabled) Color.White else Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = "Toggle Video",
                                tint = if (!isVideoEnabled) Color.Black else Color.White
                            )
                        }

                        IconButton(
                            onClick = { viewModel.endCall() },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CallRejectRed)
                        ) {
                            Icon(imageVector = Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        IconButton(
                            onClick = { viewModel.toggleMic() },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (isMicMuted) Color.White else Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute",
                                tint = if (isMicMuted) Color.Black else Color.White
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleSpeaker() },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (isSpeakerOn) NeonCyan else Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Speaker", tint = if (isSpeakerOn) Color.Black else Color.White)
                        }
                    }
                }
            }
        }
    }
}
