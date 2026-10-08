package com.twocall.chat.ui.screens

import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.data.local.entity.CallLogEntity
import com.twocall.chat.data.local.entity.MessageEntity
import com.twocall.chat.ui.components.*
import com.twocall.chat.ui.theme.*
import com.twocall.chat.ui.viewmodel.ChatViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface TimelineItem {
    val id: String
    val timestamp: Long

    data class MessageItem(val message: MessageEntity) : TimelineItem {
        override val id: String = message.id
        override val timestamp: Long = message.timestamp
    }

    data class CallLogItem(val callLog: CallLogEntity) : TimelineItem {
        override val id: String = callLog.callId
        override val timestamp: Long = callLog.endedAt ?: callLog.startedAt
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: ChatViewModel,
    onBackClick: () -> Unit = {},
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(messages, lifecycleState) {
        if (lifecycleState == androidx.lifecycle.Lifecycle.State.RESUMED) viewModel.markVisibleMessagesRead(messages)
    }
    val callLogs by viewModel.callLogs.collectAsState()
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
    val view = LocalView.current

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

    // Collect real-time love animation events emitted from repository
    LaunchedEffect(Unit) {
        viewModel.loveAnimationEvents.collect {
            loveAnimationTrigger++
        }
    }

    ZippyAquaticBackground {
        // Floating Love Hearts Overlay
        LoveHeartsOverlay(triggerCount = loveAnimationTrigger)

        Scaffold(
            topBar = {
                // Header: Neumorphic Top Bar
                Surface(
                    color = AbyssNavy.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Back to Home Button
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to home",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Partner Info with glowing ZippyAvatar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val partnerName = conversation?.partnerDisplayName?.takeIf { it.isNotBlank() } ?: "Partner"
                            val isOnline = conversation?.partnerOnline == true
                            val isTyping = conversation?.partnerTyping == true

                            ZippyAvatar(
                                name = partnerName,
                                imageBase64 = conversation?.partnerImageBase64,
                                size = 42.dp,
                                isOnline = isOnline,
                                showOnlineDot = true
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = partnerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                Text(
                                    text = if (isTyping) "typing..." else if (isOnline) "In Sanctuary" else "Offline",
                                    fontSize = 11.sp,
                                    color = if (isTyping) AquaCyan else if (isOnline) LuminousTeal else TextMutedDark,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Neumorphic Call Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ZippyNeumorphicIconButton(
                                onClick = onAudioCallClick,
                                icon = Icons.Default.Call,
                                contentDescription = "Audio Call",
                                size = 42.dp,
                                iconSize = 20.dp,
                                iconTint = AquaCyan,
                                backgroundColor = NeumorphicBaseDark
                            )

                            ZippyNeumorphicIconButton(
                                onClick = onVideoCallClick,
                                icon = Icons.Default.Videocam,
                                contentDescription = "Video Call",
                                size = 42.dp,
                                iconSize = 20.dp,
                                iconTint = OceanIndigo,
                                backgroundColor = NeumorphicBaseDark
                            )

                            ZippyNeumorphicIconButton(
                                onClick = onSettingsClick,
                                icon = Icons.Default.MoreVert,
                                contentDescription = "Settings",
                                size = 42.dp,
                                iconSize = 20.dp,
                                iconTint = TextSecondaryDark,
                                backgroundColor = NeumorphicBaseDark
                            )
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val timelineItems = remember(messages, callLogs) {
                    val items = mutableListOf<TimelineItem>()
                    messages.forEach { items.add(TimelineItem.MessageItem(it)) }
                    callLogs.forEach { items.add(TimelineItem.CallLogItem(it)) }
                    items.sortedBy { it.timestamp }
                }

                // Messages & Call History Timeline List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    items(timelineItems, key = { it.id }) { item ->
                        when (item) {
                            is TimelineItem.MessageItem -> {
                                val msg = item.message
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
                            is TimelineItem.CallLogItem -> {
                                CallLogBubble(callLog = item.callLog)
                            }
                        }
                    }
                }

                // Replying Banner
                AnimatedVisibility(
                    visible = replyingTo != null,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    if (replyingTo != null) {
                        ZippyNeumorphicCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = NeumorphicBaseDark
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(30.dp)
                                            .background(AquaCyan, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Replying to ${if (replyingTo!!.isOutgoing) "yourself" else "partner"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AquaCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = replyingTo!!.plaintext.take(45),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray,
                                            maxLines = 1
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.setReplyingTo(null) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel reply",
                                        tint = TextMutedDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Neumorphic Message Composer
                if (isRecording) {
                    VoiceRecordingBar(
                        durationSeconds = recordingDuration,
                        amplitude = recordingAmplitude,
                        onCancel = { viewModel.cancelVoiceRecording() },
                        onSend = { viewModel.stopAndSendVoiceRecording() }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                    ) {
                        ZippyNeumorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            backgroundColor = NeumorphicBaseDark,
                            borderStroke = 1.2.dp,
                            highlightColor = AquaCyan.copy(alpha = 0.25f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Attachment Picker Button
                                IconButton(
                                    onClick = { showAttachmentSheet = true },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = "Attach file",
                                        tint = AquaCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Text Input
                                TextField(
                                    value = inputText,
                                    onValueChange = {
                                        inputText = it
                                        viewModel.onTypingChanged(it)
                                    },
                                    placeholder = {
                                        Text(
                                            text = "Private message...",
                                            color = TextMutedDark,
                                            fontSize = 14.sp
                                        )
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 4
                                )

                                // Smooth Morphing Mic -> Send Button
                                val hasText = inputText.isNotBlank()
                                AnimatedContent(
                                    targetState = hasText,
                                    transitionSpec = {
                                        (fadeIn(tween(150)) + scaleIn(tween(150)))
                                            .togetherWith(fadeOut(tween(150)) + scaleOut(tween(150)))
                                    },
                                    label = "composer_action"
                                ) { isSendMode ->
                                    if (isSendMode) {
                                        ZippyNeumorphicIconButton(
                                            onClick = {
                                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                                if (containsLoveEmoji(inputText)) {
                                                    loveAnimationTrigger++
                                                }
                                                viewModel.sendText(inputText)
                                                inputText = ""
                                                viewModel.onTypingChanged("")
                                            },
                                            icon = Icons.Default.Send,
                                            contentDescription = "Send Message",
                                            size = 42.dp,
                                            iconSize = 18.dp,
                                            backgroundColor = NeumorphicBaseDark,
                                            iconTint = Color.White,
                                            isSelected = true
                                        )
                                    } else {
                                        ZippyNeumorphicIconButton(
                                            onClick = {
                                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                viewModel.startVoiceRecording()
                                            },
                                            icon = Icons.Default.Mic,
                                            contentDescription = "Record Voice Note",
                                            size = 42.dp,
                                            iconSize = 20.dp,
                                            backgroundColor = NeumorphicBaseDark,
                                            iconTint = AquaCyan
                                        )
                                    }
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

    // Image Preview Before Sending Dialog
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

    // Fullscreen Image Viewer Modal
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
            containerColor = NeumorphicBaseDark,
            title = { Text("Sanctuary Options", color = Color.White) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("❤️", "🌊", "✨", "🐟", "🔥", "🙏").forEach { emoji ->
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
                            leadingContent = { Icon(imageVector = Icons.Default.Reply, contentDescription = null, tint = AquaCyan) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                viewModel.setReplyingTo(msg)
                                selectedMessageForOptions = null
                            }
                        )

                        ListItem(
                            headlineContent = { Text("Delete for Everyone", color = AquaticDecline) },
                            leadingContent = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = AquaticDecline) },
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

@Composable
fun CallLogBubble(
    callLog: CallLogEntity,
    modifier: Modifier = Modifier
) {
    val isVideo = callLog.callType.equals("VIDEO", ignoreCase = true)
    val isMissed = callLog.status == "MISSED" || callLog.status == "DECLINED" || callLog.status == "FAILED"
    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(callLog.startedAt))

    val statusText = if (callLog.status == "ANSWERED") {
        val mins = callLog.durationSeconds / 60
        val secs = callLog.durationSeconds % 60
        if (mins > 0 && secs > 0) "$mins min $secs sec"
        else if (mins > 0) "$mins min"
        else "$secs sec"
    } else {
        callLog.status.lowercase().replaceFirstChar { it.uppercase() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp),
        contentAlignment = if (callLog.direction == "OUTGOING") Alignment.CenterEnd else Alignment.CenterStart
    ) {
        ZippyNeumorphicCard(
            modifier = Modifier.widthIn(min = 160.dp, max = 260.dp),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = if (callLog.direction == "OUTGOING") DeepAquaticBlue.copy(alpha = 0.7f) else AquaticSurface.copy(alpha = 0.85f),
            borderStroke = 1.dp,
            highlightColor = if (isMissed) BettaCoral.copy(alpha = 0.4f) else AquaCyan.copy(alpha = 0.25f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = null,
                        tint = if (isMissed) BettaCoral else AquaCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isVideo) "Video call" else "Voice call",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = if (isMissed) BettaCoral else Color.LightGray
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextMutedDark
                    )
                }
            }
        }
    }
}
