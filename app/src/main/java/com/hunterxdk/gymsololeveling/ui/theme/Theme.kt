package com.hunterxdk.gymsololeveling.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = BackgroundDark,
    primaryContainer = GoldAccentDark,
    onPrimaryContainer = TextPrimary,
    secondary = XPGreen,
    onSecondary = BackgroundDark,
    secondaryContainer = XPGreen,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = TextPrimary,
)

private val LightColorScheme = lightColorScheme(
    primary = GoldAccentDark,
    secondary = XPGreen,
)

@Composable
fun GymLevelsTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = GymLevelsTypography,
        content = content,
    )
}
