package com.twocall.chat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.twocall.chat.ui.theme.*
import kotlin.math.sin
import kotlin.random.Random

/**
 * Concentric expanding aquatic water ripples (for avatars, incoming/outgoing calls, and loading).
 */
@Composable
fun ZippyConcentricRipples(
    modifier: Modifier = Modifier,
    baseColor: Color = AquaCyan,
    rippleCount: Int = 3,
    maxRadius: Dp = 120.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "concentric_ripples")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_phase"
    )

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxPx = maxRadius.toPx()

        for (i in 0 until rippleCount) {
            val progress = (phase + i.toFloat() / rippleCount) % 1f
            val currentRadius = progress * maxPx
            val alpha = (1f - progress).coerceIn(0f, 1f) * 0.45f

            drawCircle(
                color = baseColor.copy(alpha = alpha),
                radius = currentRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = (2.5f * (1f - progress * 0.5f)).coerceAtLeast(1f))
            )
        }
    }
}

/**
 * Ambient Aquatic Background with deep gradient, subtle wave reflections, and gentle floating micro-bubbles.
 * Extremely lightweight and battery-efficient.
 */
@Composable
fun ZippyAquaticBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aquatic_particles")

    // Slow moving gradient glow
    val glowOffset by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_glow_offset"
    )

    // Floating particles drift
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_phase"
    )

    val particles = remember {
        List(14) {
            Triple(
                Random.nextFloat(), // X normalized (0..1)
                Random.nextFloat(), // Seed Y offset
                Random.nextFloat() * 3.5f + 1.5f // Radius size (1.5..5.0 px)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OceanAbyssGradient)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Soft top-center bioluminescent halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(OceanIndigo.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(w * 0.5f + glowOffset, h * 0.25f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.5f + glowOffset, h * 0.25f)
            )

            // 2. Soft bottom-right violet glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BettaViolet.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(w * 0.8f, h * 0.8f - glowOffset),
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = Offset(w * 0.8f, h * 0.8f - glowOffset)
            )

            // 3. Gentle rising micro-bubbles
            particles.forEach { (xNorm, ySeed, rad) ->
                val x = xNorm * w
                val yNorm = (1f - (particlePhase + ySeed) % 1f)
                val y = yNorm * h
                val alpha = (sin(yNorm * 3.14159f)).coerceIn(0f, 1f) * 0.25f

                drawCircle(
                    color = AquaCyan.copy(alpha = alpha),
                    radius = rad,
                    center = Offset(x, y)
                )
            }
        }

        content()
    }
}
