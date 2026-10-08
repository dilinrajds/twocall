package com.twocall.chat.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.twocall.chat.ui.theme.*
import kotlin.math.max
import kotlin.math.min

/**
 * Modern Glassmorphic Image Preview / Viewer Screen Dialog.
 * Works in two modes:
 * 1. Preview mode before sending an image (with visible, prominent "Send Image" glass button)
 * 2. Fullscreen viewer mode for viewing existing chat photo messages with pinch-zoom + pan + double-tap zoom
 */
@Composable
fun ImageViewerDialog(
    imageModel: Any?, // File, Uri, or String path
    isSendingMode: Boolean = false,
    caption: String = "",
    onCaptionChange: (String) -> Unit = {},
    onSendImageClick: () -> Unit = {},
    onDismiss: () -> Unit
) {
    if (imageModel == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = !isSendingMode
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
        ) {
            if (isSendingMode) {
                // SEND MODE: simple non-zoomable preview
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Image Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 80.dp, horizontal = 12.dp)
                )
            } else {
                // VIEWER MODE: pinch-to-zoom + pan + double-tap zoom
                PinchZoomImageViewer(
                    imageModel = imageModel,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isSendingMode) "Preview Photo" else "Photo View",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            // Bottom Actions Container (Glassmorphic)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f))
                        ),
                        RoundedCornerShape(28.dp)
                    ),
                color = Color(0x3B1A2436)
            ) {
                if (isSendingMode) {
                    // PREVIEW BEFORE SENDING MODE: Caption + PROMINENT "SEND IMAGE" BUTTON
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Optional Caption Input
                        OutlinedTextField(
                            value = caption,
                            onValueChange = onCaptionChange,
                            placeholder = { Text("Add a caption...", color = Color.LightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )

                        // HIGHLY VISIBLE "SEND IMAGE" BUTTON
                        GlassButton(
                            onClick = {
                                onSendImageClick()
                                onDismiss()
                            },
                            gradientColors = listOf(NeonCyan, NeonIndigo),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "SEND IMAGE",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                } else {
                    // VIEWING MODE CONTROLS: hint
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "End-to-End Encrypted Photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pinch-to-zoom, pan, and double-tap-to-zoom image viewer.
 */
@Composable
private fun PinchZoomImageViewer(
    imageModel: Any?,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Animate back to 1f when scale resets
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "zoom_scale"
    )

    fun resetZoom() {
        scale = 1f
        offset = Offset.Zero
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = "Zoomed Photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(0.5f, 6f)
                        scale = newScale
                        // Only allow panning when zoomed in
                        if (newScale > 1f) {
                            offset += pan
                        } else {
                            offset = Offset.Zero
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.5f) {
                                resetZoom()
                            } else {
                                scale = min(3f, scale * 2.5f)
                            }
                        }
                    )
                }
        )
    }

    // Reset zoom when dialog closes
    DisposableEffect(Unit) {
        onDispose { resetZoom() }
    }
}
