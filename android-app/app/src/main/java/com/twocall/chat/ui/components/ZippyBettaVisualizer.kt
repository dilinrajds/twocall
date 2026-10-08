package com.twocall.chat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.twocall.chat.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Betta Fish Visualizer rendered procedurally with fluid sinusoidal fin undulations.
 *
 * @param modifier Modifier
 * @param primaryColor Primary glowing body and fin tint
 * @param secondaryColor Flowing veil tail accent tint
 * @param audioLevel Audio-reactive boost between 0.0f and 1.0f
 * @param isSwimming Whether the Betta actively glides
 */
@Composable
fun ZippyBettaFish(
    modifier: Modifier = Modifier,
    primaryColor: Color = AquaCyan,
    secondaryColor: Color = BettaViolet,
    accentColor: Color = BiolumPink,
    audioLevel: Float = 0f,
    isSwimming: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "betta_motion")

    // Fin oscillation frequency & amplitude
    val finWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fin_wave"
    )

    // Gentle floating breathing translation
    val breathingY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_y"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f + breathingY

        // Dynamic audio boost
        val audioBoost = (audioLevel.coerceIn(0f, 1f) * 1.5f)
        val waveOffset = finWave

        drawBettaFish(
            centerX = centerX,
            centerY = centerY,
            scale = width / 200f,
            wave = waveOffset,
            audioBoost = audioBoost,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            accentColor = accentColor
        )
    }
}

/**
 * Twin Betta Fish Encounter: Two Betta fish swimming in harmony or approaching from opposite sides.
 */
@Composable
fun ZippyTwinBettaEncounter(
    modifier: Modifier = Modifier,
    progress: Float = 1.0f, // 0.0 = separated on sides, 1.0 = converged at center
    audioLevel: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "twin_betta_motion")

    val orbitWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_wave"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        val separationDistance = (1f - progress.coerceIn(0f, 1f)) * (width * 0.38f)

        // Betta 1 (Cyan/Aqua - swims from left/top orbit)
        val angle1 = orbitWave
        val b1X = centerX - separationDistance + cos(angle1) * (18f * progress)
        val b1Y = centerY + sin(angle1) * (14f * progress)

        // Betta 2 (Coral/Violet - swims from right/bottom orbit)
        val angle2 = orbitWave + 3.14159f // opposite phase
        val b2X = centerX + separationDistance + cos(angle2) * (18f * progress)
        val b2Y = centerY + sin(angle2) * (14f * progress)

        // If converged, draw central glowing aquatic convergence halo
        if (progress > 0.7f) {
            val haloAlpha = ((progress - 0.7f) / 0.3f) * 0.45f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AquaCyan.copy(alpha = haloAlpha), BettaViolet.copy(alpha = haloAlpha * 0.5f), Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = width * 0.35f
                ),
                radius = width * 0.35f,
                center = Offset(centerX, centerY)
            )
        }

        // Draw First Betta (Electric Cyan)
        drawBettaFish(
            centerX = b1X,
            centerY = b1Y,
            scale = (width / 240f),
            wave = orbitWave,
            audioBoost = audioLevel,
            primaryColor = AquaCyan,
            secondaryColor = OceanIndigo,
            accentColor = Color.White
        )

        // Draw Second Betta (Bioluminescent Coral / Violet)
        drawBettaFish(
            centerX = b2X,
            centerY = b2Y,
            scale = (width / 240f),
            wave = orbitWave + 2f,
            audioBoost = audioLevel,
            primaryColor = BiolumPink,
            secondaryColor = BettaViolet,
            accentColor = Color.White,
            flipHorizontal = true
        )
    }
}

/**
 * Core procedural rendering of a graceful Betta fish with sinuous flowing fins.
 */
private fun DrawScope.drawBettaFish(
    centerX: Float,
    centerY: Float,
    scale: Float,
    wave: Float,
    audioBoost: Float,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    flipHorizontal: Boolean = false
) {
    val dir = if (flipHorizontal) -1f else 1f
    val dynamicWave = sin(wave) * (8f + audioBoost * 12f) * scale
    val tailSway = sin(wave + 1.2f) * (14f + audioBoost * 18f) * scale

    // 1. Flowing Veil Caudal Tail (Multi-layered Translucent Gradient Veil)
    val tailPath = Path().apply {
        moveTo(centerX - 15f * scale * dir, centerY)
        // Upper flowing fin lobe
        cubicTo(
            centerX - 35f * scale * dir, centerY - (25f * scale + dynamicWave),
            centerX - 70f * scale * dir, centerY - (40f * scale + tailSway),
            centerX - 95f * scale * dir, centerY - (10f * scale + tailSway)
        )
        // Center fin ruffle
        cubicTo(
            centerX - 80f * scale * dir, centerY + (10f * scale + dynamicWave),
            centerX - 90f * scale * dir, centerY + (35f * scale + tailSway),
            centerX - 65f * scale * dir, centerY + (45f * scale + dynamicWave)
        )
        // Lower flowing fin lobe returning to caudal peduncle
        cubicTo(
            centerX - 45f * scale * dir, centerY + (30f * scale),
            centerX - 25f * scale * dir, centerY + (15f * scale),
            centerX - 15f * scale * dir, centerY + (4f * scale)
        )
        close()
    }

    // Draw tail veil with luminous gradient
    drawPath(
        path = tailPath,
        brush = Brush.linearGradient(
            colors = listOf(
                secondaryColor.copy(alpha = 0.85f),
                primaryColor.copy(alpha = 0.65f),
                accentColor.copy(alpha = 0.25f)
            ),
            start = Offset(centerX - 15f * scale * dir, centerY),
            end = Offset(centerX - 95f * scale * dir, centerY)
        )
    )

    // Tail fin striations / rays
    drawPath(
        path = tailPath,
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)
        ),
        style = Stroke(width = 1.2f * scale)
    )

    // 2. Flowing Dorsal Fin (Top wave)
    val dorsalPath = Path().apply {
        moveTo(centerX - 5f * scale * dir, centerY - 10f * scale)
        cubicTo(
            centerX - 18f * scale * dir, centerY - (32f * scale + dynamicWave * 0.7f),
            centerX - 42f * scale * dir, centerY - (36f * scale + tailSway * 0.6f),
            centerX - 50f * scale * dir, centerY - 14f * scale
        )
        cubicTo(
            centerX - 35f * scale * dir, centerY - 12f * scale,
            centerX - 15f * scale * dir, centerY - 8f * scale,
            centerX - 5f * scale * dir, centerY - 10f * scale
        )
        close()
    }
    drawPath(
        path = dorsalPath,
        brush = Brush.linearGradient(
            colors = listOf(primaryColor.copy(alpha = 0.75f), secondaryColor.copy(alpha = 0.35f)),
            start = Offset(centerX, centerY - 10f * scale),
            end = Offset(centerX - 50f * scale * dir, centerY - 36f * scale)
        )
    )

    // 3. Flowing Ventral / Pelvic Fin (Bottom ribbon)
    val ventralPath = Path().apply {
        moveTo(centerX + 8f * scale * dir, centerY + 8f * scale)
        cubicTo(
            centerX + 2f * scale * dir, centerY + (30f * scale + dynamicWave),
            centerX - 15f * scale * dir, centerY + (45f * scale + tailSway),
            centerX - 24f * scale * dir, centerY + (55f * scale + tailSway)
        )
        cubicTo(
            centerX - 12f * scale * dir, centerY + (35f * scale),
            centerX + 2f * scale * dir, centerY + (20f * scale),
            centerX + 8f * scale * dir, centerY + 8f * scale
        )
        close()
    }
    drawPath(
        path = ventralPath,
        brush = Brush.linearGradient(
            colors = listOf(accentColor.copy(alpha = 0.8f), secondaryColor.copy(alpha = 0.3f))
        )
    )

    // 4. Betta Body Torpedo Shape (Streamlined & Muscular)
    val bodyPath = Path().apply {
        moveTo(centerX + 28f * scale * dir, centerY) // Snout
        // Top curvature
        cubicTo(
            centerX + 20f * scale * dir, centerY - 13f * scale,
            centerX + 5f * scale * dir, centerY - 15f * scale,
            centerX - 15f * scale * dir, centerY - 4f * scale
        )
        // Tail junction
        lineTo(centerX - 18f * scale * dir, centerY + 4f * scale)
        // Bottom curvature
        cubicTo(
            centerX + 4f * scale * dir, centerY + 14f * scale,
            centerX + 18f * scale * dir, centerY + 12f * scale,
            centerX + 28f * scale * dir, centerY
        )
        close()
    }

    // Body Gradient
    drawPath(
        path = bodyPath,
        brush = Brush.linearGradient(
            colors = listOf(primaryColor, secondaryColor),
            start = Offset(centerX + 28f * scale * dir, centerY),
            end = Offset(centerX - 18f * scale * dir, centerY)
        )
    )

    // Body highlight rim
    drawPath(
        path = bodyPath,
        color = Color.White.copy(alpha = 0.4f),
        style = Stroke(width = 1f * scale)
    )

    // 5. Bioluminescent Eye
    val eyeX = centerX + 18f * scale * dir
    val eyeY = centerY - 3f * scale
    drawCircle(
        color = Color(0xFF001524),
        radius = 3.2f * scale,
        center = Offset(eyeX, eyeY)
    )
    drawCircle(
        color = Color.White,
        radius = 1.2f * scale,
        center = Offset(eyeX + 0.6f * dir, eyeY - 0.6f)
    )
}
