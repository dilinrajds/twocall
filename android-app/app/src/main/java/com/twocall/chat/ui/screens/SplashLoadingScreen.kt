package com.twocall.chat.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.ui.components.*
import com.twocall.chat.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashLoadingScreen(
    checkPairStatus: suspend () -> Boolean,
    onNavigateToWelcome: () -> Unit,
    onNavigateToConversation: () -> Unit
) {
    var animationStage by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // Stage 1: Bubble rises (0..350ms)
        delay(350)
        animationStage = 1

        // Stage 2: Betta forms & ripple expands (350..700ms)
        delay(350)
        animationStage = 2

        // Stage 3: ZippyCall reveals and navigate
        val isPaired = checkPairStatus()
        delay(400)
        if (isPaired) {
            onNavigateToConversation()
        } else {
            onNavigateToWelcome()
        }
    }

    val bubbleTransition = rememberInfiniteTransition(label = "bubble_glow")
    val bubblePulse by bubbleTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bubble_pulse"
    )

    ZippyAquaticBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .scale(bubblePulse),
                contentAlignment = Alignment.Center
            ) {
                // Expanding concentric aquatic ripples
                ZippyConcentricRipples(
                    modifier = Modifier.fillMaxSize(),
                    baseColor = AquaCyan,
                    rippleCount = 3,
                    maxRadius = 90.dp
                )

                // Soft neumorphic ambient bubble capsule
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(AquaCyan.copy(alpha = 0.22f), NeumorphicBaseDark.copy(alpha = 0.85f))
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(listOf(AquaCyan, BettaViolet)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Flowing Betta Fish with fluid fins
                    ZippyBettaFish(
                        modifier = Modifier.size(92.dp),
                        primaryColor = AquaCyan,
                        secondaryColor = OceanIndigo,
                        accentColor = BiolumPink
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Identity with luminous aquatic gradient
            AnimatedVisibility(
                visible = animationStage >= 1,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 20 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ZippyCall",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 38.sp,
                            letterSpacing = 1.8.sp
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Private. Just for two.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.2.sp
                        ),
                        color = AquaCyan.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Subtle E2EE status badge
            AnimatedVisibility(
                visible = animationStage >= 2,
                enter = fadeIn(tween(350))
            ) {
                ZippyNeumorphicCard(
                    modifier = Modifier.padding(horizontal = 48.dp),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = AquaticSurface.copy(alpha = 0.5f),
                    borderStroke = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(LuminousTeal)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "End-to-End Encrypted Sanctuary",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
