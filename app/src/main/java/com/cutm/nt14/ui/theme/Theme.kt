package com.cutm.nt14.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CobaltBlue,
    onPrimary = PureWhite,
    primaryContainer = CobaltBlueDark,
    onPrimaryContainer = CobaltBlueLight,
    secondary = CobaltBlueAccent,
    onSecondary = PureWhite,
    background = CarbonBlack,
    onBackground = PureWhite,
    surface = CharcoalBlack,
    onSurface = PureWhite
)

private val LightColorScheme = lightColorScheme(
    primary = CobaltBlue,
    onPrimary = PureWhite,
    primaryContainer = CobaltBlueLight,
    onPrimaryContainer = CobaltBlueDark,
    secondary = SlateBlack,
    onSecondary = PureWhite,
    background = OffWhite,
    onBackground = CarbonBlack,
    surface = PureWhite,
    onSurface = CarbonBlack
)

@Composable
fun NT14Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
