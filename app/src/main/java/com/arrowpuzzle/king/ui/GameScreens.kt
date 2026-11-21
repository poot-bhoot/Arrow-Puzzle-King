package com.arrowpuzzle.king.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arrowpuzzle.king.audio.SoundAndHapticEngine
import com.arrowpuzzle.king.engine.PuzzleEngine
import com.arrowpuzzle.king.model.ArrowSkin
import com.arrowpuzzle.king.model.EscapeTrail
import com.arrowpuzzle.king.model.GameThemeType
import com.arrowpuzzle.king.model.PuzzleLevel

enum class ScreenRoute {
    HOME,
    LEVEL_MAP,
    STORE,
    GAMEPLAY,
    VICTORY
}

/** Tactile bouncy button modifier for AAA game feel */
fun Modifier.bouncyClick(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "btn_bounce"
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

@Composable
fun ArrowPuzzleKingApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("arrow_puzzle_king_v2", Context.MODE_PRIVATE) }
    val audioEngine = remember {
        SoundAndHapticEngine(context).apply {
            soundEnabled = prefs.getBoolean("sound_enabled", true)
            hapticEnabled = prefs.getBoolean("haptic_enabled", true)
        }
    }

    var highestUnlockedLevel by remember { mutableIntStateOf(prefs.getInt("highest_level", 1)) }
    var currentLevelNumber by remember { mutableIntStateOf(highestUnlockedLevel) }
    var coins by remember { mutableIntStateOf(prefs.getInt("coins_balance", 150)) }
    var hintsRemaining by remember { mutableIntStateOf(prefs.getInt("hints_count", 3)) }

    var currentTheme by remember {
        val savedName = prefs.getString("theme_name", GameThemeType.ROYAL_ALABASTER.name)
        mutableStateOf(GameThemeType.entries.find { it.name == savedName } ?: GameThemeType.ROYAL_ALABASTER)
    }

    var unlockedSkins by remember {
        val raw = prefs.getStringSet("unlocked_skins", setOf(ArrowSkin.CLASSIC_INK.id)) ?: setOf(ArrowSkin.CLASSIC_INK.id)
        mutableStateOf(raw + ArrowSkin.CLASSIC_INK.id)
    }
    var equippedSkin by remember {
        val saved = prefs.getString("equipped_skin", ArrowSkin.CLASSIC_INK.id)
        mutableStateOf(ArrowSkin.entries.find { it.id == saved } ?: ArrowSkin.CLASSIC_INK)
    }

    var unlockedTrails by remember {
        val raw = prefs.getStringSet("unlocked_trails", setOf(EscapeTrail.STARDUST.id)) ?: setOf(EscapeTrail.STARDUST.id)
        mutableStateOf(raw + EscapeTrail.STARDUST.id)
    }
    var equippedTrail by remember {
        val saved = prefs.getString("equipped_trail", EscapeTrail.STARDUST.id)
        mutableStateOf(EscapeTrail.entries.find { it.id == saved } ?: EscapeTrail.STARDUST)
    }

    var showGridDots by remember { mutableStateOf(prefs.getBoolean("show_grid", true)) }
    var soundEnabled by remember { mutableStateOf(audioEngine.soundEnabled) }
    var hapticEnabled by remember { mutableStateOf(audioEngine.hapticEnabled) }

    var currentScreen by remember { mutableStateOf(ScreenRoute.HOME) }
    var completedLevelSnapshot by remember { mutableStateOf<PuzzleLevel?>(null) }
    var lastEarnedCoins by remember { mutableIntStateOf(30) }

    fun saveProgress() {
        prefs.edit()
            .putInt("highest_level", highestUnlockedLevel)
            .putInt("coins_balance", coins)
            .putInt("hints_count", hintsRemaining)
            .putString("theme_name", currentTheme.name)
            .putStringSet("unlocked_skins", unlockedSkins)
            .putString("equipped_skin", equippedSkin.id)
            .putStringSet("unlocked_trails", unlockedTrails)
            .putString("equipped_trail", equippedTrail.id)
            .putBoolean("show_grid", showGridDots)
            .putBoolean("sound_enabled", soundEnabled)
            .putBoolean("haptic_enabled", hapticEnabled)
            .apply()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(currentTheme.bgTop, currentTheme.bgBottom)))
    ) {
        when (currentScreen) {
            ScreenRoute.HOME -> {
                HomeScreen(
                    highestLevel = highestUnlockedLevel,
                    coins = coins,
                    theme = currentTheme,
                    equippedSkin = equippedSkin,
                    onPlayClick = {
                        audioEngine.playTap()
                        currentLevelNumber = highestUnlockedLevel
                        currentScreen = ScreenRoute.GAMEPLAY
                    },
                    onLevelsClick = {
                        audioEngine.playTap()
                        currentScreen = ScreenRoute.LEVEL_MAP
                    },
                    onStoreClick = {
                        audioEngine.playTap()
                        currentScreen = ScreenRoute.STORE
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

            ScreenRoute.STORE -> {
                StoreScreen(
                    coins = coins,
                    theme = currentTheme,
                    unlockedSkins = unlockedSkins,
                    equippedSkin = equippedSkin,
                    unlockedTrails = unlockedTrails,
                    equippedTrail = equippedTrail,
                    hintsRemaining = hintsRemaining,
                    onBack = {
                        audioEngine.playTap()
                        currentScreen = ScreenRoute.HOME
                    },
                    onSelectOrBuySkin = { skin ->
                        if (skin.id in unlockedSkins) {
                            audioEngine.playTap()
                            equippedSkin = skin
                            saveProgress()
                        } else if (coins >= skin.price) {
                            audioEngine.playCoinPurchase()
                            coins -= skin.price
                            unlockedSkins = unlockedSkins + skin.id
                            equippedSkin = skin
                            saveProgress()
                        }
                    },
                    onSelectOrBuyTrail = { trail ->
                        if (trail.id in unlockedTrails) {
                            audioEngine.playTap()
                            equippedTrail = trail
                            saveProgress()
                        } else if (coins >= trail.price) {
                            audioEngine.playCoinPurchase()
                            coins -= trail.price
                            unlockedTrails = unlockedTrails + trail.id
                            equippedTrail = trail
                            saveProgress()
                        }
                    },
                    onBuyHintPack = {
                        if (coins >= 80) {
                            audioEngine.playCoinPurchase()
                            coins -= 80
                            hintsRemaining += 3
                            saveProgress()
                        }
                    }
                )
            }

            ScreenRoute.GAMEPLAY -> {
                GameplayScreen(
                    levelNumber = currentLevelNumber,
                    coins = coins,
                    theme = currentTheme,
                    equippedSkin = equippedSkin,
                    equippedTrail = equippedTrail,
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
                    onBuyHintInGame = {
                        if (coins >= 40) {
                            audioEngine.playCoinPurchase()
                            coins -= 40
                            hintsRemaining += 1
                            saveProgress()
                            true
                        } else {
                            false
                        }
                    },
                    onReviveWithCoins = {
                        if (coins >= 50) {
                            audioEngine.playCoinPurchase()
                            coins -= 50
                            saveProgress()
                            true
                        } else {
                            false
                        }
                    },
                    onLevelCompleted = { solvedLevel ->
                        audioEngine.playLevelCompleteFanfare()
                        completedLevelSnapshot = solvedLevel
                        val reward = if (solvedLevel.difficulty == "Hard") 45 else 30
                        lastEarnedCoins = reward
                        coins += reward
                        if (currentLevelNumber >= highestUnlockedLevel) {
                            highestUnlockedLevel = currentLevelNumber + 1
                        }
                        saveProgress()
                        currentScreen = ScreenRoute.VICTORY
                    },
                    onOpenStore = {
                        audioEngine.playTap()
                        currentScreen = ScreenRoute.STORE
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
                    earnedCoins = lastEarnedCoins,
                    totalCoins = coins,
                    equippedSkin = equippedSkin,
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
private fun CoinBadgePill(coins: Int, theme: GameThemeType, onClick: (() -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = theme.cardSurface,
        shadowElevation = 3.dp,
        modifier = Modifier
            .border(1.dp, theme.boardBorder, RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.bouncyClick(onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MonetizationOn,
                contentDescription = "Coins",
                tint = theme.coinGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$coins",
                color = theme.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun HomeScreen(
    highestLevel: Int,
    coins: Int,
    theme: GameThemeType,
    equippedSkin: ArrowSkin,
    onPlayClick: () -> Unit,
    onLevelsClick: () -> Unit,
    onStoreClick: () -> Unit,
    onCycleTheme: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar: Coins + Studio Theme Selector Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinBadgePill(coins = coins, theme = theme, onClick = onStoreClick)

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = theme.cardSurface,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .border(1.dp, theme.boardBorder, RoundedCornerShape(20.dp))
                    .bouncyClick { onCycleTheme() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
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
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Center Studio Emblem & Typography
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(106.dp)
                    .shadow(16.dp, RoundedCornerShape(30.dp))
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(theme.accentPrimary, theme.accentSecondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NorthEast,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(58.dp)
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "ARROW PUZZLE",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = theme.textPrimary,
                letterSpacing = 2.sp
            )
            Text(
                text = "KING",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = theme.accentPrimary,
                letterSpacing = 5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = theme.cardSurface.copy(alpha = 0.85f)
            ) {
                Text(
                    text = "Skin: ${equippedSkin.title} • ${theme.subtitle}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        // Bottom Action Stack: Play + Store + Level Map
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(12.dp, RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(theme.accentPrimary, theme.accentSecondary)
                        )
                    )
                    .bouncyClick { onPlayClick() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PLAY LEVEL $highestLevel",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Store Button
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = theme.cardSurface,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .border(1.dp, theme.boardBorder, RoundedCornerShape(24.dp))
                        .bouncyClick { onStoreClick() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = "Store",
                            tint = theme.coinGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STORE",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = theme.textPrimary
                        )
                    }
                }

                // Level Map Button
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = theme.cardSurface,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .border(1.dp, theme.boardBorder, RoundedCornerShape(24.dp))
                        .bouncyClick { onLevelsClick() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.GridView,
                            contentDescription = "Levels",
                            tint = theme.accentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LEVELS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = theme.textPrimary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StoreScreen(
    coins: Int,
    theme: GameThemeType,
    unlockedSkins: Set<String>,
    equippedSkin: ArrowSkin,
    unlockedTrails: Set<String>,
    equippedTrail: EscapeTrail,
    hintsRemaining: Int,
    onBack: () -> Unit,
    onSelectOrBuySkin: (ArrowSkin) -> Unit,
    onSelectOrBuyTrail: (EscapeTrail) -> Unit,
    onBuyHintPack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Arrow Skins, 1 = Escape Trails

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(18.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = theme.textPrimary
                    )
                }
                Text(
                    text = "Royal Armory",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = theme.textPrimary
                )
            }
            CoinBadgePill(coins = coins, theme = theme)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hint Refill Banner Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = theme.cardSurface,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.boardBorder, RoundedCornerShape(22.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(theme.accentPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = theme.accentPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "+3 Smart Hints",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = theme.textPrimary
                        )
                        Text(
                            text = "Owned: $hintsRemaining hints",
                            fontSize = 12.sp,
                            color = theme.textSecondary
                        )
                    }
                }

                Button(
                    onClick = onBuyHintPack,
                    enabled = coins >= 80,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentPrimary)
                ) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("80", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Category Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(theme.cardElevated)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Arrow Designs", "Escape Trails").forEachIndexed { idx, label ->
                val active = selectedTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) theme.accentPrimary else Color.Transparent)
                        .clickable { selectedTab = idx }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (active) Color.White else theme.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(ArrowSkin.entries) { skin ->
                    val isUnlocked = skin.id in unlockedSkins
                    val isEquipped = equippedSkin.id == skin.id
                    val previewColor = skin.overrideColor ?: theme.arrowNormal

                    StoreItemCard(
                        title = skin.title,
                        description = skin.description,
                        swatchPrimary = previewColor,
                        swatchSecondary = skin.secondaryColor ?: previewColor,
                        price = skin.price,
                        isUnlocked = isUnlocked,
                        isEquipped = isEquipped,
                        canAfford = coins >= skin.price,
                        theme = theme,
                        onAction = { onSelectOrBuySkin(skin) }
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(EscapeTrail.entries) { trail ->
                    val isUnlocked = trail.id in unlockedTrails
                    val isEquipped = equippedTrail.id == trail.id

                    StoreItemCard(
                        title = trail.title,
                        description = trail.description,
                        swatchPrimary = trail.primaryColor,
                        swatchSecondary = trail.secondaryColor,
                        price = trail.price,
                        isUnlocked = isUnlocked,
                        isEquipped = isEquipped,
                        canAfford = coins >= trail.price,
                        theme = theme,
                        onAction = { onSelectOrBuyTrail(trail) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreItemCard(
    title: String,
    description: String,
    swatchPrimary: Color,
    swatchSecondary: Color,
    price: Int,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    canAfford: Boolean,
    theme: GameThemeType,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = theme.cardSurface,
        shadowElevation = if (isEquipped) 6.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isEquipped) 2.dp else 1.dp,
                color = if (isEquipped) theme.accentPrimary else theme.boardBorder,
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(swatchPrimary, swatchSecondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = theme.textPrimary
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = theme.textSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            when {
                isEquipped -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = theme.accentPrimary.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "EQUIPPED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = theme.accentPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                isUnlocked -> {
                    OutlinedButton(
                        onClick = onAction,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Equip", fontWeight = FontWeight.Bold, color = theme.textPrimary)
                    }
                }
                else -> {
                    Button(
                        onClick = onAction,
                        enabled = canAfford,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.coinGold)
                    ) {
                        Icon(
                            Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$price", fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
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
                fontWeight = FontWeight.Black,
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
                        .shadow(if (unlocked) 3.dp else 0.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            when {
                                isCurrent -> theme.accentPrimary
                                unlocked -> theme.cardSurface
                                else -> theme.cardSurface.copy(alpha = 0.45f)
                            }
                        )
                        .border(1.dp, theme.boardBorder, RoundedCornerShape(18.dp))
                        .clickable(enabled = unlocked) { onSelectLevel(lvl) },
                    contentAlignment = Alignment.Center
                ) {
                    if (unlocked) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$lvl",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCurrent) Color.White else theme.textPrimary
                            )
                            if (lvl % 10 == 0) {
                                Text(
                                    text = "★",
                                    fontSize = 11.sp,
                                    color = if (isCurrent) Color.White else theme.coinGold
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
    coins: Int,
    theme: GameThemeType,
    equippedSkin: ArrowSkin,
    equippedTrail: EscapeTrail,
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
    onBuyHintInGame: () -> Boolean,
    onReviveWithCoins: () -> Boolean,
    onLevelCompleted: (PuzzleLevel) -> Unit,
    onOpenStore: () -> Unit,
    onBackHome: () -> Unit
) {
    var restartSeed by remember(levelNumber) { mutableIntStateOf(0) }
    val level = remember(levelNumber, restartSeed) { PuzzleEngine.generateLevel(levelNumber) }
    var arrows by remember(level) { mutableStateOf(level.arrows) }
    var lives by remember(level) { mutableIntStateOf(level.maxLives) }
    var comboStreak by remember(level) { mutableIntStateOf(0) }
    var hintArrowId by remember(level) { mutableStateOf<Int?>(null) }

    var showSettingsModal by remember { mutableStateOf(false) }
    var showOutOfLivesModal by remember { mutableStateOf(false) }

    val remainingArrowsCount = remember(arrows) { arrows.count { !it.isEscaped } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Row: Back | Level Badge | Coins & Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = theme.cardSurface,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(42.dp)
                    .border(1.dp, theme.boardBorder, CircleShape)
                    .bouncyClick { onBackHome() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = theme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LEVEL ${level.levelNumber}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = theme.textPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = level.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinBadgePill(coins = coins, theme = theme, onClick = onOpenStore)
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = theme.cardSurface,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .border(1.dp, theme.boardBorder, CircleShape)
                        .bouncyClick {
                            audioEngine.playTap()
                            showSettingsModal = true
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = theme.accentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // HUD Bar: [Remaining Arrows] | [Lives] | [Difficulty]
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = theme.cardSurface,
                modifier = Modifier.border(1.dp, theme.boardBorder, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = theme.accentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$remainingArrowsCount",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = theme.textPrimary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(level.maxLives) { idx ->
                    val active = idx < lives
                    Icon(
                        imageVector = if (theme.useWaterDropLives) Icons.Default.WaterDrop else Icons.Default.Favorite,
                        contentDescription = "Life",
                        tint = when {
                            !active -> theme.gridDot
                            theme.useWaterDropLives -> Color(0xFF0EA5E9)
                            else -> Color(0xFFF43F5E)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = theme.cardSurface,
                modifier = Modifier.border(1.dp, theme.boardBorder, RoundedCornerShape(14.dp))
            ) {
                Text(
                    text = level.difficulty,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (level.difficulty == "Hard") Color(0xFFF43F5E) else theme.textSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Framed Studio Puzzle Board Card
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = theme.boardSurface,
            shadowElevation = 6.dp,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.5.dp, theme.boardBorder, RoundedCornerShape(28.dp))
        ) {
            PuzzleBoardCanvas(
                level = level,
                arrows = arrows,
                theme = theme,
                equippedSkin = equippedSkin,
                equippedTrail = equippedTrail,
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
                        comboStreak++
                        audioEngine.playWhooshEscape(comboStreak)
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
                        comboStreak = 0
                        audioEngine.playBlockedCollision()
                        val newLives = (lives - 1).coerceAtLeast(0)
                        lives = newLives
                        if (newLives == 0) {
                            audioEngine.playGameOver()
                            showOutOfLivesModal = true
                        }
                        false
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Studio Dock: [Hint] [Grid #] [Store] [Theme]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Smart Hint with Badge
            Box(contentAlignment = Alignment.TopEnd) {
                DockActionButton(
                    icon = Icons.Default.Lightbulb,
                    label = "Hint",
                    tint = theme.accentPrimary,
                    isActive = hintArrowId != null,
                    theme = theme,
                    onClick = {
                        val freeId = PuzzleEngine.findHintArrowId(arrows, level.gridWidth, level.gridHeight)
                        if (freeId != null) {
                            if (hintArrowId == freeId || onConsumeHint()) {
                                audioEngine.playHintChime()
                                hintArrowId = freeId
                            } else if (onBuyHintInGame()) {
                                hintArrowId = freeId
                            }
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(theme.accentPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (hintsRemaining > 0) "$hintsRemaining" else "40",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            DockActionButton(
                icon = Icons.Default.Tag,
                label = "Grid",
                tint = theme.accentPrimary,
                isActive = showGridDots,
                theme = theme,
                onClick = onToggleGrid
            )

            DockActionButton(
                icon = Icons.Default.Storefront,
                label = "Store",
                tint = theme.coinGold,
                isActive = false,
                theme = theme,
                onClick = onOpenStore
            )

            DockActionButton(
                icon = Icons.Default.Palette,
                label = "Theme",
                tint = theme.accentSecondary,
                isActive = false,
                theme = theme,
                onClick = onCycleTheme
            )
        }
    }

    // Polished Glass-Card Settings Modal
    if (showSettingsModal) {
        StudioModalDialog(
            onDismiss = { showSettingsModal = false },
            theme = theme,
            icon = Icons.Default.Tune,
            iconBg = theme.accentPrimary,
            title = "Studio Settings",
            subtitle = "Customize audio, haptics & board"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StudioToggleRow(
                    title = "Sound Effects",
                    icon = Icons.Default.VolumeUp,
                    checked = soundEnabled,
                    theme = theme,
                    onToggle = onToggleSound
                )
                StudioToggleRow(
                    title = "Haptic Vibration",
                    icon = Icons.Default.Vibration,
                    checked = hapticEnabled,
                    theme = theme,
                    onToggle = onToggleHaptic
                )
                StudioToggleRow(
                    title = "Grid Guide Dots",
                    icon = Icons.Default.GridOn,
                    checked = showGridDots,
                    theme = theme,
                    onToggle = onToggleGrid
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        audioEngine.playTap()
                        showSettingsModal = false
                        restartSeed++
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.cardElevated)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = theme.textPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restart Level", fontWeight = FontWeight.Bold, color = theme.textPrimary)
                }

                Button(
                    onClick = {
                        audioEngine.playTap()
                        showSettingsModal = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentPrimary)
                ) {
                    Text("Resume Game", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }

    // Polished Animated Out-Of-Lives Modal
    if (showOutOfLivesModal) {
        StudioModalDialog(
            onDismiss = { },
            theme = theme,
            icon = Icons.Default.HeartBroken,
            iconBg = Color(0xFFF43F5E),
            title = "Out of Lives!",
            subtitle = "Watch out for blocked arrowheads"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (coins >= 50) {
                    Button(
                        onClick = {
                            if (onReviveWithCoins()) {
                                showOutOfLivesModal = false
                                lives = 2
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.coinGold)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Revive +2 Lives (50 Coins)",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        audioEngine.playTap()
                        showOutOfLivesModal = false
                        lives = level.maxLives
                        arrows = level.arrows
                        comboStreak = 0
                        hintArrowId = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentPrimary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Try Again", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 16.sp)
                }

                TextButton(
                    onClick = {
                        audioEngine.playTap()
                        showOutOfLivesModal = false
                        onBackHome()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Main Menu", fontWeight = FontWeight.Bold, color = theme.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun DockActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    isActive: Boolean,
    theme: GameThemeType,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.bouncyClick { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = theme.cardSurface,
            shadowElevation = 4.dp,
            modifier = Modifier
                .size(56.dp)
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) tint else theme.boardBorder,
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(25.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textSecondary
        )
    }
}

@Composable
private fun StudioModalDialog(
    onDismiss: () -> Unit,
    theme: GameThemeType,
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)) + scaleIn(
                initialScale = 0.85f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.9f)
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = theme.cardSurface,
                shadowElevation = 18.dp,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .border(1.5.dp, theme.boardBorder, RoundedCornerShape(30.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(10.dp, CircleShape)
                            .clip(CircleShape)
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = theme.textPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = theme.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun StudioToggleRow(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    theme: GameThemeType,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = theme.cardElevated,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = theme.accentPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = theme.textPrimary)
            }
            Switch(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun VictoryScreen(
    completedLevel: PuzzleLevel,
    earnedCoins: Int,
    totalCoins: Int,
    equippedSkin: ArrowSkin,
    onNextLevel: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E3A8A),
                        Color(0xFF0284C7)
                    )
                )
            )
    ) {
        ConfettiOverlay()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+$earnedCoins COINS  •  TOTAL $totalCoins",
                            color = Color(0xFFFEF08A),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "MASTERFUL!",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Level ${completedLevel.levelNumber} (${completedLevel.shapeName}) Cleared",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFBAE6FD)
                )
            }

            // Solved Shape Showcase Card
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = Color.White,
                shadowElevation = 18.dp,
                modifier = Modifier.size(235.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    PuzzleBoardCanvas(
                        level = completedLevel,
                        arrows = completedLevel.arrows,
                        theme = GameThemeType.ROYAL_ALABASTER,
                        equippedSkin = equippedSkin,
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .shadow(14.dp, RoundedCornerShape(31.dp))
                        .clip(RoundedCornerShape(31.dp))
                        .background(Color.White)
                        .bouncyClick { onNextLevel() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NEXT LEVEL",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E3A8A),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF1E3A8A))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(onClick = onMainMenu) {
                    Text(
                        text = "Main Menu",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
