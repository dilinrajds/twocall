package com.twocall.chat.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.data.local.entity.MessageEntity
import com.twocall.chat.ui.components.*
import com.twocall.chat.ui.theme.*
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

    // Image Preview & Full View Modal state
    var selectedImageForPreview by remember { mutableStateOf<File?>(null) }
    var isSendingImageMode by remember { mutableStateOf(false) }
    var imageCaption by remember { mutableStateOf("") }
    var fullViewerImageModel by remember { mutableStateOf<Any?>(null) }

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
                selectedImageForPreview = file
                isSendingImageMode = true
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

    var loveAnimationTrigger by remember { mutableIntStateOf(0) }

    // Collect real-time love animation events emitted from repository (incoming/outgoing messages & reactions)
    LaunchedEffect(Unit) {
        viewModel.loveAnimationEvents.collect {
            loveAnimationTrigger++
        }
    }

    // Breathing pulse for online indicator
    val infinitePresenceTransition = rememberInfiniteTransition(label = "presencePulse")
    val presenceScale by infinitePresenceTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "presenceScale"
    )

    AnimatedGlassBackground {
        // Floating Love Hearts Cute Animation Overlay
        LoveHeartsOverlay(triggerCount = loveAnimationTrigger)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(NeonCyan.copy(alpha = 0.3f), NeonPurple.copy(alpha = 0.3f))
                                        )
                                    )
                                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Partner",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isOnline = conversation?.partnerOnline == true
                                    val isTyping = conversation?.partnerTyping == true

                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(if (isOnline) OnlineGreen else OfflineGray, CircleShape)
                                            .run {
                                                if (isOnline) this.scale(presenceScale) else this
                                            }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isTyping) "typing..." else if (isOnline) "Online" else "Offline",
                                        fontSize = 11.sp,
                                        color = if (isTyping) NeonCyan else DarkOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onAudioCallClick,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x20FFFFFF))
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "Audio Call", tint = NeonCyan)
                        }
                        IconButton(
                            onClick = onVideoCallClick,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x20FFFFFF))
                        ) {
                            Icon(imageVector = Icons.Default.Videocam, contentDescription = "Video Call", tint = NeonIndigo)
                        }
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x20FFFFFF))
                        ) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Settings", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
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
                            onReplyClick = { viewModel.setReplyingTo(msg) },
                            onImageClick = { imgFile ->
                                fullViewerImageModel = imgFile
                            }
                        )
                    }
                }

                // Replying Banner
                AnimatedVisibility(
                    visible = replyingTo != null,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    if (replyingTo != null) {
                        Surface(
                            color = Color(0x3B1A2436),
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Replying to ${if (replyingTo!!.isOutgoing) "yourself" else "partner"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = replyingTo!!.plaintext.take(45),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray,
                                        maxLines = 1
                                    )
                                }
                                IconButton(onClick = { viewModel.setReplyingTo(null) }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel reply", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                // Input Bar / Voice Recorder Bar (Frosted Glass Container)
                Surface(
                    color = Color(0x2B1E293B),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
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
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showAttachmentSheet = true },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x20FFFFFF))
                            ) {
                                Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Attach", tint = NeonCyan)
                            }

                            OutlinedTextField(
                                value = inputText,
                                onValueChange = {
                                    inputText = it
                                    viewModel.onTypingChanged(it)
                                },
                                placeholder = { Text("Encrypted message...", color = DarkOnSurfaceVariant) },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                maxLines = 4
                            )

                            if (inputText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        if (containsLoveEmoji(inputText)) {
                                            loveAnimationTrigger++
                                        }
                                        viewModel.sendText(inputText)
                                        inputText = ""
                                        viewModel.onTypingChanged("")
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(NeonCyan, NeonIndigo)))
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            } else {
                                IconButton(
                                    onClick = { viewModel.startVoiceRecording() },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33FFFFFF))
                                ) {
                                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Record Voice Note", tint = NeonCyan, modifier = Modifier.size(24.dp))
                                }
                            }
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

    // IMAGE PREVIEW BEFORE SENDING DIALOG (With Visible "Send Image" Button)
    if (selectedImageForPreview != null && isSendingImageMode) {
        ImageViewerDialog(
            imageModel = selectedImageForPreview,
            isSendingMode = true,
            caption = imageCaption,
            onCaptionChange = { imageCaption = it },
            onSendImageClick = {
                selectedImageForPreview?.let { file ->
                    viewModel.sendAttachment(file, "image/jpeg", "IMAGE")
                    if (imageCaption.isNotBlank()) {
                        viewModel.sendText(imageCaption)
                    }
                }
                selectedImageForPreview = null
                isSendingImageMode = false
                imageCaption = ""
            },
            onDismiss = {
                selectedImageForPreview = null
                isSendingImageMode = false
                imageCaption = ""
            }
        )
    }

    // FULLSCREEN IMAGE VIEWER MODAL
    if (fullViewerImageModel != null) {
        ImageViewerDialog(
            imageModel = fullViewerImageModel,
            isSendingMode = false,
            onDismiss = { fullViewerImageModel = null }
        )
    }

    // Message Long-Press Context Menu
    selectedMessageForOptions?.let { msg ->
        AlertDialog(
            onDismissRequest = { selectedMessageForOptions = null },
            containerColor = DarkSurface,
            title = { Text("Message Actions", color = Color.White) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("❤️", "👍", "😂", "😮", "🙏", "🔥").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        if (containsLoveEmoji(emoji)) {
                                            loveAnimationTrigger++
                                        }
                                        viewModel.addReaction(msg.id, emoji)
                                        selectedMessageForOptions = null
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    if (!msg.isDeleted) {
                        ListItem(
                            headlineContent = { Text("Reply", color = Color.White) },
                            leadingContent = { Icon(imageVector = Icons.Default.Reply, contentDescription = null, tint = NeonCyan) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                viewModel.setReplyingTo(msg)
                                selectedMessageForOptions = null
                            }
                        )

                        ListItem(
                            headlineContent = { Text("Delete for Everyone", color = DarkError) },
                            leadingContent = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = DarkError) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                viewModel.deleteMessage(msg.id)
                                selectedMessageForOptions = null
                            }
                        )
                    }
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
