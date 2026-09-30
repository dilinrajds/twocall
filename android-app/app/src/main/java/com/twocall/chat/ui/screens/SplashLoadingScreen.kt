package com.twocall.chat.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.ui.components.AnimatedGlassBackground
import com.twocall.chat.ui.components.GlassCard
import com.twocall.chat.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashLoadingScreen(
    checkPairStatus: suspend () -> Boolean,
    onNavigateToWelcome: () -> Unit,
    onNavigateToConversation: () -> Unit
) {
    LaunchedEffect(Unit) {
        val isPaired = checkPairStatus()
        delay(800)
        if (isPaired) {
            onNavigateToConversation()
        } else {
            onNavigateToWelcome()
        }
    }

    // Dynamic pulse animation for splash logo
    val infiniteTransition = rememberInfiniteTransition(label = "splashHalo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )

    AnimatedGlassBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Halo Container
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.35f), Color.Transparent)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NeonCyan, NeonIndigo)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "TwoCall Shield",
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "TwoCall",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 38.sp,
                    letterSpacing = 2.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(
                shape = RoundedCornerShape(20.dp),
                backgroundColor = Color(0x1F2B3648),
                borderColor = NeonCyan.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = 40.dp)
            ) {
                Text(
                    text = "End-to-End Encrypted Private Channel",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = NeonCyan,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = NeonCyan,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
