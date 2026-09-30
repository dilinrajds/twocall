package com.twocall.chat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    modifier: Modifier = Modifier
) {
    val isOutgoing = message.isOutgoing
    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart

    val bubbleColor = if (isOutgoing) {
        OutgoingBubbleDark
    } else {
        IncomingBubbleDark
    }

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(bubbleColor)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick
                    )
                    .padding(10.dp)
            ) {
                Column {
                    // Reply Quote preview if replying to another message
                    if (replyMessage != null) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(modifier = Modifier.padding(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(28.dp)
                                        .background(DarkPrimary, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (replyMessage.isOutgoing) "You" else "Partner",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DarkPrimary
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
                    when (message.messageType) {
                        "TEXT" -> {
                            Text(
                                text = if (message.isDeleted) "This message was deleted" else message.plaintext,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (message.isDeleted) Color.Gray else Color.White
                            )
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
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
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

                    Spacer(modifier = Modifier.height(4.dp))

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

            // Emoji Reaction Badge
            if (message.reactionEmoji != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.offset(y = (-8).dp, x = if (isOutgoing) (-4).dp else 4.dp)
                ) {
                    Text(
                        text = message.reactionEmoji,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                .size(36.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(
                imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isThisPlaying) "Pause" else "Play",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        LinearProgressIndicator(
            progress = { if (isThisPlaying) progress else 0f },
            modifier = Modifier
                .width(130.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = DarkPrimary,
            trackColor = Color.White.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Voice",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
    }
}
