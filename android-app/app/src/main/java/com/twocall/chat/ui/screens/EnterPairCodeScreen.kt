package com.twocall.chat.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.ui.components.*
import com.twocall.chat.ui.theme.*
import com.twocall.chat.ui.viewmodel.PairingViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterPairCodeScreen(
    viewModel: PairingViewModel,
    onBackClick: () -> Unit,
    onPairedSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var codeInput by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current

    var isConnectingAnimation by remember { mutableStateOf(false) }
    var isConnectedSuccessState by remember { mutableStateOf(false) }

    val encounterProgress by animateFloatAsState(
        targetValue = if (isConnectingAnimation) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "encounter_progress"
    )

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(uiState.isPairedSuccessfully) {
        if (uiState.isPairedSuccessfully) {
            isConnectingAnimation = true
            delay(1200)
            isConnectedSuccessState = true
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            delay(1000)
            onPairedSuccess()
        }
    }

    ZippyAquaticBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Join Partner",
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
                if (isConnectingAnimation) {
                    // Two Betta Fish Approaching and Converging Animation
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            ZippyConcentricRipples(
                                modifier = Modifier.fillMaxSize(),
                                baseColor = if (isConnectedSuccessState) LuminousTeal else AquaCyan,
                                rippleCount = 3,
                                maxRadius = 110.dp
                            )
                            ZippyTwinBettaEncounter(
                                modifier = Modifier.size(220.dp),
                                progress = encounterProgress
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        AnimatedContent(
                            targetState = isConnectedSuccessState,
                            label = "connected_text"
                        ) { isSuccess ->
                            if (isSuccess) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Connected ",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 28.sp
                                        ),
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Connected Love",
                                        tint = BiolumPink,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Connecting sanctuaries...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AquaCyan,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    // Regular Digit Entry UI
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Enter 6-digit Code",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Enter the single-use pairing code displayed on your partner's screen.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = TextSecondaryDark,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        // Hidden input field capturing keystrokes
                        BasicTextField(
                            value = codeInput,
                            onValueChange = {
                                if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                    codeInput = it
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    if (it.length == 6) {
                                        viewModel.joinPair(it)
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .size(1.dp)
                        )

                        // 6 Individual Neumorphic PIN boxes
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                        ) {
                            repeat(6) { index ->
                                val digit = if (index < codeInput.length) codeInput[index].toString() else ""
                                val isCurrent = index == codeInput.length
                                val isFilled = index < codeInput.length

                                val boxScale by animateFloatAsState(
                                    targetValue = if (isFilled) 1.05f else if (isCurrent) 1.02f else 1.0f,
                                    animationSpec = tween(120),
                                    label = "digit_scale"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(44.dp, 60.dp)
                                        .scale(boxScale)
                                        .shadow(
                                            elevation = if (isCurrent) 8.dp else 4.dp,
                                            shape = RoundedCornerShape(16.dp),
                                            spotColor = if (isFilled) BettaViolet.copy(alpha = 0.4f) else Color.Transparent
                                        )
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(NeumorphicBaseDark)
                                        .border(
                                            width = if (isCurrent) 2.dp else 1.2.dp,
                                            brush = if (isCurrent) Brush.linearGradient(listOf(AquaCyan, BettaViolet))
                                            else if (isFilled) Brush.linearGradient(listOf(BettaViolet.copy(alpha = 0.8f), OceanIndigo.copy(alpha = 0.5f)))
                                            else Brush.linearGradient(listOf(GlassBorderDark, Color.Transparent)),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (digit.isNotEmpty()) {
                                        Text(
                                            text = digit,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 24.sp
                                            ),
                                            color = Color.White
                                        )
                                    } else {
                                        // Empty underscore placeholder indicator
                                        Box(
                                            modifier = Modifier
                                                .width(14.dp)
                                                .height(2.5.dp)
                                                .background(
                                                    if (isCurrent) AquaCyan else Color.White.copy(alpha = 0.2f),
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        if (uiState.isLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = AquaCyan,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Authenticating with partner...",
                                    color = AquaCyan,
                                    fontSize = 13.sp
                                )
                            }
                        } else if (uiState.error != null) {
                            Text(
                                text = uiState.error!!,
                                color = AquaticDecline,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
