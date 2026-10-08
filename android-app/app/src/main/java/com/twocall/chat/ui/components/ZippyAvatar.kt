package com.twocall.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.ui.theme.*

/**
 * Premium Neumorphic Avatar with Betta Fish aquatic halo and online indicator.
 */
@Composable
fun ZippyAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    isOnline: Boolean = true,
    showOnlineDot: Boolean = true,
    imageBase64: String? = null
) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "P"

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Soft outer shadow & aquatic border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation = 8.dp, shape = CircleShape, spotColor = AquaCyan.copy(alpha = 0.25f))
                .clip(CircleShape)
                .background(NeumorphicBaseDark)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(AquaCyan, BettaViolet)),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            val bytes = androidx.compose.runtime.remember(imageBase64) {
                try { imageBase64?.takeIf { it.isNotBlank() }?.let { android.util.Base64.decode(it, android.util.Base64.DEFAULT) } }
                catch (e: IllegalArgumentException) { null }
            }
            if (bytes != null) coil.compose.AsyncImage(
                model = bytes, contentDescription = "$name profile photo",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            ) else Text(
                text = initial,
                color = Color.White,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Online Status Dot with subtle pulse
        if (showOnlineDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(if (isOnline) OnlineGreen else OfflineGray)
                    .border(1.5.dp, AbyssNavy, CircleShape)
            )
        }
    }
}
