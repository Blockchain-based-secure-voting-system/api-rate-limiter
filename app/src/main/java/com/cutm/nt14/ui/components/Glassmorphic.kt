package com.cutm.nt14.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ========================================================
// White, Black & Cobalt Blue iOS Glassmorphism Palette
// ========================================================

// Authentic Cobalt Blue (#0047AB)
val CobaltBlue = Color(0xFF0047AB)
val CobaltBlueDark = Color(0xFF002F75)
val CobaltBlueLight = Color(0xFFEEF4FF)
val CobaltBlueAccent = Color(0xFF0D5CD6)
val CobaltBlueBorder = Color(0xFFBFD7FE)

// Crisp White Canvas & Surfaces
val PureWhite = Color(0xFFFFFFFF)
val PolyWhiteBg = Color(0xFFF8FAFD)
val PolyWhiteCanvasEnd = Color(0xFFF1F5F9)

// Translucent White Glass Surfaces
val GlassSurfaceWhite = Color.White.copy(alpha = 0.88f)
val GlassSurfaceWhiteTranslucent = Color.White.copy(alpha = 0.65f)
val GlassSurfaceWhiteSolid = Color.White.copy(alpha = 0.98f)

// Crisp White and Cobalt Tint Borders
val GlassBorderWhite = Brush.verticalGradient(
    listOf(
        Color.White,
        Color(0xFFE2E8F0).copy(alpha = 0.85f)
    )
)
val GlassBorderCobalt = Brush.verticalGradient(
    listOf(
        CobaltBlueBorder.copy(alpha = 0.85f),
        Color(0xFFE2E8F0).copy(alpha = 0.60f)
    )
)
val GlassBorderSubtle = Brush.verticalGradient(
    listOf(
        Color.White.copy(alpha = 0.95f),
        Color(0xFFCBD5E1).copy(alpha = 0.50f)
    )
)

// Brand Core: Cobalt Blue
val PolyPrimary = CobaltBlue
val PolyPrimaryDark = CobaltBlueDark
val PolyPrimaryLight = CobaltBlueLight
val PolyPurple = CobaltBlue
val PolyCyan = CobaltBlueAccent

// Functional Status Accents
val PolySuccess = Color(0xFF059669) // Crisp Emerald
val PolySuccessBg = Color(0xFFECFDF5)
val PolyWarning = Color(0xFFD97706) // Crisp Amber
val PolyWarningBg = Color(0xFFFFFBEB)
val PolyDanger = Color(0xFFDC2626)  // Crisp Crimson
val PolyDangerBg = Color(0xFFFEF2F2)

// High-Contrast Black & Charcoal Text Hierarchy
val PolyBlack = Color(0xFF000000)
val PolyTextPrimary = Color(0xFF0A0F1D)    // Near-black sharp text
val PolyTextSecondary = Color(0xFF334155)  // Dark slate charcoal
val PolyTextMuted = Color(0xFF64748B)      // Muted slate gray

// Backward compatibility alias for theme
val GlassBackgroundDark = PolyWhiteBg
val AppleBlue = CobaltBlue
val AppleCyan = CobaltBlueAccent
val AppleGreen = PolySuccess
val AppleOrange = PolyWarning
val AppleRed = PolyDanger
val ApplePurple = CobaltBlue

/**
 * Root clean light canvas with subtle, elegant atmospheric tints
 * (soft Cobalt Blue & crisp white mist), perfect for transparent white glass surfaces.
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF8FAFD),
                        Color(0xFFEEF4FF).copy(alpha = 0.45f)
                    )
                )
            )
    ) {
        // Soft ambient top-right cobalt mist
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = (-50).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CobaltBlue.copy(alpha = 0.09f),
                            CobaltBlueLight.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Soft ambient bottom-left clean slate tint
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 60.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE2E8F0).copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        content()
    }
}

/**
 * Transparent White Frosted Glass Card with crisp white borders,
 * soft diffused ambient elevation, and squircle rounded corners.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderBrush: Brush = GlassBorderWhite,
    borderWidth: Dp = 1.dp,
    backgroundColor: Color = GlassSurfaceWhite,
    elevation: Dp = 3.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Column(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color(0x0F0F172A),
                spotColor = Color(0x140F172A)
            )
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(borderWidth, borderBrush), shape)
            .then(clickableModifier)
            .padding(16.dp),
        content = content
    )
}

/**
 * Clean Transparent White Button or Solid Indigo Brand Button.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = PolyPrimary,
    isFilled: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val bgModifier = if (isFilled) {
        Modifier.background(if (enabled) accentColor else accentColor.copy(alpha = 0.4f))
    } else {
        Modifier
            .background(Color.White.copy(alpha = if (enabled) 0.90f else 0.5f))
            .border(
                BorderStroke(
                    1.dp,
                    if (enabled) accentColor.copy(alpha = 0.35f) else Color(0xFFE2E8F0)
                ),
                shape
            )
    }

    val textColor = if (isFilled) Color.White else if (enabled) accentColor else PolyTextMuted

    Box(
        modifier = modifier
            .shadow(if (isFilled) 3.dp else 1.dp, shape, ambientColor = Color(0x080F172A))
            .clip(shape)
            .then(bgModifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 0.2.sp
            )
        }
    }
}

/**
 * Clean Translucent Status Pill Badge.
 */
@Composable
fun GlassBadge(
    text: String,
    color: Color = PolySuccess,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.30f)), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}
