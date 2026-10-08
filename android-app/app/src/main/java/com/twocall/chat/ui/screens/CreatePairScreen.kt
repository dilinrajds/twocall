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
import com.twocall.chat.ui.components.*
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

    ZippyAquaticBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Private Pair Code",
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ZippyBettaFish(
                            modifier = Modifier.size(90.dp),
                            primaryColor = AquaCyan,
                            secondaryColor = OceanIndigo
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Generating private pair code...",
                            color = TextSecondaryDark,
                            fontSize = 14.sp
                        )
                    }
                } else if (uiState.pairingCode != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Animated Betta atop the card
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(AquaCyan.copy(alpha = 0.2f), Color.Transparent)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            ZippyBettaFish(
                                modifier = Modifier.size(95.dp),
                                primaryColor = AquaCyan,
                                secondaryColor = OceanIndigo,
                                accentColor = BiolumPink
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Floating Neumorphic Capsule with Ripple
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Gentle water ripple behind code capsule
                            ZippyConcentricRipples(
                                modifier = Modifier.size(300.dp, 200.dp),
                                baseColor = AquaCyan.copy(alpha = 0.4f),
                                rippleCount = 2,
                                maxRadius = 130.dp
                            )

                            ZippyNeumorphicCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(28.dp),
                                backgroundColor = NeumorphicBaseDark,
                                borderStroke = 1.2.dp,
                                highlightColor = AquaCyan.copy(alpha = 0.35f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Your private code",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AquaCyan,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Share with your partner to connect",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondaryDark
                                    )

                                    Spacer(modifier = Modifier.height(26.dp))

                                    // Spaced individual 6-digit neumorphic pills
                                    val code = uiState.pairingCode!!
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        code.forEach { digit ->
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp, 58.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(AquaticSurface)
                                                    .border(
                                                        width = 1.2.dp,
                                                        brush = Brush.verticalGradient(
                                                            listOf(AquaCyan.copy(alpha = 0.7f), OceanIndigo.copy(alpha = 0.2f))
                                                        ),
                                                        shape = RoundedCornerShape(14.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = digit.toString(),
                                                    style = MaterialTheme.typography.headlineMedium.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 24.sp
                                                    ),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Countdown Timer
                                    val minutes = uiState.secondsRemaining / 60
                                    val seconds = uiState.secondsRemaining % 60
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassTop,
                                            contentDescription = null,
                                            tint = BettaViolet,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Expires in %02d:%02d".format(minutes, seconds),
                                            color = TextSecondaryDark,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Copy Code Action
                                    ZippyNeumorphicButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(code))
                                            copied = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        text = if (copied) "Copied to Clipboard!" else "Copy Private Code",
                                        icon = Icons.Default.ContentCopy,
                                        gradient = if (copied) Brush.linearGradient(listOf(LuminousTeal, OceanIndigo))
                                        else BettaFlowGradient
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Listening for partner connection indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = AquaCyan,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Awaiting partner entry...",
                                color = TextMutedDark,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else if (uiState.error != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = AquaticDecline,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        ZippyNeumorphicButton(
                            onClick = { viewModel.createPair() },
                            text = "Retry"
                        )
                    }
                }
            }
        }
    }
}
