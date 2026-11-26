package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class DeskThemeId(
    val displayName: String,
    val subtitle: String
) {
    OBSIDIAN_GOLD("Obsidian Gold", "Deep obsidian with emerald & brushed gold"),
    CYBER_MATRIX("Cyber Matrix", "Pitch black with electric cyan & neon mint"),
    ROYAL_PLATINUM("Royal Platinum", "Sapphire navy with champagne gold"),
    STEALTH_CARBON("Stealth Carbon", "Matte charcoal with crisp mint & coral");

    companion object {
        fun fromRaw(raw: String?): DeskThemeId =
            entries.firstOrNull { it.name == raw } ?: OBSIDIAN_GOLD
    }
}

@Immutable
data class TerminalPalette(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHighlight: Color,
    val border: Color,
    val borderSubtle: Color,
    val emeraldBuy: Color,
    val emeraldBuyDim: Color,
    val emeraldContainer: Color,
    val crimsonSell: Color,
    val crimsonSellDim: Color,
    val crimsonContainer: Color,
    val goldAccent: Color,
    val goldAccentBright: Color,
    val goldContainer: Color,
    val cyanInfo: Color,
    val cyanContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color
)

fun paletteForTheme(
    themeId: DeskThemeId,
    isLightMode: Boolean = false
): TerminalPalette = if (isLightMode) {
    lightPaletteForTheme(themeId)
} else {
    darkPaletteForTheme(themeId)
}

private fun darkPaletteForTheme(themeId: DeskThemeId): TerminalPalette = when (themeId) {
    DeskThemeId.OBSIDIAN_GOLD -> TerminalPalette(
        background = Color(0xFF070A0F),
        surface = Color(0xFF0E141D),
        surfaceElevated = Color(0xFF151E2B),
        surfaceHighlight = Color(0xFF1C2738),
        border = Color(0xFF233044),
        borderSubtle = Color(0xFF182231),
        emeraldBuy = Color(0xFF00E676),
        emeraldBuyDim = Color(0xFF00B85C),
        emeraldContainer = Color(0xFF0A2A1C),
        crimsonSell = Color(0xFFFF4D6A),
        crimsonSellDim = Color(0xFFD93654),
        crimsonContainer = Color(0xFF2E1019),
        goldAccent = Color(0xFFFFB300),
        goldAccentBright = Color(0xFFFFCA28),
        goldContainer = Color(0xFF2B2009),
        cyanInfo = Color(0xFF38BDF8),
        cyanContainer = Color(0xFF0C2433),
        textPrimary = Color(0xFFF0F4FA),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF64748B)
    )

    DeskThemeId.CYBER_MATRIX -> TerminalPalette(
        background = Color(0xFF04070B),
        surface = Color(0xFF0A1018),
        surfaceElevated = Color(0xFF101925),
        surfaceHighlight = Color(0xFF172334),
        border = Color(0xFF1C324A),
        borderSubtle = Color(0xFF132234),
        emeraldBuy = Color(0xFF00F59B),
        emeraldBuyDim = Color(0xFF00C47D),
        emeraldContainer = Color(0xFF07291E),
        crimsonSell = Color(0xFFFF3B6B),
        crimsonSellDim = Color(0xFFD92B57),
        crimsonContainer = Color(0xFF2F0D1A),
        goldAccent = Color(0xFF00E5FF),
        goldAccentBright = Color(0xFF64F3FF),
        goldContainer = Color(0xFF082732),
        cyanInfo = Color(0xFF38BDF8),
        cyanContainer = Color(0xFF0C2433),
        textPrimary = Color(0xFFEAF6FF),
        textSecondary = Color(0xFF8CA6C1),
        textMuted = Color(0xFF56708B)
    )

    DeskThemeId.ROYAL_PLATINUM -> TerminalPalette(
        background = Color(0xFF080E1B),
        surface = Color(0xFF0F192D),
        surfaceElevated = Color(0xFF17233D),
        surfaceHighlight = Color(0xFF202F50),
        border = Color(0xFF2B3E66),
        borderSubtle = Color(0xFF1B2947),
        emeraldBuy = Color(0xFF10E88A),
        emeraldBuyDim = Color(0xFF0BB86C),
        emeraldContainer = Color(0xFF0B2C24),
        crimsonSell = Color(0xFFFF5277),
        crimsonSellDim = Color(0xFFD93B5E),
        crimsonContainer = Color(0xFF311220),
        goldAccent = Color(0xFFF5B700),
        goldAccentBright = Color(0xFFFFD147),
        goldContainer = Color(0xFF2E240B),
        cyanInfo = Color(0xFF60A5FA),
        cyanContainer = Color(0xFF122544),
        textPrimary = Color(0xFFF4F7FD),
        textSecondary = Color(0xFF9CB0D1),
        textMuted = Color(0xFF687CA0)
    )

    DeskThemeId.STEALTH_CARBON -> TerminalPalette(
        background = Color(0xFF0E1014),
        surface = Color(0xFF161920),
        surfaceElevated = Color(0xFF1E222B),
        surfaceHighlight = Color(0xFF262B36),
        border = Color(0xFF2F3644),
        borderSubtle = Color(0xFF222731),
        emeraldBuy = Color(0xFF2EE59D),
        emeraldBuyDim = Color(0xFF22B87D),
        emeraldContainer = Color(0xFF0F2920),
        crimsonSell = Color(0xFFFF5C72),
        crimsonSellDim = Color(0xFFD94559),
        crimsonContainer = Color(0xFF2F141A),
        goldAccent = Color(0xFFF59E0B),
        goldAccentBright = Color(0xFFFBBF24),
        goldContainer = Color(0xFF2B200B),
        cyanInfo = Color(0xFF38BDF8),
        cyanContainer = Color(0xFF102430),
        textPrimary = Color(0xFFF3F4F6),
        textSecondary = Color(0xFF9CA3AF),
        textMuted = Color(0xFF6B7280)
    )
}

private fun lightPaletteForTheme(themeId: DeskThemeId): TerminalPalette = when (themeId) {
    DeskThemeId.OBSIDIAN_GOLD -> TerminalPalette(
        background = Color(0xFFF4F6FA),
        surface = Color(0xFFFFFFFF),
        surfaceElevated = Color(0xFFF0F4F9),
        surfaceHighlight = Color(0xFFE4EAF3),
        border = Color(0xFFD4DEEC),
        borderSubtle = Color(0xFFE5ECF5),
        emeraldBuy = Color(0xFF059669),
        emeraldBuyDim = Color(0xFF047857),
        emeraldContainer = Color(0xFFD1FAE5),
        crimsonSell = Color(0xFFDC2626),
        crimsonSellDim = Color(0xFFB91C1C),
        crimsonContainer = Color(0xFFFEE2E2),
        goldAccent = Color(0xFFB45309),
        goldAccentBright = Color(0xFFD97706),
        goldContainer = Color(0xFFFEF3C7),
        cyanInfo = Color(0xFF0284C7),
        cyanContainer = Color(0xFFE0F2FE),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        textMuted = Color(0xFF64748B)
    )

    DeskThemeId.CYBER_MATRIX -> TerminalPalette(
        background = Color(0xFFF0F7FA),
        surface = Color(0xFFFFFFFF),
        surfaceElevated = Color(0xFFE9F3F8),
        surfaceHighlight = Color(0xFFDCEBF3),
        border = Color(0xFFC5DCE8),
        borderSubtle = Color(0xFFDDECF4),
        emeraldBuy = Color(0xFF059669),
        emeraldBuyDim = Color(0xFF047857),
        emeraldContainer = Color(0xFFCCFBF1),
        crimsonSell = Color(0xFFE11D48),
        crimsonSellDim = Color(0xFFBE123C),
        crimsonContainer = Color(0xFFFFE4E6),
        goldAccent = Color(0xFF0369A1),
        goldAccentBright = Color(0xFF0284C7),
        goldContainer = Color(0xFFE0F2FE),
        cyanInfo = Color(0xFF0891B2),
        cyanContainer = Color(0xFFCFFAFE),
        textPrimary = Color(0xFF0B192C),
        textSecondary = Color(0xFF3E556E),
        textMuted = Color(0xFF627A94)
    )

    DeskThemeId.ROYAL_PLATINUM -> TerminalPalette(
        background = Color(0xFFF3F5FB),
        surface = Color(0xFFFFFFFF),
        surfaceElevated = Color(0xFFEEF1F9),
        surfaceHighlight = Color(0xFFE1E7F4),
        border = Color(0xFFD0D9ED),
        borderSubtle = Color(0xFFE3E9F6),
        emeraldBuy = Color(0xFF059669),
        emeraldBuyDim = Color(0xFF047857),
        emeraldContainer = Color(0xFFD1FAE5),
        crimsonSell = Color(0xFFDC2626),
        crimsonSellDim = Color(0xFFB91C1C),
        crimsonContainer = Color(0xFFFEE2E2),
        goldAccent = Color(0xFFA16207),
        goldAccentBright = Color(0xFFCA8A04),
        goldContainer = Color(0xFFFEF9C3),
        cyanInfo = Color(0xFF1D4ED8),
        cyanContainer = Color(0xFFDBEAFE),
        textPrimary = Color(0xFF101B36),
        textSecondary = Color(0xFF475577),
        textMuted = Color(0xFF69799B)
    )

    DeskThemeId.STEALTH_CARBON -> TerminalPalette(
        background = Color(0xFFF5F6F8),
        surface = Color(0xFFFFFFFF),
        surfaceElevated = Color(0xFFF0F1F4),
        surfaceHighlight = Color(0xFFE5E7EC),
        border = Color(0xFFD5D8E0),
        borderSubtle = Color(0xFFE6E8EE),
        emeraldBuy = Color(0xFF0D9488),
        emeraldBuyDim = Color(0xFF0F766E),
        emeraldContainer = Color(0xFFCCFBF1),
        crimsonSell = Color(0xFFE11D48),
        crimsonSellDim = Color(0xFFBE123C),
        crimsonContainer = Color(0xFFFFE4E6),
        goldAccent = Color(0xFFD97706),
        goldAccentBright = Color(0xFFF59E0B),
        goldContainer = Color(0xFFFEF3C7),
        cyanInfo = Color(0xFF0284C7),
        cyanContainer = Color(0xFFE0F2FE),
        textPrimary = Color(0xFF111827),
        textSecondary = Color(0xFF4B5563),
        textMuted = Color(0xFF6B7280)
    )
}

val LocalTerminalPalette = staticCompositionLocalOf { paletteForTheme(DeskThemeId.OBSIDIAN_GOLD, false) }
