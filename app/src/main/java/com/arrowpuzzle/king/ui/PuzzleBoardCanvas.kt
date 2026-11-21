package com.arrowpuzzle.king.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.arrowpuzzle.king.model.ArrowEntity
import com.arrowpuzzle.king.model.Direction
import com.arrowpuzzle.king.model.GameThemeType
import com.arrowpuzzle.king.model.GridPoint
import com.arrowpuzzle.king.model.PuzzleLevel
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PuzzleBoardCanvas(
    level: PuzzleLevel,
    arrows: List<ArrowEntity>,
    theme: GameThemeType,
    showGridDots: Boolean,
    hintArrowId: Int?,
    onArrowTapped: (ArrowEntity) -> Boolean, // returns true if escaped, false if blocked
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // Zoom & pan state for larger levels
    var scale by remember(level.levelNumber) { mutableFloatStateOf(1f) }
    var panOffset by remember(level.levelNumber) { mutableStateOf(Offset.Zero) }

    // Animating arrow states
    var escapingArrow by remember(level.levelNumber) { mutableStateOf<ArrowEntity?>(null) }
    val escapeProgress = remember(level.levelNumber) { Animatable(0f) }

    var blockedArrow by remember(level.levelNumber) { mutableStateOf<ArrowEntity?>(null) }
    val blockedShakeProgress = remember(level.levelNumber) { Animatable(0f) }

    // Subtle pulse for hinted arrow
    val infiniteTransition = rememberInfiniteTransition(label = "hint_pulse")
    val hintPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hint_alpha"
    )

    // Store layout geometry for accurate tap hit-testing
    var lastCellSize by remember { mutableFloatStateOf(40f) }
    var lastBoardOrigin by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(level.levelNumber) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.85f, 2.6f)
                    panOffset = Offset(
                        x = (panOffset.x + pan.x).coerceIn(-350f, 350f),
                        y = (panOffset.y + pan.y).coerceIn(-350f, 350f)
                    )
                }
            }
            .pointerInput(level.levelNumber, arrows) {
                detectTapGestures { tapPos ->
                    if (escapingArrow != null) return@detectTapGestures
                    val cell = lastCellSize
                    val origin = lastBoardOrigin
                    if (cell <= 0f) return@detectTapGestures

                    // Find closest non-escaped arrow within tap tolerance
                    var bestArrow: ArrowEntity? = null
                    var bestDist = cell * 0.68f

                    for (arrow in arrows) {
                        if (arrow.isEscaped) continue
                        for (i in arrow.points.indices) {
                            val pt = arrow.points[i]
                            val px = origin.x + (pt.x + 0.5f) * cell
                            val py = origin.y + (pt.y + 0.5f) * cell
                            val d = hypot(tapPos.x - px, tapPos.y - py)
                            if (d < bestDist) {
                                bestDist = d
                                bestArrow = arrow
                            }
                            // Also check segment midpoint for easy tapping
                            if (i + 1 < arrow.points.size) {
                                val nextPt = arrow.points[i + 1]
                                val mx = origin.x + ((pt.x + nextPt.x) * 0.5f + 0.5f) * cell
                                val my = origin.y + ((pt.y + nextPt.y) * 0.5f + 0.5f) * cell
                                val md = hypot(tapPos.x - mx, tapPos.y - my)
                                if (md < bestDist) {
                                    bestDist = md
                                    bestArrow = arrow
                                }
                            }
                        }
                    }

                    bestArrow?.let { tapped ->
                        val escaped = onArrowTapped(tapped)
                        if (escaped) {
                            escapingArrow = tapped
                            scope.launch {
                                escapeProgress.snapTo(0f)
                                escapeProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(260, easing = FastOutSlowInEasing)
                                )
                                escapingArrow = null
                            }
                        } else {
                            blockedArrow = tapped
                            scope.launch {
                                blockedShakeProgress.snapTo(0f)
                                blockedShakeProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(360)
                                )
                                blockedArrow = null
                            }
                        }
                    }
                }
            }
    ) {
        val padding = size.width * 0.08f
        val availW = size.width - padding * 2f
        val availH = size.height - padding * 2f

        val baseCell = min(availW / level.gridWidth, availH / level.gridHeight)
        val cellSize = baseCell * scale
        val boardW = cellSize * level.gridWidth
        val boardH = cellSize * level.gridHeight

        val originX = (size.width - boardW) / 2f + panOffset.x
        val originY = (size.height - boardH) / 2f + panOffset.y

        lastCellSize = cellSize
        lastBoardOrigin = Offset(originX, originY)

        // 1. Draw Background Grid Dots inside active silhouette
        if (showGridDots) {
            val dotRadius = (cellSize * 0.075f).coerceIn(2.2f, 6f)
            for (pt in level.activeMask) {
                val cx = originX + (pt.x + 0.5f) * cellSize
                val cy = originY + (pt.y + 0.5f) * cellSize
                drawCircle(
                    color = theme.gridDot,
                    radius = dotRadius,
                    center = Offset(cx, cy)
                )
            }
        }

        // 2. Draw all active arrows
        val strokeWidth = (cellSize * 0.145f).coerceIn(3.5f, 12f)

        for (arrow in arrows) {
            if (arrow.isEscaped && escapingArrow?.id != arrow.id) continue

            val isEscaping = escapingArrow?.id == arrow.id
            val isBlocked = blockedArrow?.id == arrow.id
            val isHinted = hintArrowId == arrow.id

            val arrowColor = when {
                isBlocked -> theme.arrowBlocked
                isHinted -> theme.arrowHint.copy(alpha = hintPulse)
                isEscaping -> theme.arrowHint
                else -> theme.arrowNormal
            }

            // Compute animation shift
            val shiftOffset = when {
                isEscaping -> {
                    val travel = escapeProgress.value * (maxOf(size.width, size.height) * 0.85f)
                    Offset(
                        x = arrow.direction.dx * travel,
                        y = arrow.direction.dy * travel
                    )
                }
                isBlocked -> {
                    val p = blockedShakeProgress.value
                    // Bump forward slightly then shake sideways
                    val bump = sin(p * Math.PI).toFloat() * (cellSize * 0.28f)
                    val shake = sin(p * Math.PI * 6.0).toFloat() * (cellSize * 0.12f) * (1f - p)
                    Offset(
                        x = arrow.direction.dx * bump + (if (arrow.direction.dx == 0) shake else 0f),
                        y = arrow.direction.dy * bump + (if (arrow.direction.dy == 0) shake else 0f)
                    )
                }
                else -> Offset.Zero
            }

            drawArrowEntity(
                arrow = arrow,
                origin = Offset(originX, originY) + shiftOffset,
                cellSize = cellSize,
                strokeWidth = strokeWidth,
                color = arrowColor,
                glow = isHinted
            )
        }
    }
}

private fun DrawScope.drawArrowEntity(
    arrow: ArrowEntity,
    origin: Offset,
    cellSize: Float,
    strokeWidth: Float,
    color: Color,
    glow: Boolean
) {
    if (arrow.points.isEmpty()) return

    val path = Path()
    arrow.points.forEachIndexed { index, pt ->
        val cx = origin.x + (pt.x + 0.5f) * cellSize
        val cy = origin.y + (pt.y + 0.5f) * cellSize
        if (index == 0) path.moveTo(cx, cy) else path.lineTo(cx, cy)
    }

    if (glow) {
        drawPath(
            path = path,
            color = color.copy(alpha = 0.25f),
            style = Stroke(
                width = strokeWidth * 2.1f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Draw sharp equilateral-style triangle arrowhead at the head cell
    val head = arrow.head
    val hx = origin.x + (head.x + 0.5f) * cellSize
    val hy = origin.y + (head.y + 0.5f) * cellSize

    val tipReach = cellSize * 0.34f
    val backReach = cellSize * 0.14f
    val wingSpan = cellSize * 0.25f

    val headPath = Path()
    when (arrow.direction) {
        Direction.UP -> {
            headPath.moveTo(hx, hy - tipReach)
            headPath.lineTo(hx - wingSpan, hy + backReach)
            headPath.lineTo(hx + wingSpan, hy + backReach)
        }
        Direction.DOWN -> {
            headPath.moveTo(hx, hy + tipReach)
            headPath.lineTo(hx - wingSpan, hy - backReach)
            headPath.lineTo(hx + wingSpan, hy - backReach)
        }
        Direction.LEFT -> {
            headPath.moveTo(hx - tipReach, hy)
            headPath.lineTo(hx + backReach, hy - wingSpan)
            headPath.lineTo(hx + backReach, hy + wingSpan)
        }
        Direction.RIGHT -> {
            headPath.moveTo(hx + tipReach, hy)
            headPath.lineTo(hx - backReach, hy - wingSpan)
            headPath.lineTo(hx - backReach, hy + wingSpan)
        }
    }
    headPath.close()
    drawPath(path = headPath, color = color)
}

@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val particles = remember {
        val colors = listOf(
            Color(0xFFFFD166),
            Color(0xFF06D6A0),
            Color(0xFFEF476F),
            Color(0xFF38BDF8),
            Color(0xFFA78BFA),
            Color(0xFFFFFFFF)
        )
        List(55) {
            ConfettiParticle(
                xRatio = Random.nextFloat(),
                yStart = Random.nextFloat() * -0.6f,
                speed = 0.55f + Random.nextFloat() * 0.75f,
                wobble = Random.nextFloat() * 6.28f,
                size = 8f + Random.nextFloat() * 10f,
                color = colors[it % colors.size]
            )
        }
    }

    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, animationSpec = tween(2600))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val p = anim.value
        for (item in particles) {
            val y = ((item.yStart + p * item.speed * 1.6f) % 1.2f) * size.height
            val x = item.xRatio * size.width + sin(p * 12f + item.wobble) * 24f
            drawRect(
                color = item.color,
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(item.size, item.size * 0.6f)
            )
        }
    }
}

private data class ConfettiParticle(
    val xRatio: Float,
    val yStart: Float,
    val speed: Float,
    val wobble: Float,
    val size: Float,
    val color: Color
)
