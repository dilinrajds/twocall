package com.twocall.chat.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.ui.theme.*

@Composable
fun VoiceRecordingBar(
    durationSeconds: Int,
    amplitude: Int,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val infiniteTransition = rememberInfiniteTransition(label = "voice_recording_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    ZippyNeumorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(32.dp),
        backgroundColor = NeumorphicBaseDark,
        borderStroke = 1.2.dp,
        highlightColor = AquaCyan.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Cancel Button
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    onCancel()
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel Recording",
                    tint = AquaticDecline,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Pulsing recording indicator + Duration
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(pulseScale)
                        .background(BiolumPink, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val minutes = durationSeconds / 60
                val seconds = durationSeconds % 60
                Text(
                    text = "%02d:%02d".format(minutes, seconds),
                    color = Color.White,
                    fontSize = 13.sp
                )
            }

            // Flowing aquatic waveform
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                val normalizedAmp = (amplitude / 32767f).coerceIn(0.2f, 1f)
                ZippyWaveform(
                    height = 24.dp,
                    barCount = 18,
                    isRecording = true,
                    amplitude = normalizedAmp,
                    primaryColor = AquaCyan,
                    secondaryColor = BettaViolet
                )
            }

            // Send Button
            ZippyNeumorphicIconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onSend()
                },
                icon = Icons.Default.Send,
                contentDescription = "Send Voice Message",
                size = 42.dp,
                iconSize = 18.dp,
                backgroundColor = NeumorphicBaseDark,
                iconTint = Color.White,
                isSelected = true
            )
        }
    }
}
