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
    val background: Color,
    val cardSurface: Color,
    val arrowNormal: Color,
    val arrowHint: Color,
    val arrowBlocked: Color,
    val gridDot: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentPrimary: Color,
    val useWaterDropLives: Boolean
) {
    CLASSIC_CLEAN(
        displayName = "Classic Clean",
        background = Color(0xFFFFFFFF),
        cardSurface = Color(0xFFF3F6FA),
        arrowNormal = Color(0xFF14213D),
        arrowHint = Color(0xFF1D9BF0),
        arrowBlocked = Color(0xFFE63946),
        gridDot = Color(0xFFD0D7E2),
        textPrimary = Color(0xFF1E293B),
        textSecondary = Color(0xFF64748B),
        accentPrimary = Color(0xFF1D9BF0),
        useWaterDropLives = false
    ),
    EYE_COMFORT(
        displayName = "Eye Comfort",
        background = Color(0xFFF6F0E2),
        cardSurface = Color(0xFFEDE3CE),
        arrowNormal = Color(0xFF6B533B),
        arrowHint = Color(0xFF2B8CBE),
        arrowBlocked = Color(0xFFD93829),
        gridDot = Color(0xFFD5C8B0),
        textPrimary = Color(0xFF4A3826),
        textSecondary = Color(0xFF8C7358),
        accentPrimary = Color(0xFFB5853F),
        useWaterDropLives = true
    ),
    MIDNIGHT_OLED(
        displayName = "Midnight Dark",
        background = Color(0xFF0F172A),
        cardSurface = Color(0xFF1E293B),
        arrowNormal = Color(0xFFE2E8F0),
        arrowHint = Color(0xFF38BDF8),
        arrowBlocked = Color(0xFFF43F5E),
        gridDot = Color(0xFF334155),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        accentPrimary = Color(0xFF38BDF8),
        useWaterDropLives = false
    )
}
