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

fun paletteForTheme(themeId: DeskThemeId): TerminalPalette = when (themeId) {
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

val LocalTerminalPalette = staticCompositionLocalOf { paletteForTheme(DeskThemeId.OBSIDIAN_GOLD) }
