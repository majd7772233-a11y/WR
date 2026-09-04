package com.example.ui.theme

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color

@Keep
enum class GameThemeId {
    CYBER_NEON,     // Main Theme (Matching uploaded user picture)
    DEEP_SLATE,     // Current Theme
    ROYAL_EMERALD,  // Emerald & Gold
    CLASSIC_WOOD,   // Mahogany Wood & Brass
    OBSIDIAN_AMBER  // Dark Obsidian & Radiant Amber
}

data class GameTheme(
    val id: GameThemeId,
    val nameKey: String,
    val descriptionKey: String,
    val boardBackground: Color,
    val boardSurface: Color,
    val cellColor: Color,
    val cellAltColor: Color,
    val cellBorderColor: Color,
    val p1Primary: Color,
    val p1Dark: Color,
    val p1Glow: Color,
    val p1WallColor: Color,
    val p1WallBorder: Color,
    val p2Primary: Color,
    val p2Dark: Color,
    val p2Glow: Color,
    val p2WallColor: Color,
    val p2WallBorder: Color,
    val defaultWallColor: Color,
    val defaultWallBorder: Color,
    val isWallPlayerSpecific: Boolean = false,
    val showCoordinates: Boolean = true,
    val isGlowEnabled: Boolean = true
) {
    companion object {
        val MainCyberNeon = GameTheme(
            id = GameThemeId.CYBER_NEON,
            nameKey = "theme_cyber_neon",
            descriptionKey = "theme_cyber_neon_desc",
            boardBackground = Color(0xFF070B14),
            boardSurface = Color(0xFF0F172A),
            cellColor = Color(0xFF131D33),
            cellAltColor = Color(0xFF111A2E),
            cellBorderColor = Color(0xFF38BDF8).copy(alpha = 0.45f),
            p1Primary = Color(0xFF00E5FF),
            p1Dark = Color(0xFF0284C7),
            p1Glow = Color(0x7700E5FF),
            p1WallColor = Color(0xFF00E5FF),
            p1WallBorder = Color(0xFF38BDF8),
            p2Primary = Color(0xFFFF2A6D),
            p2Dark = Color(0xFFE11D48),
            p2Glow = Color(0x77FF2A6D),
            p2WallColor = Color(0xFFFF2A6D),
            p2WallBorder = Color(0xFFF43F5E),
            defaultWallColor = Color(0xFF00E5FF),
            defaultWallBorder = Color(0xFF38BDF8),
            isWallPlayerSpecific = true,
            showCoordinates = true,
            isGlowEnabled = true
        )

        val DeepSlate = GameTheme(
            id = GameThemeId.DEEP_SLATE,
            nameKey = "theme_deep_slate",
            descriptionKey = "theme_deep_slate_desc",
            boardBackground = Color(0xFF090D16),
            boardSurface = Color(0xFF131B2E),
            cellColor = Color(0xFF1E293B),
            cellAltColor = Color(0xFF192333),
            cellBorderColor = Color(0xFF334155).copy(alpha = 0.5f),
            p1Primary = Color(0xFF00D2FF),
            p1Dark = Color(0xFF0284C7),
            p1Glow = Color(0x6600D2FF),
            p1WallColor = Color(0xFFF59E0B),
            p1WallBorder = Color(0xFFD97706),
            p2Primary = Color(0xFFFF416C),
            p2Dark = Color(0xFFE11D48),
            p2Glow = Color(0x66FF416C),
            p2WallColor = Color(0xFFF59E0B),
            p2WallBorder = Color(0xFFD97706),
            defaultWallColor = Color(0xFFF59E0B),
            defaultWallBorder = Color(0xFFD97706),
            isWallPlayerSpecific = false,
            showCoordinates = true,
            isGlowEnabled = false
        )

        val RoyalEmerald = GameTheme(
            id = GameThemeId.ROYAL_EMERALD,
            nameKey = "theme_royal_emerald",
            descriptionKey = "theme_royal_emerald_desc",
            boardBackground = Color(0xFF031A14),
            boardSurface = Color(0xFF062C22),
            cellColor = Color(0xFF0B3C2D),
            cellAltColor = Color(0xFF083326),
            cellBorderColor = Color(0xFF10B981).copy(alpha = 0.35f),
            p1Primary = Color(0xFF10B981),
            p1Dark = Color(0xFF047857),
            p1Glow = Color(0x6610B981),
            p1WallColor = Color(0xFFFBBF24),
            p1WallBorder = Color(0xFFD97706),
            p2Primary = Color(0xFFF43F5E),
            p2Dark = Color(0xFFBE123C),
            p2Glow = Color(0x66F43F5E),
            p2WallColor = Color(0xFFFBBF24),
            p2WallBorder = Color(0xFFD97706),
            defaultWallColor = Color(0xFFFBBF24),
            defaultWallBorder = Color(0xFFD97706),
            isWallPlayerSpecific = false,
            showCoordinates = true,
            isGlowEnabled = true
        )

        val ClassicWood = GameTheme(
            id = GameThemeId.CLASSIC_WOOD,
            nameKey = "theme_classic_wood",
            descriptionKey = "theme_classic_wood_desc",
            boardBackground = Color(0xFF1A0F0A),
            boardSurface = Color(0xFF2A1810),
            cellColor = Color(0xFF3D2314),
            cellAltColor = Color(0xFF351E11),
            cellBorderColor = Color(0xFF5C3A21).copy(alpha = 0.6f),
            p1Primary = Color(0xFFFDE68A),
            p1Dark = Color(0xFFD97706),
            p1Glow = Color(0x66FDE68A),
            p1WallColor = Color(0xFFD97706),
            p1WallBorder = Color(0xFF78350F),
            p2Primary = Color(0xFFEA580C),
            p2Dark = Color(0xFF9A3412),
            p2Glow = Color(0x66EA580C),
            p2WallColor = Color(0xFFD97706),
            p2WallBorder = Color(0xFF78350F),
            defaultWallColor = Color(0xFFD97706),
            defaultWallBorder = Color(0xFF78350F),
            isWallPlayerSpecific = false,
            showCoordinates = true,
            isGlowEnabled = false
        )

        val ObsidianAmber = GameTheme(
            id = GameThemeId.OBSIDIAN_AMBER,
            nameKey = "theme_obsidian_amber",
            descriptionKey = "theme_obsidian_amber_desc",
            boardBackground = Color(0xFF09090B),
            boardSurface = Color(0xFF18181B),
            cellColor = Color(0xFF27272A),
            cellAltColor = Color(0xFF202023),
            cellBorderColor = Color(0xFF52525B).copy(alpha = 0.5f),
            p1Primary = Color(0xFFF59E0B),
            p1Dark = Color(0xFFB45309),
            p1Glow = Color(0x66F59E0B),
            p1WallColor = Color(0xFFF97316),
            p1WallBorder = Color(0xFFC2410C),
            p2Primary = Color(0xFFA855F7),
            p2Dark = Color(0xFF7E22CE),
            p2Glow = Color(0x66A855F7),
            p2WallColor = Color(0xFFF97316),
            p2WallBorder = Color(0xFFC2410C),
            defaultWallColor = Color(0xFFF97316),
            defaultWallBorder = Color(0xFFC2410C),
            isWallPlayerSpecific = false,
            showCoordinates = true,
            isGlowEnabled = true
        )

        val ALL_THEMES = listOf(MainCyberNeon, DeepSlate, RoyalEmerald, ClassicWood, ObsidianAmber)

        fun fromId(id: GameThemeId): GameTheme {
            return ALL_THEMES.firstOrNull { it.id == id } ?: MainCyberNeon
        }

        fun fromIdString(idStr: String): GameTheme {
            return try {
                fromId(GameThemeId.valueOf(idStr))
            } catch (e: Exception) {
                MainCyberNeon
            }
        }
    }
}
