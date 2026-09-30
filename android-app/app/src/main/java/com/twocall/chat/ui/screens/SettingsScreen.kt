package com.twocall.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.ChatApplication
import com.twocall.chat.ui.components.AnimatedGlassBackground
import com.twocall.chat.ui.components.GlassButton
import com.twocall.chat.ui.components.GlassCard
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
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDisconnectConfirmDialog by remember { mutableStateOf(false) }

    AnimatedGlassBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Privacy & Security Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // E2EE Safety Fingerprint Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NeonCyan.copy(alpha = 0.35f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = NeonCyan)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "End-to-End Encryption",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Messages and calls are encrypted on-device using NIST P-256 ECDH and AES-256-GCM. Plaintext never leaves your phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Partner Identity Fingerprint:",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = viewModel.getSafetyFingerprint(),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 12.dp)
                    )

                    Text(
                        text = "Your Identity Fingerprint:",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = viewModel.getMyFingerprint(),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }

                // Session & Pair Danger Zone Controls
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = Color.White.copy(alpha = 0.2f)
                ) {
                    ListItem(
                        headlineContent = { Text("Disconnect Device", color = Color.White, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Log out and revoke device session", color = DarkOnSurfaceVariant) },
                        leadingContent = { Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = NeonCyan) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    GlassButton(
                        onClick = { showDisconnectConfirmDialog = true },
                        gradientColors = listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Disconnect", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    ListItem(
                        headlineContent = { Text("Permanently Delete Pair", color = DarkError, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Destroys pair credentials, chat history, and files from both phones", color = DarkOnSurfaceVariant) },
                        leadingContent = { Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = DarkError) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    GlassButton(
                        onClick = { showDeleteConfirmDialog = true },
                        gradientColors = listOf(DarkError, Color(0xFFB91C1C)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Delete Pair & All Data", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDisconnectConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirmDialog = false },
            containerColor = DarkSurface,
            title = { Text("Disconnect Device?", color = Color.White) },
            text = { Text("You will be logged out of this private pair. Re-pairing will be required.", color = DarkOnSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisconnectConfirmDialog = false
                        coroutineScope.launch {
                            app.disconnectDevice()
                            onDisconnected()
                        }
                    }
                ) {
                    Text("Disconnect", color = NeonCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = DarkSurface,
            title = { Text("Permanently Delete Pair?", color = Color.White) },
            text = { Text("This permanently deletes the private pair, encrypted messages, and media. Action cannot be undone.", color = DarkOnSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        coroutineScope.launch {
                            app.deletePairPermanently()
                            onDisconnected()
                        }
                    }
                ) {
                    Text("Delete Forever", color = DarkError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
