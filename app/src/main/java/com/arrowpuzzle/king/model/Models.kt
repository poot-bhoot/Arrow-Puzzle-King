package com.arrowpuzzle.king.model

import androidx.compose.ui.graphics.Color

enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}

data class GridPoint(val x: Int, val y: Int)

data class ArrowEntity(
    val id: Int,
    /** Ordered points from tail (index 0) to head (last index) */
    val points: List<GridPoint>,
    val direction: Direction,
    val isEscaped: Boolean = false
) {
    val head: GridPoint get() = points.last()
}

data class PuzzleLevel(
    val levelNumber: Int,
    val title: String,
    val shapeName: String,
    val difficulty: String,
    val gridWidth: Int,
    val gridHeight: Int,
    val activeMask: Set<GridPoint>,
    val arrows: List<ArrowEntity>,
    val maxLives: Int = 3
)

enum class GameThemeType(
    val displayName: String,
    val subtitle: String,
    val bgTop: Color,
    val bgBottom: Color,
    val boardSurface: Color,
    val boardBorder: Color,
    val cardSurface: Color,
    val cardElevated: Color,
    val arrowNormal: Color,
    val arrowShadow: Color,
    val arrowHint: Color,
    val arrowBlocked: Color,
    val gridDot: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val coinGold: Color,
    val useWaterDropLives: Boolean,
    val isDark: Boolean
) {
    ROYAL_ALABASTER(
        displayName = "Royal Alabaster",
        subtitle = "Crisp Daylight Studio",
        bgTop = Color(0xFFF8FAFC),
        bgBottom = Color(0xFFE2E8F0),
        boardSurface = Color(0xFFFFFFFF),
        boardBorder = Color(0xFFCBD5E1),
        cardSurface = Color(0xFFFFFFFF),
        cardElevated = Color(0xFFF1F5F9),
        arrowNormal = Color(0xFF0F172A),
        arrowShadow = Color(0x220F172A),
        arrowHint = Color(0xFF0284C7),
        arrowBlocked = Color(0xFFE11D48),
        gridDot = Color(0xFFCBD5E1),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        accentPrimary = Color(0xFF2563EB),
        accentSecondary = Color(0xFF0EA5E9),
        coinGold = Color(0xFFF59E0B),
        useWaterDropLives = false,
        isDark = false
    ),
    PARCHMENT_ZEN(
        displayName = "Parchment Zen",
        subtitle = "Eye-Comfort Tactile Paper",
        bgTop = Color(0xFFF9F3E6),
        bgBottom = Color(0xFFEDE0C9),
        boardSurface = Color(0xFFFDF9F1),
        boardBorder = Color(0xFFD8C7A8),
        cardSurface = Color(0xFFF5ECDC),
        cardElevated = Color(0xFFEBE0CB),
        arrowNormal = Color(0xFF5C4033),
        arrowShadow = Color(0x245C4033),
        arrowHint = Color(0xFF0D9488),
        arrowBlocked = Color(0xFFDC2626),
        gridDot = Color(0xFFD6C5A9),
        textPrimary = Color(0xFF3E2723),
        textSecondary = Color(0xFF795548),
        accentPrimary = Color(0xFFB45309),
        accentSecondary = Color(0xFFD97706),
        coinGold = Color(0xFFD97706),
        useWaterDropLives = true,
        isDark = false
    ),
    OBSIDIAN_LUXE(
        displayName = "Obsidian Luxe",
        subtitle = "Midnight OLED Velvet",
        bgTop = Color(0xFF090D16),
        bgBottom = Color(0xFF111827),
        boardSurface = Color(0xFF131B2E),
        boardBorder = Color(0xFF1E293B),
        cardSurface = Color(0xFF1E293B),
        cardElevated = Color(0xFF27354F),
        arrowNormal = Color(0xFFF1F5F9),
        arrowShadow = Color(0x4038BDF8),
        arrowHint = Color(0xFF38BDF8),
        arrowBlocked = Color(0xFFFB7185),
        gridDot = Color(0xFF334155),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        accentPrimary = Color(0xFF38BDF8),
        accentSecondary = Color(0xFF818CF8),
        coinGold = Color(0xFFFBBF24),
        useWaterDropLives = false,
        isDark = true
    ),
    EMERALD_PALACE(
        displayName = "Emerald Palace",
        subtitle = "Deep Velvet & Gold",
        bgTop = Color(0xFF06201B),
        bgBottom = Color(0xFF0B312A),
        boardSurface = Color(0xFF0E3A32),
        boardBorder = Color(0xFF1B564B),
        cardSurface = Color(0xFF13483E),
        cardElevated = Color(0xFF1A5C50),
        arrowNormal = Color(0xFFFDE68A),
        arrowShadow = Color(0x35F59E0B),
        arrowHint = Color(0xFF34D399),
        arrowBlocked = Color(0xFFF87171),
        gridDot = Color(0xFF236B5E),
        textPrimary = Color(0xFFECFDF5),
        textSecondary = Color(0xFFA7F3D0),
        accentPrimary = Color(0xFFF59E0B),
        accentSecondary = Color(0xFF10B981),
        coinGold = Color(0xFFFBBF24),
        useWaterDropLives = true,
        isDark = true
    )
}

enum class ArrowSkin(
    val id: String,
    val title: String,
    val description: String,
    val price: Int,
    val overrideColor: Color?,
    val secondaryColor: Color?,
    val isNeonGlow: Boolean,
    val isHollowCore: Boolean
) {
    CLASSIC_INK(
        id = "classic_ink",
        title = "Classic Ink",
        description = "Clean studio finish adapted to your theme",
        price = 0,
        overrideColor = null,
        secondaryColor = null,
        isNeonGlow = false,
        isHollowCore = false
    ),
    ROYAL_GOLD(
        id = "royal_gold",
        title = "Imperial Gold",
        description = "24K metallic sheen with warm specular highlight",
        price = 120,
        overrideColor = Color(0xFFD97706),
        secondaryColor = Color(0xFFFEF08A),
        isNeonGlow = false,
        isHollowCore = false
    ),
    CYBER_NEON(
        id = "cyber_neon",
        title = "Cyber Pulse",
        description = "Electric cyan plasma core with outer bloom",
        price = 250,
        overrideColor = Color(0xFF06B6D4),
        secondaryColor = Color(0xFFECFEFF),
        isNeonGlow = true,
        isHollowCore = true
    ),
    CRIMSON_DRAGON(
        id = "crimson_dragon",
        title = "Crimson Katana",
        description = "Razor-sharp ruby blade with ember aura",
        price = 400,
        overrideColor = Color(0xFFE11D48),
        secondaryColor = Color(0xFFFDA4AF),
        isNeonGlow = true,
        isHollowCore = false
    ),
    AMETHYST_CROWN(
        id = "amethyst_crown",
        title = "Sovereign Violet",
        description = "Royal crystal core with luminous edge",
        price = 600,
        overrideColor = Color(0xFF7C3AED),
        secondaryColor = Color(0xFFDDD6FE),
        isNeonGlow = true,
        isHollowCore = true
    )
}

enum class EscapeTrail(
    val id: String,
    val title: String,
    val description: String,
    val price: Int,
    val primaryColor: Color,
    val secondaryColor: Color,
    val isRainbow: Boolean
) {
    STARDUST(
        id = "stardust",
        title = "Pure Stardust",
        description = "Subtle crystalline shimmer as arrows glide free",
        price = 0,
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFFE0F2FE),
        isRainbow = false
    ),
    SOLAR_FLARE(
        id = "solar_flare",
        title = "Golden Comet",
        description = "Rich molten gold sparks trailing the tail",
        price = 150,
        primaryColor = Color(0xFFF59E0B),
        secondaryColor = Color(0xFFFEF3C7),
        isRainbow = false
    ),
    EMERALD_AURORA(
        id = "emerald_aurora",
        title = "Aurora Silk",
        description = "Mystic teal & mint ribbon stream",
        price = 300,
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFFA7F3D0),
        isRainbow = false
    ),
    PRISM_RAINBOW(
        id = "prism_rainbow",
        title = "Prism Spectrum",
        description = "Full-spectrum iridescent ribbon & confetti burst",
        price = 500,
        primaryColor = Color(0xFFEC4899),
        secondaryColor = Color(0xFF8B5CF6),
        isRainbow = true
    )
}
