package com.twocall.chat.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.ui.theme.*

/**
 * Premium Neumorphic Card with soft top-left highlight and bottom-right depth shadow.
 */
@Composable
fun ZippyNeumorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = NeumorphicBaseDark,
    highlightColor: Color = NeumorphicTopHighlight,
    shadowColor: Color = NeumorphicBottomShadow,
    borderStroke: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = shape, spotColor = shadowColor, ambientColor = shadowColor)
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = borderStroke,
                brush = Brush.linearGradient(
                    colors = listOf(highlightColor, Color.Transparent, shadowColor.copy(alpha = 0.4f)),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = shape
            ),
        content = content
    )
}

/**
 * Interactive Neumorphic Button with tactile press depression, aquatic gradient accent, and haptics.
 */
@Composable
fun ZippyNeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: ImageVector? = null,
    gradient: Brush = BettaFlowGradient,
    shape: Shape = RoundedCornerShape(20.dp),
    enabled: Boolean = true
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(120),
        label = "btn_press_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 4.dp else 10.dp,
                shape = shape,
                spotColor = AquaCyan.copy(alpha = 0.3f),
                ambientColor = NeumorphicBottomShadow
            )
            .clip(shape)
            .background(if (enabled) gradient else Brush.linearGradient(listOf(Color(0xFF1E283D), Color(0xFF1E283D))))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                if (text != null) Spacer(modifier = Modifier.width(8.dp))
            }
            if (text != null) {
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * Circular Neumorphic Icon Button with soft depth and glowing border highlight.
 */
@Composable
fun ZippyNeumorphicIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    iconTint: Color = AquaCyan,
    backgroundColor: Color = NeumorphicBaseDark,
    glowColor: Color = AquaCyan.copy(alpha = 0.25f),
    isSelected: Boolean = false
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(100),
        label = "icon_btn_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = if (isSelected) 10.dp else 6.dp,
                shape = CircleShape,
                spotColor = glowColor,
                ambientColor = NeumorphicBottomShadow
            )
            .clip(CircleShape)
            .background(if (isSelected) glowColor else backgroundColor)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isSelected) listOf(AquaCyan, BettaViolet)
                    else listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) Color.White else iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Circular Call Control Button (e.g. Accept, Decline, End, Mute) with aquatic halo.
 */
@Composable
fun ZippyCallButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    iconSize: Dp = 32.dp,
    backgroundColor: Color,
    iconTint: Color = Color.White,
    glowColor: Color = backgroundColor.copy(alpha = 0.45f)
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = tween(120),
        label = "call_btn_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(elevation = 16.dp, shape = CircleShape, spotColor = glowColor, ambientColor = glowColor)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = 1.5.dp,
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                    radius = 80f
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
