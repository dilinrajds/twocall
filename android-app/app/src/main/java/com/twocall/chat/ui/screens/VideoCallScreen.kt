package com.twocall.chat.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.twocall.chat.ui.theme.CallAcceptGreen
import com.twocall.chat.ui.theme.CallRejectRed
import com.twocall.chat.ui.theme.DarkPrimary
import com.twocall.chat.ui.viewmodel.CallViewModel
import com.twocall.chat.webrtc.CallState
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

    var localRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    var remoteRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    LaunchedEffect(callState) {
        if (callState == CallState.IDLE || callState == CallState.ENDED) {
            onCallEnded()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Remote Video (Full Screen)
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).also { renderer ->
                    remoteRenderer = renderer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Local Video Preview (Floating PiP top right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
                .size(110.dp, 160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.DarkGray)
        ) {
            AndroidView(
                factory = { context ->
                    SurfaceViewRenderer(context).also { renderer ->
                        localRenderer = renderer
                        remoteRenderer?.let { remote ->
                            viewModel.webRtcManager.initSurfaceViews(renderer, remote)
                        }
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
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            val statusText = when (callState) {
                CallState.OUTGOING_RINGING -> "Calling..."
                CallState.INCOMING_RINGING -> "Incoming Video Call..."
                CallState.CONNECTING -> "Connecting..."
                CallState.CONNECTED -> {
                    val min = durationSeconds / 60
                    val sec = durationSeconds % 60
                    "%02d:%02d".format(min, sec)
                }
                CallState.ENDED -> "Call Ended"
                else -> ""
            }

            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = DarkPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Bottom Controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 36.dp)
        ) {
            if (callState == CallState.INCOMING_RINGING) {
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
                        onClick = { viewModel.acceptCall(localRenderer) },
                        modifier = Modifier
                            .size(72.dp)
                            .background(CallAcceptGreen, CircleShape)
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
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
                    }

                    IconButton(
                        onClick = { viewModel.toggleVideo() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(if (!isVideoEnabled) Color.White else Color.White.copy(alpha = 0.2f), CircleShape)
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
                            .size(70.dp)
                            .background(CallRejectRed, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(34.dp))
                    }

                    IconButton(
                        onClick = { viewModel.toggleMic() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(if (isMicMuted) Color.White else Color.White.copy(alpha = 0.2f), CircleShape)
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
                            .background(if (isSpeakerOn) DarkPrimary else Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Speaker", tint = Color.White)
                    }
                }
            }
        }
    }
}
