package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
    isLightMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = paletteForTheme(themeId, isLightMode)
    val onPrimaryColor = if (isLightMode) palette.surface else palette.background
    val colorScheme = if (isLightMode) {
        lightColorScheme(
            primary = palette.emeraldBuy,
            onPrimary = onPrimaryColor,
            primaryContainer = palette.emeraldContainer,
            onPrimaryContainer = palette.emeraldBuy,
            secondary = palette.goldAccent,
            onSecondary = onPrimaryColor,
            secondaryContainer = palette.goldContainer,
            onSecondaryContainer = palette.goldAccentBright,
            tertiary = palette.cyanInfo,
            onTertiary = onPrimaryColor,
            tertiaryContainer = palette.cyanContainer,
            onTertiaryContainer = palette.cyanInfo,
            error = palette.crimsonSell,
            onError = palette.surface,
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
    } else {
        darkColorScheme(
            primary = palette.emeraldBuy,
            onPrimary = onPrimaryColor,
            primaryContainer = palette.emeraldContainer,
            onPrimaryContainer = palette.emeraldBuy,
            secondary = palette.goldAccent,
            onSecondary = onPrimaryColor,
            secondaryContainer = palette.goldContainer,
            onSecondaryContainer = palette.goldAccentBright,
            tertiary = palette.cyanInfo,
            onTertiary = onPrimaryColor,
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
    }

    CompositionLocalProvider(LocalTerminalPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SignalDeskTypography,
            content = content
        )
    }
}
