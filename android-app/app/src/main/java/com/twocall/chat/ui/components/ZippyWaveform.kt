package com.twocall.chat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.twocall.chat.ui.theme.*
import kotlin.math.sin

/**
 * Aquatic fluid audio waveform resembling flowing Betta fins / ocean waves.
 */
@Composable
fun ZippyWaveform(
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    barCount: Int = 26,
    isRecording: Boolean = true,
    amplitude: Float = 0.5f,
    primaryColor: Color = AquaCyan,
    secondaryColor: Color = BettaViolet
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_motion")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalWidth = size.width
        val midY = size.height / 2f
        val step = totalWidth / barCount.toFloat()
        val maxBarH = size.height * 0.85f

        for (i in 0 until barCount) {
            val normX = i.toFloat() / barCount.toFloat()
            // Sine modulation combined with user amplitude
            val waveVal = if (isRecording) {
                val wave1 = sin(phase + normX * 8f)
                val wave2 = sin(phase * 1.5f + normX * 12f) * 0.5f
                ((wave1 + wave2) * 0.5f).coerceIn(-1f, 1f)
            } else 0.1f

            val currentH = (4.dp.toPx() + (waveVal * 0.5f + 0.5f) * maxBarH * amplitude.coerceIn(0.2f, 1f))
                .coerceAtLeast(3.dp.toPx())

            val x = i * step + step / 2f

            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor, secondaryColor),
                    startY = midY - currentH / 2f,
                    endY = midY + currentH / 2f
                ),
                start = Offset(x, midY - currentH / 2f),
                end = Offset(x, midY + currentH / 2f),
                strokeWidth = (step * 0.45f).coerceAtLeast(2.dp.toPx()),
                cap = StrokeCap.Round
            )
        }
    }
}
