package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object TerminalTheme {
    val colors: TerminalPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalTerminalPalette.current
}

@Composable
fun SignalDeskTheme(
    themeId: DeskThemeId = DeskThemeId.OBSIDIAN_GOLD,
    content: @Composable () -> Unit
) {
    val palette = paletteForTheme(themeId)
    val colorScheme = darkColorScheme(
        primary = palette.emeraldBuy,
        onPrimary = palette.background,
        primaryContainer = palette.emeraldContainer,
        onPrimaryContainer = palette.emeraldBuy,
        secondary = palette.goldAccent,
        onSecondary = palette.background,
        secondaryContainer = palette.goldContainer,
        onSecondaryContainer = palette.goldAccentBright,
        tertiary = palette.cyanInfo,
        onTertiary = palette.background,
        tertiaryContainer = palette.cyanContainer,
        onTertiaryContainer = palette.cyanInfo,
        error = palette.crimsonSell,
        onError = palette.textPrimary,
        errorContainer = palette.crimsonContainer,
        onErrorContainer = palette.crimsonSell,
        background = palette.background,
        onBackground = palette.textPrimary,
        surface = palette.surface,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surfaceElevated,
        onSurfaceVariant = palette.textSecondary,
        outline = palette.border,
        outlineVariant = palette.borderSubtle
    )

    CompositionLocalProvider(LocalTerminalPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SignalDeskTypography,
            content = content
        )
    }
}
