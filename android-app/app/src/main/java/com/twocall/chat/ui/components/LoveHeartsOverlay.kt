package com.twocall.chat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlin.random.Random

val LOVE_EMOJIS = setOf(
    "❤️", "💖", "💕", "💗", "💓", "💘", "💞", "😍", "🥰", "😘", "💋", "❣️", "🩵", "🩷", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "❤️‍🔥", "❤️‍🩹"
)

fun containsLoveEmoji(text: String): Boolean {
    return LOVE_EMOJIS.any { text.contains(it) }
}

private data class HeartParticle(
    val id: Int,
    val emoji: String,
    val startXFraction: Float,
    val sizeSp: Int,
    val speedFactor: Float,
    val swayAmplitudeDp: Float,
    val swayFrequency: Float,
    val delayMs: Int,
    val rotationMaxDeg: Float
)

@Composable
fun LoveHeartsOverlay(
    triggerCount: Int,
    modifier: Modifier = Modifier
) {
    if (triggerCount <= 0) return

    val particles = remember(triggerCount) {
        val emojisList = listOf("❤️", "💖", "💕", "💗", "💓", "💘", "🥰", "✨", "💞", "🌸", "💋", "😍", "😘", "🩷")
        List(32) { index ->
            HeartParticle(
                id = index,
                emoji = emojisList[Random.nextInt(emojisList.size)],
                startXFraction = Random.nextFloat() * 0.88f + 0.06f,
                sizeSp = Random.nextInt(22, 48),
                speedFactor = Random.nextFloat() * 0.5f + 0.85f,
                swayAmplitudeDp = Random.nextFloat() * 32f + 12f,
                swayFrequency = Random.nextFloat() * 2f + 1f,
                delayMs = Random.nextInt(0, 850),
                rotationMaxDeg = Random.nextFloat() * 50f - 25f
            )
        }
    }

    val transitionState = remember(triggerCount) {
        MutableTransitionState(0f).apply { targetState = 1f }
    }

    val transition = updateTransition(transitionState, label = "loveOverlayTransition")

    val progress by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 3500, easing = LinearEasing)
        },
        label = "overlayProgress"
    ) { state -> state }

    val config = LocalConfiguration.current
    val screenWidthDp = config.screenWidthDp.dp
    val screenHeightDp = config.screenHeightDp.dp

    // Ambient Rose Pink Soft Glow pulse
    val glowAlpha = when {
        progress < 0.2f -> (progress / 0.2f) * 0.22f
        progress > 0.7f -> ((1f - progress) / 0.3f) * 0.22f
        else -> 0.22f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Romantic ambient background glow
        if (glowAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF2A6D).copy(alpha = glowAlpha),
                                Color(0xFFFF758C).copy(alpha = glowAlpha * 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Floating particles
        particles.forEach { particle ->
            val delayedProgress = ((progress * 3500f - particle.delayMs) / (2600f / particle.speedFactor))
                .coerceIn(0f, 1f)

            if (delayedProgress > 0f && delayedProgress < 1f) {
                val yOffsetDp = screenHeightDp * (1f - delayedProgress * 1.18f)
                val swayPx = sin(delayedProgress * Math.PI * 2 * particle.swayFrequency).toFloat() * particle.swayAmplitudeDp
                val xOffsetDp = (screenWidthDp.value * particle.startXFraction).dp + swayPx.dp

                val alpha = when {
                    delayedProgress < 0.12f -> delayedProgress / 0.12f
                    delayedProgress > 0.75f -> (1f - delayedProgress) / 0.25f
                    else -> 1f
                }

                val scale = when {
                    delayedProgress < 0.18f -> 0.4f + (delayedProgress / 0.18f) * 0.6f
                    else -> 1.0f + sin(delayedProgress * Math.PI).toFloat() * 0.15f
                }

                val rotation = sin(delayedProgress * Math.PI * 2).toFloat() * particle.rotationMaxDeg

                Text(
                    text = particle.emoji,
                    fontSize = particle.sizeSp.sp,
                    modifier = Modifier
                        .offset(x = xOffsetDp, y = yOffsetDp)
                        .alpha(alpha.coerceIn(0f, 1f))
                        .scale(scale)
                        .rotate(rotation)
                )
            }
        }
    }
}
