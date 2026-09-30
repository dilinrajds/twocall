package com.twocall.chat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.twocall.chat.ui.theme.*

@Composable
fun AnimatedGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bgGradient")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientAnim"
    )

    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF090D16),
            Color(0xFF0F172A),
            Color(0xFF181024),
            Color(0xFF0B1424)
        ),
        start = androidx.compose.ui.geometry.Offset(animOffset, 0f),
        end = androidx.compose.ui.geometry.Offset(0f, animOffset + 1000f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // Glowing background ambient circles
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80).dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonPurple.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )

        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = Color(0x1F2B3648),
    borderColor: Color = Color(0x33FFFFFF),
    borderWidth: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clip(shape)
            .border(
                borderWidth,
                Brush.linearGradient(
                    listOf(
                        borderColor,
                        borderColor.copy(alpha = 0.08f),
                        borderColor.copy(alpha = 0.25f)
                    )
                ),
                shape
            ),
        color = backgroundColor,
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradientColors: List<Color> = listOf(NeonCyan, NeonIndigo),
    shape: Shape = RoundedCornerShape(18.dp),
    content: @Composable RowScope.() -> Unit
) {
    val brush = if (enabled) {
        Brush.horizontalGradient(gradientColors)
    } else {
        Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.2f)))
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent)),
                shape
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}
