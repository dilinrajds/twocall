package com.twocall.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.ui.components.*
import com.twocall.chat.ui.theme.*

@Composable
fun PairingSuccessScreen(
    onOpenConversation: () -> Unit
) {
    ZippyAquaticBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                // Two Betta Fish in harmonious union
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(AquaCyan.copy(alpha = 0.25f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ZippyConcentricRipples(
                        modifier = Modifier.fillMaxSize(),
                        baseColor = LuminousTeal,
                        rippleCount = 3,
                        maxRadius = 80.dp
                    )
                    ZippyTwinBettaEncounter(
                        modifier = Modifier.size(160.dp),
                        progress = 1.0f
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Connected ",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp
                        ),
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Heart",
                        tint = BiolumPink,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Your private sanctuary is open.\nZero central storage • Hardware-bound AES-256",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = TextSecondaryDark,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                ZippyNeumorphicCard(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = AquaticSurface.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = LuminousTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "End-to-End Encrypted Forever",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            ZippyNeumorphicButton(
                onClick = onOpenConversation,
                modifier = Modifier.fillMaxWidth(),
                text = "Enter Private Sanctuary",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                gradient = BettaFlowGradient
            )
        }
    }
}
