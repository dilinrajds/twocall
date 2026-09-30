package com.twocall.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.ui.components.AnimatedGlassBackground
import com.twocall.chat.ui.components.GlassButton
import com.twocall.chat.ui.components.GlassCard
import com.twocall.chat.ui.theme.*
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

    AnimatedGlassBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Create Pair Code", color = Color.White, fontWeight = FontWeight.Bold) },
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonCyan, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Generating one-time pairing code...", color = DarkOnSurfaceVariant)
                    }
                } else if (uiState.pairingCode != null) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = NeonCyan.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = "Share this code with your partner",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Single-use code expires in 5 minutes. Destroys automatically after pairing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkOnSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(30.dp))

                        // 6-digit Code Display in individual glass boxes
                        val code = uiState.pairingCode!!
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            code.forEach { digit ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp, 60.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x33FFFFFF))
                                        .border(1.5.dp, Brush.linearGradient(listOf(NeonCyan, NeonIndigo)), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = digit.toString(),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 26.sp
                                        ),
                                        color = NeonCyan
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        GlassButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(code))
                                copied = true
                            },
                            gradientColors = listOf(Color(0x30FFFFFF), Color(0x1AFFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (copied) "Copied to Clipboard!" else "Copy Pair Code", color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Countdown Timer
                        val minutes = uiState.secondsRemaining / 60
                        val seconds = uiState.secondsRemaining % 60
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clip(CircleShape)
                                .background(Color(0x20FFFFFF))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = if (uiState.secondsRemaining < 60) DarkError else NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Expires in %02d:%02d".format(minutes, seconds),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (uiState.secondsRemaining < 60) DarkError else Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            CircularProgressIndicator(
                                color = NeonCyan,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Waiting for partner...",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkOnSurfaceVariant
                            )
                        }
                    }

                } else if (uiState.error != null) {
                    GlassCard(borderColor = DarkError.copy(alpha = 0.4f)) {
                        Text(
                            text = uiState.error!!,
                            color = DarkError,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GlassButton(
                            onClick = { viewModel.createPair() },
                            gradientColors = listOf(NeonCyan, NeonIndigo),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Try Again", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
