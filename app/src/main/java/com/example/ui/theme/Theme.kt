package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryAccent,
    onPrimary = DarkBackground,
    primaryContainer = DarkElevatedSurface,
    onPrimaryContainer = DarkPrimaryAccent,
    secondary = DarkMutedGold,
    onSecondary = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkElevatedSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimaryAccent,
    onPrimary = LightSurface,
    primaryContainer = LightElevatedSurface,
    onPrimaryContainer = LightPrimaryAccent,
    secondary = LightGoldHighlight,
    onSecondary = LightSurface,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightElevatedSurface,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline
)

@Composable
fun MusabbihTheme(
    themePreference: String = "SYSTEM", // "LIGHT", "DARK", "SYSTEM"
    content: @Composable () -> Unit
) {
    val isDark = when (themePreference) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MusabbihTypography,
            content = content
        )
    }
}
