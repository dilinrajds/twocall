package com.twocall.chat.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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

    // Betta Fin-inspired organic asymmetrical curvature
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(24.dp, 24.dp, 6.dp, 24.dp)
    } else {
        RoundedCornerShape(24.dp, 24.dp, 24.dp, 6.dp)
    }

    // Gentle entrance: small fade + slight upward motion + subtle scale
    val scaleAnim = remember { Animatable(0.92f) }
    val alphaAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(14f) }

    LaunchedEffect(message.id) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    LaunchedEffect(message.id) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(240, easing = LinearOutSlowInEasing)
        )
    }
    LaunchedEffect(message.id) {
        offsetYAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(240, easing = FastOutSlowInEasing)
        )
    }

    // Standalone love emoji pulse
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
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .offset(y = offsetYAnim.value.dp)
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
                    .shadow(
                        elevation = if (isOutgoing) 8.dp else 4.dp,
                        shape = bubbleShape,
                        spotColor = if (isOutgoing) AquaCyan.copy(alpha = 0.35f) else NeumorphicBottomShadow
                    )
                    .clip(bubbleShape)
                    .then(
                        if (isOutgoing) {
                            Modifier.background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0077B6), Color(0xFF0096C7), Color(0xFF03045E))
                                )
                            )
                        } else {
                            Modifier.background(NeumorphicBaseDark)
                        }
                    )
                    .border(
                        width = 1.dp,
                        brush = if (isOutgoing) {
                            Brush.linearGradient(
                                listOf(AquaCyan.copy(alpha = 0.6f), OceanIndigo.copy(alpha = 0.2f))
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(GlassBorderDark, Color.Transparent)
                            )
                        },
                        shape = bubbleShape
                    )
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick
                    )
                    .padding(13.dp)
            ) {
                Column {
                    // Reply Quote preview
                    if (replyMessage != null) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.28f),
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
                                        .background(AquaCyan, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (replyMessage.isOutgoing) "You" else "Partner",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AquaCyan
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

                    // Content Rendering
                    if (message.isDeleted) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = TextMutedDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "This message was deleted",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMutedDark
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
                            color = Color.LightGray.copy(alpha = 0.75f)
                        )
                        if (isOutgoing) {
                            Spacer(modifier = Modifier.width(4.dp))
                            DeliveryStatusTick(status = message.status)
                        }
                    }
                }
            }

            // Emoji Reaction Badge
            if (message.reactionEmoji != null) {
                Surface(
                    color = AquaticSurface,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AquaCyan.copy(alpha = 0.3f)),
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
                .background(Brush.linearGradient(listOf(AquaCyan, BettaViolet)), CircleShape)
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
                    .width(130.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AquaCyan,
                trackColor = Color.White.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            ZippyWaveform(
                modifier = Modifier.width(130.dp),
                height = 16.dp,
                barCount = 18,
                isRecording = isThisPlaying,
                amplitude = if (isThisPlaying) 0.8f else 0.2f,
                primaryColor = AquaCyan,
                secondaryColor = BettaViolet
            )
        }
    }
}
