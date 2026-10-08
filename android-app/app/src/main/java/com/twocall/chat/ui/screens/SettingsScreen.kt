package com.twocall.chat.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.ChatApplication
import com.twocall.chat.ui.components.*
import com.twocall.chat.ui.theme.*
import com.twocall.chat.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    app: ChatApplication,
    onBackClick: () -> Unit,
    onDisconnected: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val view = LocalView.current

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDisconnectConfirmDialog by remember { mutableStateOf(false) }

    ZippyAquaticBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Settings & Privacy",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
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
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ProfileEditor(app)

                NotificationSettings(app)

                // Section 1: End-to-End Encryption & Security Fingerprints
                ZippyNeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = NeumorphicBaseDark,
                    borderStroke = 1.2.dp,
                    highlightColor = AquaCyan.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(AquaCyan, OceanIndigo))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Private Sanctuary Encryption",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "NIST P-256 ECDH & AES-256-GCM",
                                    color = AquaCyan,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "All messages, photos, and calls are encrypted on-device. Plaintext never traverses servers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Partner Identity Fingerprint",
                            style = MaterialTheme.typography.labelSmall,
                            color = AquaCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = AquaticSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 12.dp)
                        ) {
                            Text(
                                text = viewModel.getSafetyFingerprint(),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Text(
                            text = "Your Identity Fingerprint",
                            style = MaterialTheme.typography.labelSmall,
                            color = AquaCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = AquaticSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Text(
                                text = viewModel.getMyFingerprint(),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Section 2: Call Quality & Video Engine
                ZippyNeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = NeumorphicBaseDark,
                    borderStroke = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(BettaViolet, BiolumPink))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Calling Engine",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Full HD (1080p @ 30fps) Enabled",
                                    color = LuminousTeal,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Video Resolution",
                                color = TextSecondaryDark,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "1920×1080 Full HD",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bandwidth Allocation",
                                color = TextSecondaryDark,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Adaptive up to 4.0 Mbps",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Section 3: About ZippyCall
                ZippyNeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = NeumorphicBaseDark,
                    borderStroke = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ZippyBettaFish(
                            modifier = Modifier.size(54.dp),
                            primaryColor = AquaCyan,
                            secondaryColor = BettaViolet
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "ZippyCall",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Version 2.0 • Sanctuary for Two",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Private. Just for two.",
                                color = AquaCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Section 4: Device & Connection Controls (Disconnect & Terminate)
                ZippyNeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = NeumorphicBaseDark,
                    borderStroke = 1.dp,
                    highlightColor = AquaticDecline.copy(alpha = 0.25f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Connection Controls",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        // Disconnect Device
                        ZippyNeumorphicButton(
                            onClick = { showDisconnectConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            text = "Disconnect Device",
                            icon = Icons.Default.ExitToApp,
                            gradient = Brush.linearGradient(listOf(Color(0xFF263248), Color(0xFF1A233A)))
                        )

                        // Permanently Delete Pair
                        ZippyNeumorphicButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            text = "Permanently Delete Pair",
                            icon = Icons.Default.DeleteForever,
                            gradient = Brush.linearGradient(listOf(AquaticDecline, Color(0xFF8B0000)))
                        )
                    }
                }
            }
        }
    }

    // Disconnect Confirmation Dialog
    if (showDisconnectConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirmDialog = false },
            containerColor = NeumorphicBaseDark,
            title = { Text("Disconnect Device?", color = Color.White) },
            text = {
                Text(
                    "You will be disconnected from the private channel. You can re-enter with your partner code.",
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        showDisconnectConfirmDialog = false
                        coroutineScope.launch {
                            app.disconnectDevice()
                            onDisconnected()
                        }
                    }
                ) {
                    Text("Disconnect", color = AquaCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            }
        )
    }

    // Permanently Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = NeumorphicBaseDark,
            title = { Text("Permanently Delete Pair?", color = AquaticDecline, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This action cannot be undone. All messages, files, and encryption keys will be permanently destroyed on both devices.",
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        showDeleteConfirmDialog = false
                        coroutineScope.launch {
                            app.deletePairPermanently()
                            onDisconnected()
                        }
                    }
                ) {
                    Text("Delete Everything", color = AquaticDecline, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            }
        )
    }
}
