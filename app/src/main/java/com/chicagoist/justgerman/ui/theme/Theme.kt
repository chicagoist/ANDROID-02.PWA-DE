package com.chicagoist.justgerman.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = FlagBlack,
    primaryContainer = GoldDark,
    onPrimaryContainer = FlagBlack,
    secondary = Zinc700,
    onSecondary = Zinc100,
    secondaryContainer = Zinc800,
    onSecondaryContainer = Zinc200,
    tertiary = FlagRed,
    onTertiary = Zinc100,
    error = FlagRed,
    onError = Zinc100,
    background = Zinc950,
    onBackground = Zinc100,
    surface = Zinc900,
    onSurface = Zinc100,
    surfaceVariant = Zinc800,
    onSurfaceVariant = Zinc300,
    outline = Zinc700,
    outlineVariant = Zinc800
)

@Composable
fun JustGermanTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
