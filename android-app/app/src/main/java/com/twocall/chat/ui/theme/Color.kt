package com.twocall.chat.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================================
// ZippyCall - Aquatic Neumorphic Design Tokens (Betta Fish + Private Water Theme)
// ============================================================================

// Deep Ocean Abyss - Dark Mode Foundations
val AbyssNavy = Color(0xFF040812)               // Deepest background
val DeepOcean = Color(0xFF0A1324)               // Secondary ocean backdrop
val AquaticSurface = Color(0xFF101F3B)          // Elevated surface
val AquaticSurfaceVariant = Color(0xFF162A4F)   // High-elevation card surface

// Soft Pearl / Sea Foam - Light Mode Foundations
val PearlBackground = Color(0xFFF0F5FA)
val PearlSurface = Color(0xFFE5EEF7)
val PearlSurfaceVariant = Color(0xFFD9E6F2)
val PearlTextPrimary = Color(0xFF0D1B2A)
val PearlTextSecondary = Color(0xFF415A77)

// Bioluminescent Aquatic Accents (Electric Betta Fins)
val AquaCyan = Color(0xFF00F2FE)                // Primary vibrant aqua
val OceanIndigo = Color(0xFF4FACFE)             // Flowing fin electric blue
val BettaViolet = Color(0xFF7B61FF)             // Intimate caudal fin violet
val BiolumPink = Color(0xFFFF4B72)              // Warm intimate Betta coral/magenta
val LuminousTeal = Color(0xFF00E5A3)            // Call accept / online status
val AquaticDecline = Color(0xFFFF3B5C)          // Call decline / destructive

// Text & Content Hierarchy
val TextPrimaryDark = Color(0xFFF1F5F9)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

// Neumorphic Soft Depths & Glow Tokens
val NeumorphicBaseDark = Color(0xFF0C1629)
val NeumorphicTopHighlight = Color(0x334FACFE)  // Soft light bevel from top-left
val NeumorphicBottomShadow = Color(0x99010307)  // Deep shadow towards bottom-right
val NeumorphicInnerGlow = Color(0x1F00F2FE)     // Subtle internal luminance

// Water Glass Tokens
val GlassSurfaceDark = Color(0x1A00F2FE)
val GlassBorderDark = Color(0x2600F2FE)
val GlassHighlightDark = Color(0x40FFFFFF)

// Call & Connection State Tokens
val CallAcceptGreen = LuminousTeal
val CallRejectRed = AquaticDecline
val OnlineGreen = LuminousTeal
val OfflineGray = Color(0xFF64748B)

// Gradients
val BettaFlowGradient = Brush.linearGradient(
    listOf(AquaCyan, OceanIndigo, BettaViolet)
)
val BettaTwinGradient = Brush.linearGradient(
    listOf(AquaCyan, BiolumPink)
)
val OceanAbyssGradient = Brush.verticalGradient(
    listOf(AbyssNavy, DeepOcean)
)
val OutgoingMessageGradient = Brush.linearGradient(
    listOf(Color(0xFF0083B0), Color(0xFF00B4DB))
)

// Legacy alias compatibility so no existing code breaks
val DarkBackground = AbyssNavy
val DarkSurface = DeepOcean
val DarkSurfaceVariant = AquaticSurface
val DarkPrimary = AquaCyan
val DarkOnPrimary = Color(0xFF001A24)
val DarkSecondary = OceanIndigo
val DarkOnBackground = TextPrimaryDark
val DarkOnSurface = TextPrimaryDark
val DarkOnSurfaceVariant = TextSecondaryDark
val DarkError = AquaticDecline

val LightBackground = PearlBackground
val LightSurface = PearlSurface
val LightSurfaceVariant = PearlSurfaceVariant
val LightPrimary = Color(0xFF0077B6)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightSecondary = OceanIndigo
val LightOnBackground = PearlTextPrimary
val LightOnSurface = PearlTextPrimary
val LightOnSurfaceVariant = PearlTextSecondary
val LightError = AquaticDecline

val NeonCyan = AquaCyan
val NeonIndigo = OceanIndigo
val NeonPurple = BettaViolet
val NeonEmerald = LuminousTeal

val OutgoingBubbleDark = Color(0x4D0083B0)
val OutgoingBubbleBorder = Color(0x6600F2FE)
val IncomingBubbleDark = Color(0x33101F3B)
val IncomingBubbleBorder = Color(0x334FACFE)

val BettaCoral = BiolumPink
val DeepAquaticBlue = DeepOcean
