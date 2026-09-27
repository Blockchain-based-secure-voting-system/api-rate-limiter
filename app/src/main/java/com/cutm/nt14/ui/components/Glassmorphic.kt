package com.cutm.nt14.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// iOS Glassmorphism Palette
val GlassBackgroundDark = Color(0xFF08090E)
val GlassSurfaceDark = Color(0xFF141724).copy(alpha = 0.68f)
val GlassSurfaceLight = Color(0xFF1E2235).copy(alpha = 0.45f)
val GlassBorderGradient = Brush.linearGradient(
    listOf(
        Color.White.copy(alpha = 0.28f),
        Color.White.copy(alpha = 0.06f),
        Color(0xFF00C6FF).copy(alpha = 0.18f)
    )
)
val GlassBorderSubtle = Brush.linearGradient(
    listOf(
        Color.White.copy(alpha = 0.15f),
        Color.White.copy(alpha = 0.03f)
    )
)

val AppleBlue = Color(0xFF0A84FF)
val AppleGreen = Color(0xFF30D158)
val AppleRed = Color(0xFFFF453A)
val AppleOrange = Color(0xFFFF9F0A)
val ApplePurple = Color(0xFFBF5AF2)
val AppleCyan = Color(0xFF64D2FF)

/**
 * Root container providing dynamic ambient glowing gradient orbs
 * behind frosted glass surfaces to create true iOS glassmorphism depth.
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_float"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GlassBackgroundDark)
    ) {
        // Glowing Ambient Orb 1: Violet/Indigo (Top-Left)
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-40 + animOffset).dp, y = (-20).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF5E17EB).copy(alpha = 0.38f),
                            Color(0xFF3B82F6).copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
                .blur(60.dp)
        )

        // Glowing Ambient Orb 2: Electric Cyan (Top-Right)
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (100 - animOffset).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00F2FE).copy(alpha = 0.32f),
                            Color(0xFF4FACFE).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .blur(50.dp)
        )

        // Glowing Ambient Orb 3: Coral/Rose (Bottom-Center)
        Box(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (80 + animOffset).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF2A6D).copy(alpha = 0.22f),
                            Color(0xFF7000FF).copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
                .blur(70.dp)
        )

        // Screen Content Layer
        content()
    }
}

/**
 * Frosted translucent glass card container with fine glowing specular border
 * and squircle rounded corners.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    borderBrush: Brush = GlassBorderGradient,
    borderWidth: Dp = 1.dp,
    backgroundColor: Color = GlassSurfaceDark,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(borderWidth, borderBrush), shape)
            .then(clickableModifier)
            .padding(16.dp),
        content = content
    )
}

/**
 * Sleek iOS-style glass button with frosted acrylic styling and subtle glow.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = AppleBlue,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accentColor.copy(alpha = if (enabled) 0.35f else 0.15f),
                        accentColor.copy(alpha = if (enabled) 0.18f else 0.08f)
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = if (enabled) 0.60f else 0.20f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    )
                ),
                shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
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
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                letterSpacing = 0.3.sp
            )
        }
    }
}

/**
 * Micro glass badge with active status dot and translucent pill border.
 */
@Composable
fun GlassBadge(
    text: String,
    color: Color = AppleGreen,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.35f)), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
