package com.twocall.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.ui.components.*
import com.twocall.chat.ui.theme.*

@Composable
fun PairingWelcomeScreen(
    onCreatePairClick: () -> Unit,
    onJoinPartnerClick: () -> Unit
) {
    ZippyAquaticBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top App Title & Twin Betta Encounter Animation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "ZippyCall",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Private. Just for two.",
                    fontSize = 14.sp,
                    color = AquaCyan,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Beautiful Twin Betta Encounter Visualization
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(AquaticSurface.copy(alpha = 0.6f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ZippyConcentricRipples(
                        modifier = Modifier.fillMaxSize(),
                        baseColor = AquaCyan.copy(alpha = 0.6f),
                        rippleCount = 2,
                        maxRadius = 95.dp
                    )
                    ZippyTwinBettaEncounter(
                        modifier = Modifier.size(190.dp),
                        progress = 0.85f
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                ZippyNeumorphicCard(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = AquaticSurface.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = "Like two Betta fish in calm waters, connect your device once with your partner through end-to-end encryption.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = TextSecondaryDark,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            // Neumorphic Action Options
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Create New Pair Code
                ZippyNeumorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCreatePairClick() },
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = NeumorphicBaseDark,
                    borderStroke = 1.2.dp,
                    highlightColor = AquaCyan.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(AquaCyan, OceanIndigo))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddLink,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Create Private Pair",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Generate a 6-digit code for your partner",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = AquaCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Card 2: Join Partner Code
                ZippyNeumorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onJoinPartnerClick() },
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = NeumorphicBaseDark,
                    borderStroke = 1.2.dp,
                    highlightColor = BettaViolet.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(BettaViolet, BiolumPink))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Join Partner's Space",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Enter the 6 digits shared by your partner",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = BiolumPink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
