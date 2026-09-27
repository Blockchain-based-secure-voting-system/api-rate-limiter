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

// ==========================================
// PolyLance Clean Transparent White Palette
// ==========================================
val PolyWhiteBg = Color(0xFFF8FAFC)
val PolyWhiteCanvasEnd = Color(0xFFF1F5F9)

// Translucent White Glass Surfaces
val GlassSurfaceWhite = Color.White.copy(alpha = 0.82f)
val GlassSurfaceWhiteTranslucent = Color.White.copy(alpha = 0.65f)
val GlassSurfaceWhiteSolid = Color.White.copy(alpha = 0.95f)

// Crisp Translucent White Borders
val GlassBorderWhite = Brush.verticalGradient(
    listOf(
        Color.White,
        Color(0xFFE2E8F0).copy(alpha = 0.85f)
    )
)
val GlassBorderSubtle = Brush.verticalGradient(
    listOf(
        Color.White.copy(alpha = 0.9f),
        Color(0xFFCBD5E1).copy(alpha = 0.5f)
    )
)

// PolyLance Brand Accents
val PolyPrimary = Color(0xFF6366F1) // Indigo / Violet
val PolyPrimaryDark = Color(0xFF4F46E5)
val PolyPrimaryLight = Color(0xFFEEF2FF)
val PolyPurple = Color(0xFF7C3AED) // Sovereign Purple
val PolySuccess = Color(0xFF10B981) // Emerald Green
val PolySuccessBg = Color(0xFFECFDF5)
val PolyWarning = Color(0xFFF59E0B) // Amber
val PolyWarningBg = Color(0xFFFFFBEB)
val PolyDanger = Color(0xFFEF4444) // Red
val PolyDangerBg = Color(0xFFFEF2F2)
val PolyCyan = Color(0xFF0284C7)

// Text Hierarchy
val PolyTextPrimary = Color(0xFF0F172A)
val PolyTextSecondary = Color(0xFF475569)
val PolyTextMuted = Color(0xFF94A3B8)

// Backward compatibility alias for theme
val GlassBackgroundDark = PolyWhiteBg
val AppleBlue = PolyPrimary
val AppleCyan = PolyCyan
val AppleGreen = PolySuccess
val AppleOrange = PolyWarning
val AppleRed = PolyDanger
val ApplePurple = PolyPurple

/**
 * Root clean light canvas with subtle, elegant atmospheric tints
 * (soft PolyLance lavender & sky tones), perfect for transparent white glass surfaces.
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
                        Color(0xFFF8FAFC),
                        Color(0xFFF1F5F9),
                        Color(0xFFEDE9FE).copy(alpha = 0.35f)
                    )
                )
            )
    ) {
        // Soft ambient top-right pastel tint (PolyLance lavender)
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = (-50).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFC7D2FE).copy(alpha = 0.35f),
                            Color(0xFFE0E7FF).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Soft ambient bottom-left pastel tint (PolyLance sky)
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 60.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFBAE6FD).copy(alpha = 0.30f),
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
