package com.twocall.chat.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.data.local.entity.ConversationEntity
import com.twocall.chat.ui.components.ZippyAquaticBackground
import com.twocall.chat.ui.components.ZippyAvatar
import com.twocall.chat.ui.components.ZippyNeumorphicCard
import com.twocall.chat.ui.theme.*
import com.twocall.chat.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: ChatViewModel,
    onOpenConversation: (pairId: String) -> Unit,
    onAddPersonClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val conversations by viewModel.allConversations.collectAsState()

    ZippyAquaticBackground {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onAddPersonClick,
                    containerColor = AquaCyan,
                    contentColor = AbyssNavy,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    shape = RoundedCornerShape(24.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Person",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "+ Add / Pair Person",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ZippyCall",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                brush = Brush.linearGradient(listOf(AquaCyan, BettaViolet, BettaCoral))
                            )
                        )
                        Text(
                            text = "Private Sanctuary",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedDark
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick Add Button in Header
                        IconButton(
                            onClick = onAddPersonClick,
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(AquaticSurface)
                                .border(1.dp, AquaCyan.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Person",
                                tint = AquaCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(AquaticSurface)
                                .border(1.dp, GlassBorderDark, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Conversations List
                if (conversations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ZippyNeumorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            backgroundColor = NeumorphicBaseDark,
                            borderStroke = 1.dp,
                            highlightColor = AquaCyan.copy(alpha = 0.2f)
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = AquaCyan,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Contacts Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Pair privately with a friend or partner using a secure 6-digit code.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMutedDark,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = onAddPersonClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = AquaCyan),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = "+ Add / Pair Person",
                                        color = AbyssNavy,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                    ) {
                        itemsIndexed(conversations, key = { _, conv -> conv.pairId }) { index, conv ->
                            ConversationRowItem(
                                conversation = conv,
                                index = index,
                                onClick = {
                                    viewModel.selectConversation(conv.pairId)
                                    onOpenConversation(conv.pairId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRowItem(
    conversation: ConversationEntity,
    index: Int,
    onClick: () -> Unit
) {
    val displayName = conversation.partnerDisplayName?.takeIf { it.isNotBlank() }
        ?: "Person ${(index + 65).toChar()}" // Person A, Person B, Person C, Person D...

    ZippyNeumorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = AquaticSurface.copy(alpha = 0.85f),
        borderStroke = 1.dp,
        highlightColor = if (conversation.unreadCount > 0) AquaCyan.copy(alpha = 0.35f) else GlassBorderDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with online status dot
            ZippyAvatar(
                name = displayName,
                imageBase64 = conversation.partnerImageBase64,
                size = 50.dp,
                isOnline = conversation.partnerOnline,
                showOnlineDot = true
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Contact Name + Last Message Preview
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Timestamp
                    val timeText = formatConversationTimestamp(conversation.lastMessageTimestamp)
                    if (timeText.isNotBlank()) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (conversation.unreadCount > 0) AquaCyan else TextMutedDark,
                            fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val previewText = when {
                        conversation.partnerTyping -> "Typing..."
                        !conversation.lastMessagePreview.isNullOrBlank() -> conversation.lastMessagePreview!!
                        else -> "Tap to start chatting"
                    }
                    val previewColor = when {
                        conversation.partnerTyping -> AquaCyan
                        conversation.unreadCount > 0 -> Color.White
                        else -> TextMutedDark
                    }

                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = previewColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )

                    // Unread Count Badge
                    if (conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .shadow(4.dp, CircleShape, spotColor = BettaCoral)
                                .clip(CircleShape)
                                .background(BettaCoral),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatConversationTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val date = Date(timestamp)
    val calNow = Calendar.getInstance()
    val calMsg = Calendar.getInstance().apply { time = date }

    return when {
        diff < 60_000L -> "Just now"
        calNow.get(Calendar.YEAR) == calMsg.get(Calendar.YEAR) &&
        calNow.get(Calendar.DAY_OF_YEAR) == calMsg.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        }
        diff < 48 * 3600 * 1000L && (calNow.get(Calendar.DAY_OF_YEAR) - calMsg.get(Calendar.DAY_OF_YEAR) == 1) -> {
            "Yesterday"
        }
        diff < 7 * 24 * 3600 * 1000L -> {
            SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
        }
        else -> {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        }
    }
}
