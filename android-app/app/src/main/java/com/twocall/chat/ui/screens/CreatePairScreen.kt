package com.twocall.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.ui.theme.DarkBackground
import com.twocall.chat.ui.theme.DarkPrimary
import com.twocall.chat.ui.theme.DarkSurfaceVariant
import com.twocall.chat.ui.viewmodel.PairingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePairScreen(
    viewModel: PairingViewModel,
    onBackClick: () -> Unit,
    onPairedSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (uiState.pairingCode == null) {
            viewModel.createPair()
        } else {
            viewModel.checkCurrentPairStatus()
        }
    }

    LaunchedEffect(uiState.isPairedSuccessfully) {
        if (uiState.isPairedSuccessfully) {
            onPairedSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Pair") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DarkPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Generating one-time pairing code...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (uiState.pairingCode != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Share this code with your partner",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "This single-use code connects your phones with end-to-end encryption and expires in 5 minutes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // 6-digit Code Display in individual boxes
                    val code = uiState.pairingCode!!
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        code.forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .size(46.dp, 60.dp)
                                    .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                                    .border(1.dp, DarkPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit.toString(),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = DarkPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(code))
                            copied = true
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (copied) "Copied to clipboard!" else "Copy Code")
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // 5-minute Countdown Timer
                    val minutes = uiState.secondsRemaining / 60
                    val seconds = uiState.secondsRemaining % 60
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(DarkSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = if (uiState.secondsRemaining < 60) MaterialTheme.colorScheme.error else DarkPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Expires in %02d:%02d".format(minutes, seconds),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.secondsRemaining < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = DarkPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Waiting for partner to enter code...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { viewModel.checkCurrentPairStatus() }) {
                        Text(
                            text = "Partner already entered? Tap to check",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkPrimary
                        )
                    }
                }

            } else if (uiState.error != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.createPair() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary)
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}
