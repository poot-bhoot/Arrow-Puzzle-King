package com.arrowpuzzle.king.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.arrowpuzzle.king.model.ArrowEntity
import com.arrowpuzzle.king.model.ArrowSkin
import com.arrowpuzzle.king.model.Direction
import com.arrowpuzzle.king.model.EscapeTrail
import com.arrowpuzzle.king.model.GameThemeType
import com.arrowpuzzle.king.model.PuzzleLevel
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private data class ActiveEscapingArrow(
    val arrow: ArrowEntity,
    val progress: Animatable<Float, *>
)

private data class TrailSparkle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val color: Color,
    var life: Float // 1.0 -> 0.0
)

@Composable
fun PuzzleBoardCanvas(
    level: PuzzleLevel,
    arrows: List<ArrowEntity>,
    theme: GameThemeType,
    equippedSkin: ArrowSkin = ArrowSkin.CLASSIC_INK,
    equippedTrail: EscapeTrail = EscapeTrail.STARDUST,
    showGridDots: Boolean,
    hintArrowId: Int?,
    onArrowTapped: (ArrowEntity) -> Boolean,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // Smooth Zoom & Pan
    var scale by remember(level.levelNumber) { mutableFloatStateOf(1f) }
    var panOffset by remember(level.levelNumber) { mutableStateOf(Offset.Zero) }

    // Support MULTIPLE simultaneous escaping arrows sliding smoothly along their own paths!
    val escapingArrows = remember(level.levelNumber) { mutableStateListOf<ActiveEscapingArrow>() }
    val trailSparkles = remember(level.levelNumber) { mutableStateListOf<TrailSparkle>() }

    // Blocked arrow bump & shake state
    var blockedArrow by remember(level.levelNumber) { mutableStateOf<ArrowEntity?>(null) }
    val blockedShakeProgress = remember(level.levelNumber) { Animatable(0f) }

    // Subtle pulse for hinted arrow
    val infiniteTransition = rememberInfiniteTransition(label = "hint_pulse")
    val hintPulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hint_alpha"
    )

    var lastCellSize by remember { mutableFloatStateOf(40f) }
    var lastBoardOrigin by remember { mutableStateOf(Offset.Zero) }

    // Tick sparkle decay smoothly
    val frameTicker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_tick"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(level.levelNumber) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.85f, 2.6f)
                    panOffset = Offset(
                        x = (panOffset.x + pan.x).coerceIn(-380f, 380f),
                        y = (panOffset.y + pan.y).coerceIn(-380f, 380f)
                    )
                }
            }
            .pointerInput(level.levelNumber, arrows) {
                detectTapGestures { tapPos ->
                    val cell = lastCellSize
                    val origin = lastBoardOrigin
                    if (cell <= 0f) return@detectTapGestures

                    var bestArrow: ArrowEntity? = null
                    var bestDist = cell * 0.72f

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
                            val anim = Animatable(0f)
                            val entry = ActiveEscapingArrow(tapped, anim)
                            escapingArrows.add(entry)
                            scope.launch {
                                anim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(420, easing = FastOutSlowInEasing)
                                )
                                escapingArrows.remove(entry)
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
        // Read frameTicker so Canvas redraws smoothly while sparkles exist
        val unusedTick = frameTicker

        val padding = size.width * 0.07f
        val availW = size.width - padding * 2f
        val availH = size.height - padding * 2f

        val baseCell = min(availW / level.gridWidth, availH / level.gridHeight)
        val cellSize = baseCell * scale
        val boardW = cellSize * level.gridWidth
        val boardH = cellSize * level.gridHeight

        val originX = (size.width - boardW) / 2f + panOffset.x
        val originY = (size.height - boardH) / 2f + panOffset.y
        val origin = Offset(originX, originY)

        lastCellSize = cellSize
        lastBoardOrigin = origin

        // 1. Draw Refined Background Grid Dots
        if (showGridDots) {
            val dotRadius = (cellSize * 0.072f).coerceIn(2.2f, 5.5f)
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

        val strokeWidth = (cellSize * 0.155f).coerceIn(4f, 13f)
        val baseSkinColor = equippedSkin.overrideColor ?: theme.arrowNormal

        // 2. Draw Stationary & Blocked Arrows
        for (arrow in arrows) {
            if (arrow.isEscaped) continue

            val isBlocked = blockedArrow?.id == arrow.id
            val isHinted = hintArrowId == arrow.id

            val primaryColor = when {
                isBlocked -> theme.arrowBlocked
                isHinted -> theme.arrowHint
                else -> baseSkinColor
            }

            val bumpOffset = if (isBlocked) {
                val p = blockedShakeProgress.value
                val bump = sin(p * Math.PI).toFloat() * (cellSize * 0.25f)
                val shake = sin(p * Math.PI * 6.0).toFloat() * (cellSize * 0.11f) * (1f - p)
                Offset(
                    x = arrow.direction.dx * bump + (if (arrow.direction.dx == 0) shake else 0f),
                    y = arrow.direction.dy * bump + (if (arrow.direction.dy == 0) shake else 0f)
                )
            } else {
                Offset.Zero
            }

            val worldPts = arrow.points.map { pt ->
                Offset(
                    x = originX + (pt.x + 0.5f) * cellSize + bumpOffset.x,
                    y = originY + (pt.y + 0.5f) * cellSize + bumpOffset.y
                )
            }

            drawStyledArrowPolyline(
                polyline = worldPts,
                headAngleRad = directionToAngle(arrow.direction),
                cellSize = cellSize,
                strokeWidth = strokeWidth,
                primaryColor = primaryColor,
                secondaryColor = equippedSkin.secondaryColor,
                shadowColor = theme.arrowShadow,
                neonGlow = isHinted || equippedSkin.isNeonGlow,
                hollowCore = equippedSkin.isHollowCore,
                alpha = if (isHinted) hintPulse else 1f
            )
        }

        // 3. Draw Escaping Arrows Following Their Exact Path Track (Snake-Like Uncoiling!)
        // Every point of the arrow's tail travels strictly along the arrow's own bends and exits
        // out of the head's clear corridor — NEVER overlapping neighboring arrows!
        for (active in escapingArrows) {
            val arrow = active.arrow
            val p = active.progress.value

            val basePts = arrow.points.map { pt ->
                Offset(
                    x = originX + (pt.x + 0.5f) * cellSize,
                    y = originY + (pt.y + 0.5f) * cellSize
                )
            }
            if (basePts.size < 2) continue

            // Extend the path straight ahead from the head along arrow.direction until off-screen
            val exitDistance = maxOf(size.width, size.height) * 1.15f
            val headPt = basePts.last()
            val exitEndPt = Offset(
                x = headPt.x + arrow.direction.dx * exitDistance,
                y = headPt.y + arrow.direction.dy * exitDistance
            )
            val fullTrack = basePts + exitEndPt

            // Measure original arrow body length
            val bodyLen = computePolylineLength(basePts)
            // As p goes 0 -> 1, head advances along fullTrack by `travelDist`
            val travelDist = p * exitDistance
            val headDistOnTrack = bodyLen + travelDist
            val tailDistOnTrack = travelDist

            val slicedPts = extractPolylineSegment(fullTrack, tailDistOnTrack, headDistOnTrack)
            if (slicedPts.size >= 2) {
                val tailPt = slicedPts.first()
                val currentHeadPt = slicedPts.last()

                // Draw glowing kinetic ribbon trail behind the tail
                drawEscapeTrailRibbon(
                    track = fullTrack,
                    trailStartDist = (tailDistOnTrack - bodyLen * 0.85f).coerceAtLeast(0f),
                    trailEndDist = tailDistOnTrack,
                    strokeWidth = strokeWidth * 0.85f,
                    trail = equippedTrail,
                    fadeAlpha = (1f - p * 0.85f).coerceIn(0f, 1f)
                )

                // Spawn delicate sparkles at the tail tip
                if (p < 0.85f && Random.nextFloat() < 0.65f) {
                    val rainbowColors = listOf(
                        Color(0xFFF43F5E), Color(0xFFF59E0B),
                        Color(0xFF10B981), Color(0xFF38BDF8), Color(0xFFA855F7)
                    )
                    val sparkColor = if (equippedTrail.isRainbow) {
                        rainbowColors[Random.nextInt(rainbowColors.size)]
                    } else {
                        if (Random.nextBoolean()) equippedTrail.primaryColor else equippedTrail.secondaryColor
                    }
                    trailSparkles.add(
                        TrailSparkle(
                            x = tailPt.x + (Random.nextFloat() - 0.5f) * cellSize * 0.35f,
                            y = tailPt.y + (Random.nextFloat() - 0.5f) * cellSize * 0.35f,
                            vx = (Random.nextFloat() - 0.5f) * 2.4f,
                            vy = (Random.nextFloat() - 0.5f) * 2.4f,
                            radius = (cellSize * 0.08f).coerceIn(2.5f, 7f),
                            color = sparkColor,
                            life = 1.0f
                        )
                    )
                }

                // Compute current head tangent angle (follows corners smoothly!)
                val prevBeforeHead = slicedPts[slicedPts.size - 2]
                val angle = atan2(
                    (currentHeadPt.y - prevBeforeHead.y).toDouble(),
                    (currentHeadPt.x - prevBeforeHead.x).toDouble()
                ).toFloat()

                drawStyledArrowPolyline(
                    polyline = slicedPts,
                    headAngleRad = angle,
                    cellSize = cellSize,
                    strokeWidth = strokeWidth,
                    primaryColor = baseSkinColor,
                    secondaryColor = equippedSkin.secondaryColor,
                    shadowColor = theme.arrowShadow,
                    neonGlow = true,
                    hollowCore = equippedSkin.isHollowCore,
                    alpha = (1f - (p - 0.75f).coerceAtLeast(0f) * 4f).coerceIn(0f, 1f)
                )
            }
        }

        // 4. Render & Decay Trail Sparkles
        val iterator = trailSparkles.iterator()
        while (iterator.hasNext()) {
            val s = iterator.next()
            s.life -= 0.045f
            if (s.life <= 0f) {
                iterator.remove()
            } else {
                val drawX = s.x + s.vx * (1f - s.life) * 18f
                val drawY = s.y + s.vy * (1f - s.life) * 18f
                drawCircle(
                    color = s.color.copy(alpha = s.life.coerceIn(0f, 1f)),
                    radius = s.radius * s.life,
                    center = Offset(drawX, drawY)
                )
            }
        }
    }
}

private fun directionToAngle(dir: Direction): Float = when (dir) {
    Direction.RIGHT -> 0f
    Direction.DOWN -> (Math.PI / 2.0).toFloat()
    Direction.LEFT -> Math.PI.toFloat()
    Direction.UP -> (-Math.PI / 2.0).toFloat()
}

private fun computePolylineLength(pts: List<Offset>): Float {
    var total = 0f
    for (i in 0 until pts.size - 1) {
        total += hypot(pts[i + 1].x - pts[i].x, pts[i + 1].y - pts[i].y)
    }
    return total
}

/** Extracts a sub-polyline between `startDist` and `endDist` along `track` */
private fun extractPolylineSegment(
    track: List<Offset>,
    startDist: Float,
    endDist: Float
): List<Offset> {
    if (track.size < 2 || endDist <= startDist) return emptyList()
    val result = mutableListOf<Offset>()
    var accumulated = 0f

    for (i in 0 until track.size - 1) {
        val a = track[i]
        val b = track[i + 1]
        val segLen = hypot(b.x - a.x, b.y - a.y)
        if (segLen <= 0.0001f) continue

        val segStart = accumulated
        val segEnd = accumulated + segLen

        if (segEnd >= startDist && segStart <= endDist) {
            if (result.isEmpty()) {
                val t0 = ((startDist - segStart) / segLen).coerceIn(0f, 1f)
                result.add(Offset(a.x + (b.x - a.x) * t0, a.y + (b.y - a.y) * t0))
            }
            if (segEnd <= endDist) {
                result.add(b)
            } else {
                val t1 = ((endDist - segStart) / segLen).coerceIn(0f, 1f)
                result.add(Offset(a.x + (b.x - a.x) * t1, a.y + (b.y - a.y) * t1))
                break
            }
        }
        accumulated = segEnd
    }
    return result
}

private fun DrawScope.drawEscapeTrailRibbon(
    track: List<Offset>,
    trailStartDist: Float,
    trailEndDist: Float,
    strokeWidth: Float,
    trail: EscapeTrail,
    fadeAlpha: Float
) {
    val ribbonPts = extractPolylineSegment(track, trailStartDist, trailEndDist)
    if (ribbonPts.size < 2) return

    val path = Path()
    ribbonPts.forEachIndexed { idx, pt ->
        if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
    }

    val brush = if (trail.isRainbow) {
        Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFFA855F7).copy(alpha = 0.55f * fadeAlpha),
                Color(0xFF38BDF8).copy(alpha = 0.75f * fadeAlpha),
                Color(0xFF10B981).copy(alpha = 0.85f * fadeAlpha),
                Color(0xFFF59E0B).copy(alpha = 0.95f * fadeAlpha)
            ),
            start = ribbonPts.first(),
            end = ribbonPts.last()
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                trail.primaryColor.copy(alpha = 0.55f * fadeAlpha),
                trail.secondaryColor.copy(alpha = 0.90f * fadeAlpha)
            ),
            start = ribbonPts.first(),
            end = ribbonPts.last()
        )
    }

    drawPath(
        path = path,
        brush = brush,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawStyledArrowPolyline(
    polyline: List<Offset>,
    headAngleRad: Float,
    cellSize: Float,
    strokeWidth: Float,
    primaryColor: Color,
    secondaryColor: Color?,
    shadowColor: Color,
    neonGlow: Boolean,
    hollowCore: Boolean,
    alpha: Float
) {
    if (polyline.size < 2) return

    val path = Path()
    polyline.forEachIndexed { index, pt ->
        if (index == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
    }

    // 1. Subtle 3D Drop Shadow for tactile depth
    val shadowPath = Path()
    val shadowShift = cellSize * 0.045f
    polyline.forEachIndexed { index, pt ->
        if (index == 0) shadowPath.moveTo(pt.x, pt.y + shadowShift)
        else shadowPath.lineTo(pt.x, pt.y + shadowShift)
    }
    drawPath(
        path = shadowPath,
        color = shadowColor.copy(alpha = shadowColor.alpha * alpha),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // 2. Outer Neon / Hint Bloom
    if (neonGlow) {
        drawPath(
            path = path,
            color = primaryColor.copy(alpha = 0.28f * alpha),
            style = Stroke(width = strokeWidth * 2.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // 3. Main Arrow Body
    drawPath(
        path = path,
        color = primaryColor.copy(alpha = alpha),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // 4. Inner Specular / Hollow Core for Premium Skins
    if (secondaryColor != null) {
        drawPath(
            path = path,
            color = secondaryColor.copy(alpha = (if (hollowCore) 0.92f else 0.45f) * alpha),
            style = Stroke(
                width = strokeWidth * (if (hollowCore) 0.36f else 0.24f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    // 5. Rotated Arrowhead Triangle at the Tip
    val head = polyline.last()
    val tipReach = cellSize * 0.35f
    val backReach = cellSize * 0.14f
    val wingSpan = cellSize * 0.25f

    val cosA = cos(headAngleRad)
    val sinA = sin(headAngleRad)

    fun rotateLocal(lx: Float, ly: Float): Offset = Offset(
        x = head.x + lx * cosA - ly * sinA,
        y = head.y + lx * sinA + ly * cosA
    )

    val tip = rotateLocal(tipReach, 0f)
    val leftWing = rotateLocal(-backReach, -wingSpan)
    val rightWing = rotateLocal(-backReach, wingSpan)

    val headPath = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(leftWing.x, leftWing.y)
        lineTo(rightWing.x, rightWing.y)
        close()
    }

    if (neonGlow) {
        drawCircle(
            color = primaryColor.copy(alpha = 0.26f * alpha),
            radius = cellSize * 0.38f,
            center = head
        )
    }
    drawPath(path = headPath, color = primaryColor.copy(alpha = alpha))
}

@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val particles = remember {
        val colors = listOf(
            Color(0xFFFBBF24),
            Color(0xFF34D399),
            Color(0xFFFB7185),
            Color(0xFF38BDF8),
            Color(0xFFA78BFA),
            Color(0xFFFFFFFF)
        )
        List(65) {
            ConfettiParticle(
                xRatio = Random.nextFloat(),
                yStart = Random.nextFloat() * -0.6f,
                speed = 0.55f + Random.nextFloat() * 0.75f,
                wobble = Random.nextFloat() * 6.28f,
                size = 8f + Random.nextFloat() * 11f,
                color = colors[it % colors.size]
            )
        }
    }

    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, animationSpec = tween(2800))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val p = anim.value
        for (item in particles) {
            val y = ((item.yStart + p * item.speed * 1.65f) % 1.2f) * size.height
            val x = item.xRatio * size.width + sin(p * 12f + item.wobble) * 26f
            drawRect(
                color = item.color,
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(item.size, item.size * 0.58f)
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
