package com.twocall.chat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.twocall.chat.audio.VoicePlayer
import com.twocall.chat.data.local.entity.MessageEntity
import com.twocall.chat.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: MessageEntity,
    replyMessage: MessageEntity? = null,
    voicePlayer: VoicePlayer,
    onLongClick: () -> Unit,
    onReplyClick: () -> Unit,
    onImageClick: (File) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isOutgoing = message.isOutgoing
    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart

    val bubbleColor = if (isOutgoing) OutgoingBubbleDark else IncomingBubbleDark
    val borderColor = if (isOutgoing) OutgoingBubbleBorder else IncomingBubbleBorder

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(22.dp, 22.dp, 4.dp, 22.dp)
    } else {
        RoundedCornerShape(22.dp, 22.dp, 22.dp, 4.dp)
    }

    // Modern Spring Entrance Scale & Alpha Animation
    val scaleAnim = remember { Animatable(0.88f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(message.id) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    LaunchedEffect(message.id) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(220, easing = LinearOutSlowInEasing)
        )
    }

    // Check for standalone love emoji for enlarged heart pulse animation
    val isLoveEmojiOnly = remember(message.plaintext) {
        containsLoveEmoji(message.plaintext) && message.plaintext.trim().length <= 6
    }

    val infiniteTransition = rememberInfiniteTransition(label = "loveHeartPulse")
    val heartPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .scale(scaleAnim.value)
            .alpha(alphaAnim.value),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 295.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(bubbleColor)
                    .border(1.dp, borderColor, bubbleShape)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick
                    )
                    .padding(12.dp)
            ) {
                Column {
                    // Reply Quote preview if replying to another message
                    if (replyMessage != null) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(modifier = Modifier.padding(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(30.dp)
                                        .background(NeonCyan, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (replyMessage.isOutgoing) "You" else "Partner",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan
                                    )
                                    Text(
                                        text = replyMessage.plaintext.take(45),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Content Rendering based on MessageType
                    if (message.isDeleted) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "This message was deleted",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    } else {
                        when (message.messageType) {
                            "TEXT" -> {
                                if (isLoveEmojiOnly) {
                                    Text(
                                        text = message.plaintext,
                                        fontSize = 38.sp,
                                        modifier = Modifier
                                            .scale(heartPulseScale)
                                            .padding(vertical = 4.dp, horizontal = 6.dp)
                                    )
                                } else {
                                    Text(
                                        text = message.plaintext,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White
                                    )
                                }
                            }
                            "IMAGE" -> {
                                val imgFile = message.attachmentLocalPath?.let { File(it) }
                                if (imgFile != null && imgFile.exists()) {
                                    AsyncImage(
                                        model = imgFile,
                                        contentDescription = "Image Attachment",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .combinedClickable(
                                                onClick = { onImageClick(imgFile) },
                                                onLongClick = onLongClick
                                            )
                                    )
                                } else {
                                    Text(
                                        text = "📷 Photo",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White
                                    )
                                }
                            }
                            "AUDIO" -> {
                                VoiceMessageBubble(message = message, voicePlayer = voicePlayer)
                            }
                            else -> {
                                Text(
                                    text = "📄 ${message.attachmentFileName ?: "Document"}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Timestamp and Delivery Status Tick
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
                        Text(
                            text = timeStr,
                            fontSize = 10.sp,
                            color = Color.LightGray.copy(alpha = 0.8f)
                        )
                        if (isOutgoing) {
                            Spacer(modifier = Modifier.width(4.dp))
                            DeliveryStatusTick(status = message.status)
                        }
                    }
                }
            }

            // Emoji Reaction Badge with Pop-in Animation
            if (message.reactionEmoji != null) {
                Surface(
                    color = Color(0x661E293B),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier.offset(y = (-10).dp, x = if (isOutgoing) (-4).dp else 4.dp)
                ) {
                    Text(
                        text = message.reactionEmoji,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceMessageBubble(message: MessageEntity, voicePlayer: VoicePlayer) {
    val isPlaying by voicePlayer.isPlaying.collectAsState()
    val playingPath by voicePlayer.currentPlayingPath.collectAsState()
    val progress by voicePlayer.progressFraction.collectAsState()

    val isThisPlaying = isPlaying && playingPath == message.attachmentLocalPath

    val infiniteTransition = rememberInfiniteTransition(label = "voiceWave")
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse), label = "w1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "w2"
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse), label = "w3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        IconButton(
            onClick = {
                message.attachmentLocalPath?.let { path ->
                    voicePlayer.play(path)
                }
            },
            modifier = Modifier
                .size(38.dp)
                .background(Brush.linearGradient(listOf(NeonCyan, NeonIndigo)), CircleShape)
        ) {
            Icon(
                imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isThisPlaying) "Pause" else "Play",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            LinearProgressIndicator(
                progress = { if (isThisPlaying) progress else 0f },
                modifier = Modifier
                    .width(120.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = NeonCyan,
                trackColor = Color.White.copy(alpha = 0.25f)
            )

            if (isThisPlaying) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(wave1, wave2, wave3, wave1).forEach { w ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((12 * w).dp)
                                .background(NeonCyan, RoundedCornerShape(1.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Playing...",
                        fontSize = 9.sp,
                        color = NeonCyan
                    )
                }
            }
        }
    }
}
