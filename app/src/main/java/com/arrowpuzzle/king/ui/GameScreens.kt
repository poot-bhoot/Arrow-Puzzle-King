package com.arrowpuzzle.king.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowpuzzle.king.audio.SoundAndHapticEngine
import com.arrowpuzzle.king.engine.PuzzleEngine
import com.arrowpuzzle.king.model.ArrowEntity
import com.arrowpuzzle.king.model.GameThemeType
import com.arrowpuzzle.king.model.PuzzleLevel

enum class ScreenRoute {
    HOME,
    LEVEL_MAP,
    GAMEPLAY,
    VICTORY
}

@Composable
fun ArrowPuzzleKingApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("arrow_puzzle_king_prefs", Context.MODE_PRIVATE) }
    val audioEngine = remember {
        SoundAndHapticEngine(context).apply {
            soundEnabled = prefs.getBoolean("sound_enabled", true)
            hapticEnabled = prefs.getBoolean("haptic_enabled", true)
        }
    }

    var highestUnlockedLevel by remember { mutableIntStateOf(prefs.getInt("highest_level", 1)) }
    var currentLevelNumber by remember { mutableIntStateOf(highestUnlockedLevel) }
    var hintsRemaining by remember { mutableIntStateOf(prefs.getInt("hints_count", 3)) }
    var currentTheme by remember {
        val savedName = prefs.getString("theme_name", GameThemeType.CLASSIC_CLEAN.name)
        mutableStateOf(GameThemeType.entries.find { it.name == savedName } ?: GameThemeType.CLASSIC_CLEAN)
    }
    var showGridDots by remember { mutableStateOf(prefs.getBoolean("show_grid", true)) }
    var soundEnabled by remember { mutableStateOf(audioEngine.soundEnabled) }
    var hapticEnabled by remember { mutableStateOf(audioEngine.hapticEnabled) }

    var currentScreen by remember { mutableStateOf(ScreenRoute.HOME) }
    var completedLevelSnapshot by remember { mutableStateOf<PuzzleLevel?>(null) }

    fun saveProgress() {
        prefs.edit()
            .putInt("highest_level", highestUnlockedLevel)
            .putInt("hints_count", hintsRemaining)
            .putString("theme_name", currentTheme.name)
            .putBoolean("show_grid", showGridDots)
            .putBoolean("sound_enabled", soundEnabled)
            .putBoolean("haptic_enabled", hapticEnabled)
            .apply()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = currentTheme.background
    ) {
        when (currentScreen) {
            ScreenRoute.HOME -> {
                HomeScreen(
                    highestLevel = highestUnlockedLevel,
                    theme = currentTheme,
                    onPlayClick = {
                        audioEngine.playTap()
                        currentLevelNumber = highestUnlockedLevel
                        currentScreen = ScreenRoute.GAMEPLAY
                    },
                    onLevelsClick = {
                        audioEngine.playTap()
                        currentScreen = ScreenRoute.LEVEL_MAP
                    },
                    onCycleTheme = {
                        audioEngine.playTap()
                        val nextIdx = (currentTheme.ordinal + 1) % GameThemeType.entries.size
                        currentTheme = GameThemeType.entries[nextIdx]
                        saveProgress()
                    }
                )
            }

            ScreenRoute.LEVEL_MAP -> {
                LevelSelectScreen(
                    highestUnlockedLevel = highestUnlockedLevel,
                    theme = currentTheme,
                    onBack = {
                        audioEngine.playTap()
                        currentScreen = ScreenRoute.HOME
                    },
                    onSelectLevel = { lvl ->
                        audioEngine.playTap()
                        currentLevelNumber = lvl
                        currentScreen = ScreenRoute.GAMEPLAY
                    }
                )
            }

            ScreenRoute.GAMEPLAY -> {
                GameplayScreen(
                    levelNumber = currentLevelNumber,
                    theme = currentTheme,
                    showGridDots = showGridDots,
                    hintsRemaining = hintsRemaining,
                    soundEnabled = soundEnabled,
                    hapticEnabled = hapticEnabled,
                    audioEngine = audioEngine,
                    onToggleGrid = {
                        audioEngine.playTap()
                        showGridDots = !showGridDots
                        saveProgress()
                    },
                    onCycleTheme = {
                        audioEngine.playTap()
                        val nextIdx = (currentTheme.ordinal + 1) % GameThemeType.entries.size
                        currentTheme = GameThemeType.entries[nextIdx]
                        saveProgress()
                    },
                    onToggleSound = {
                        soundEnabled = !soundEnabled
                        audioEngine.soundEnabled = soundEnabled
                        if (soundEnabled) audioEngine.playTap()
                        saveProgress()
                    },
                    onToggleHaptic = {
                        hapticEnabled = !hapticEnabled
                        audioEngine.hapticEnabled = hapticEnabled
                        if (hapticEnabled) audioEngine.playTap()
                        saveProgress()
                    },
                    onConsumeHint = {
                        if (hintsRemaining > 0) {
                            hintsRemaining--
                            saveProgress()
                            true
                        } else {
                            false
                        }
                    },
                    onRefillHint = {
                        audioEngine.playHintChime()
                        hintsRemaining += 2
                        saveProgress()
                    },
                    onLevelCompleted = { solvedLevel ->
                        audioEngine.playLevelCompleteFanfare()
                        completedLevelSnapshot = solvedLevel
                        if (currentLevelNumber >= highestUnlockedLevel) {
                            highestUnlockedLevel = currentLevelNumber + 1
                        }
                        if (currentLevelNumber % 5 == 0) {
                            hintsRemaining++
                        }
                        saveProgress()
                        currentScreen = ScreenRoute.VICTORY
                    },
                    onBackHome = {
                        audioEngine.playTap()
                        currentScreen = ScreenRoute.HOME
                    }
                )
            }

            ScreenRoute.VICTORY -> {
                VictoryScreen(
                    completedLevel = completedLevelSnapshot ?: PuzzleEngine.generateLevel(currentLevelNumber),
                    theme = currentTheme,
                    onNextLevel = {
                        audioEngine.playTap()
                        currentLevelNumber++
                        currentScreen = ScreenRoute.GAMEPLAY
                    },
                    onMainMenu = {
                        audioEngine.playTap()
                        currentScreen = ScreenRoute.HOME
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    highestLevel: Int,
    theme: GameThemeType,
    onPlayClick: () -> Unit,
    onLevelsClick: () -> Unit,
    onCycleTheme: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Theme Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = theme.cardSurface,
                modifier = Modifier.clickable { onCycleTheme() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Theme",
                        tint = theme.accentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = theme.displayName,
                        color = theme.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Center Hero Title & Crown Emblem
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(theme.cardSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NorthEast,
                    contentDescription = null,
                    tint = theme.accentPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "ARROW PUZZLE",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = theme.textPrimary,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "KING",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = theme.accentPrimary,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Untangle the path • Clear the board",
                fontSize = 14.sp,
                color = theme.textSecondary
            )
        }

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onPlayClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accentPrimary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PLAY LEVEL $highestLevel",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onLevelsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp)
            ) {
                Icon(Icons.Default.GridView, contentDescription = null, tint = theme.textPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SELECT LEVEL (10,000+)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LevelSelectScreen(
    highestUnlockedLevel: Int,
    theme: GameThemeType,
    onBack: () -> Unit,
    onSelectLevel: (Int) -> Unit
) {
    val displayMax = maxOf(60, ((highestUnlockedLevel / 20) + 2) * 20)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = theme.textPrimary)
            }
            Text(
                text = "Select Level",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(displayMax) { index ->
                val lvl = index + 1
                val unlocked = lvl <= highestUnlockedLevel
                val isCurrent = lvl == highestUnlockedLevel

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when {
                                isCurrent -> theme.accentPrimary
                                unlocked -> theme.cardSurface
                                else -> theme.cardSurface.copy(alpha = 0.45f)
                            }
                        )
                        .clickable(enabled = unlocked) { onSelectLevel(lvl) },
                    contentAlignment = Alignment.Center
                ) {
                    if (unlocked) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$lvl",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color.White else theme.textPrimary
                            )
                            if (lvl % 10 == 0) {
                                Text(
                                    text = "★",
                                    fontSize = 10.sp,
                                    color = if (isCurrent) Color.White else theme.accentPrimary
                                )
                            }
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = theme.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameplayScreen(
    levelNumber: Int,
    theme: GameThemeType,
    showGridDots: Boolean,
    hintsRemaining: Int,
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    audioEngine: SoundAndHapticEngine,
    onToggleGrid: () -> Unit,
    onCycleTheme: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptic: () -> Unit,
    onConsumeHint: () -> Boolean,
    onRefillHint: () -> Unit,
    onLevelCompleted: (PuzzleLevel) -> Unit,
    onBackHome: () -> Unit
) {
    var restartSeed by remember(levelNumber) { mutableIntStateOf(0) }
    val level = remember(levelNumber, restartSeed) { PuzzleEngine.generateLevel(levelNumber) }
    var arrows by remember(level) { mutableStateOf(level.arrows) }
    var lives by remember(level) { mutableIntStateOf(level.maxLives) }
    var hintArrowId by remember(level) { mutableStateOf<Int?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showOutOfLivesDialog by remember { mutableStateOf(false) }

    val remainingArrowsCount = remember(arrows) { arrows.count { !it.isEscaped } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dynamic Title Header (matches screenshots: "Tap Away Arrows", "Clear the Board", "Find the Free Path")
        Text(
            text = level.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = theme.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )

        // Top Control Row: Back | Level X | Palette & Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBackHome) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = theme.accentPrimary
                )
            }

            Text(
                text = "Level ${level.levelNumber}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textPrimary
            )

            Row {
                IconButton(onClick = onCycleTheme) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Theme",
                        tint = theme.accentPrimary
                    )
                }
                IconButton(onClick = {
                    audioEngine.playTap()
                    showSettingsDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = theme.accentPrimary
                    )
                }
            }
        }

        // HUD Status Row: [Arrow Count Pill]  [Hearts / Water Drops]  [Difficulty Badge]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Remaining Arrows Counter
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = theme.cardSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = theme.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$remainingArrowsCount",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = theme.textPrimary
                    )
                }
            }

            // Lives Indicator (Red Hearts in Classic/Dark, Blue Water Drops in Eye Comfort!)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(level.maxLives) { idx ->
                    val active = idx < lives
                    if (theme.useWaterDropLives) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Life",
                            tint = if (active) Color(0xFF38BDF8) else theme.gridDot,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Life",
                            tint = if (active) Color(0xFFEF4444) else theme.gridDot,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Difficulty Badge ("Normal" / "Hard")
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = theme.cardSurface
            ) {
                Text(
                    text = level.difficulty,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (level.difficulty == "Hard") Color(0xFFE11D48) else theme.textSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Main Interactive Puzzle Board
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            PuzzleBoardCanvas(
                level = level,
                arrows = arrows,
                theme = theme,
                showGridDots = showGridDots,
                hintArrowId = hintArrowId,
                onArrowTapped = { tappedArrow ->
                    val free = PuzzleEngine.canArrowEscape(
                        arrow = tappedArrow,
                        allArrows = arrows,
                        gridWidth = level.gridWidth,
                        gridHeight = level.gridHeight
                    )
                    if (free) {
                        audioEngine.playWhooshEscape()
                        if (hintArrowId == tappedArrow.id) hintArrowId = null
                        val updated = arrows.map {
                            if (it.id == tappedArrow.id) it.copy(isEscaped = true) else it
                        }
                        arrows = updated
                        if (updated.all { it.isEscaped }) {
                            onLevelCompleted(level)
                        }
                        true
                    } else {
                        audioEngine.playBlockedCollision()
                        val newLives = (lives - 1).coerceAtLeast(0)
                        lives = newLives
                        if (newLives == 0) {
                            audioEngine.playGameOver()
                            showOutOfLivesDialog = true
                        }
                        false
                    }
                }
            )
        }

        // Bottom Controls: [Hint Bulb with Badge] & [Grid Toggle #]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hint Button
            Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                    shape = CircleShape,
                    color = theme.cardSurface,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(62.dp)
                        .border(
                            width = 2.dp,
                            color = if (hintArrowId != null) theme.arrowHint else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable {
                            val freeId = PuzzleEngine.findHintArrowId(arrows, level.gridWidth, level.gridHeight)
                            if (freeId != null) {
                                if (hintArrowId == freeId || onConsumeHint()) {
                                    audioEngine.playHintChime()
                                    hintArrowId = freeId
                                } else {
                                    // Free refill if out of hints
                                    onRefillHint()
                                }
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Hint",
                            tint = theme.accentPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Count Badge
                Box(
                    modifier = Modifier
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(theme.accentPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (hintsRemaining > 0) "$hintsRemaining" else "+",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(28.dp))

            // Grid Toggle (#) Button
            Surface(
                shape = CircleShape,
                color = theme.cardSurface,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(62.dp)
                    .border(
                        width = if (showGridDots) 2.dp else 0.dp,
                        color = if (showGridDots) theme.accentPrimary else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onToggleGrid() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = "Toggle Grid",
                        tint = theme.accentPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sound Effects")
                        Switch(checked = soundEnabled, onCheckedChange = { onToggleSound() })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vibration / Haptics")
                        Switch(checked = hapticEnabled, onCheckedChange = { onToggleHaptic() })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grid Guide Dots")
                        Switch(checked = showGridDots, onCheckedChange = { onToggleGrid() })
                    }
                    Button(
                        onClick = {
                            showSettingsDialog = false
                            restartSeed++
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restart Level")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Out of Lives Dialog
    if (showOutOfLivesDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Out of Lives!", fontWeight = FontWeight.Bold) },
            text = { Text("An arrow was blocked! Watch your path carefully and try again.") },
            confirmButton = {
                Button(onClick = {
                    audioEngine.playTap()
                    showOutOfLivesDialog = false
                    lives = level.maxLives
                    arrows = level.arrows
                    hintArrowId = null
                }) {
                    Text("Retry Level")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOutOfLivesDialog = false
                    onBackHome()
                }) {
                    Text("Home")
                }
            }
        )
    }
}

@Composable
private fun VictoryScreen(
    completedLevel: PuzzleLevel,
    theme: GameThemeType,
    onNextLevel: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0284C7),
                        Color(0xFF38BDF8),
                        Color(0xFF0EA5E9)
                    )
                )
            )
    ) {
        ConfettiOverlay()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Train Your Brain",
                    
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 40.sp
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Level ${completedLevel.levelNumber} Completed!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFEF08A)
                )
            }

            // Solved Shape Preview Card (matches Screenshot 1)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 10.dp,
                modifier = Modifier.size(230.dp)
            ) {
                Box(modifier = Modifier.padding(14.dp)) {
                    PuzzleBoardCanvas(
                        level = completedLevel,
                        arrows = completedLevel.arrows,
                        theme = GameThemeType.CLASSIC_CLEAN,
                        showGridDots = false,
                        hintArrowId = null,
                        onArrowTapped = { false }
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onNextLevel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(29.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text(
                        text = "Next Level",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(onClick = onMainMenu) {
                    Text(
                        text = "Main",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
