package com.twocall.chat.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.data.local.entity.MessageEntity
import com.twocall.chat.ui.components.*
import com.twocall.chat.ui.theme.DarkBackground
import com.twocall.chat.ui.theme.DarkPrimary
import com.twocall.chat.ui.theme.OnlineGreen
import com.twocall.chat.ui.viewmodel.ChatViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: ChatViewModel,
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val conversation by viewModel.conversation.collectAsState()
    val replyingTo by viewModel.replyingTo.collectAsState()

    val isRecording by viewModel.voiceRecorder.isRecording.collectAsState()
    val recordingDuration by viewModel.voiceRecorder.durationSeconds.collectAsState()
    val recordingAmplitude by viewModel.voiceRecorder.currentAmplitude.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var selectedMessageForOptions by remember { mutableStateOf<MessageEntity?>(null) }

    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Media file pickers
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            copyUriToFile(context, it, "photo_${System.currentTimeMillis()}.jpg")?.let { file ->
                viewModel.sendAttachment(file, "image/jpeg", "IMAGE")
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            copyUriToFile(context, it, "video_${System.currentTimeMillis()}.mp4")?.let { file ->
                viewModel.sendAttachment(file, "video/mp4", "VIDEO")
            }
        }
    }

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            copyUriToFile(context, it, "document_${System.currentTimeMillis()}.pdf")?.let { file ->
                viewModel.sendAttachment(file, "application/pdf", "DOCUMENT")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = DarkPrimary, modifier = Modifier.size(22.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Partner",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isOnline = conversation?.partnerOnline == true
                                val isTyping = conversation?.partnerTyping == true

                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (isOnline) OnlineGreen else Color.Gray, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isTyping) "typing..." else if (isOnline) "Online" else "Offline",
                                    fontSize = 11.sp,
                                    color = if (isTyping) DarkPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onAudioCallClick) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Audio Call", tint = DarkPrimary)
                    }
                    IconButton(onClick = onVideoCallClick) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = "Video Call", tint = DarkPrimary)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val replyMsg = msg.replyToMessageId?.let { replyId ->
                        messages.find { it.id == replyId }
                    }

                    ChatBubble(
                        message = msg,
                        replyMessage = replyMsg,
                        voicePlayer = viewModel.voicePlayer,
                        onLongClick = { selectedMessageForOptions = msg },
                        onReplyClick = { viewModel.setReplyingTo(msg) }
                    )
                }
            }

            // Replying Banner
            if (replyingTo != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Replying to ${if (replyingTo!!.isOutgoing) "yourself" else "partner"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkPrimary
                            )
                            Text(
                                text = replyingTo!!.plaintext.take(40),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = { viewModel.setReplyingTo(null) }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel reply", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Input Bar / Recording Bar
            if (isRecording) {
                VoiceRecordingBar(
                    durationSeconds = recordingDuration,
                    amplitude = recordingAmplitude,
                    onCancel = { viewModel.cancelVoiceRecording() },
                    onSend = { viewModel.stopAndSendVoiceRecording() }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showAttachmentSheet = true },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Attach", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            viewModel.onTypingChanged(it)
                        },
                        placeholder = { Text("Encrypted message...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        maxLines = 4
                    )

                    if (inputText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.sendText(inputText)
                                inputText = ""
                                viewModel.onTypingChanged("")
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(DarkPrimary, CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.startVoiceRecording() },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = "Record Voice Note", tint = DarkPrimary)
                        }
                    }
                }
            }
        }
    }

    // Attachment Picker Bottom Sheet
    if (showAttachmentSheet) {
        AttachmentBottomSheet(
            onDismiss = { showAttachmentSheet = false },
            onPickImage = { imagePicker.launch("image/*") },
            onPickVideo = { videoPicker.launch("video/*") },
            onPickDocument = { docPicker.launch("application/*") }
        )
    }

    // Message Long-Press Context Menu (Reactions, Reply, Delete)
    selectedMessageForOptions?.let { msg ->
        AlertDialog(
            onDismissRequest = { selectedMessageForOptions = null },
            title = { Text("Message Actions") },
            text = {
                Column {
                    // Emoji reactions bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("❤️", "👍", "😂", "😮", "🙏", "🔥").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.addReaction(msg.id, emoji)
                                        selectedMessageForOptions = null
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Reply") },
                        leadingContent = { Icon(imageVector = Icons.Default.Reply, contentDescription = null) },
                        modifier = Modifier.clickable {
                            viewModel.setReplyingTo(msg)
                            selectedMessageForOptions = null
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Delete for Everyone", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            viewModel.deleteMessage(msg.id)
                            selectedMessageForOptions = null
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }
}

private fun copyUriToFile(context: android.content.Context, uri: Uri, fileName: String): File? {
    return try {
        val destFile = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile
    } catch (e: Exception) {
        null
    }
}
