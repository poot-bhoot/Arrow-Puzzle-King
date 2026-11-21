package com.arrowpuzzle.king.engine

import com.arrowpuzzle.king.model.ArrowEntity
import com.arrowpuzzle.king.model.Direction
import com.arrowpuzzle.king.model.GridPoint
import com.arrowpuzzle.king.model.PuzzleLevel
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

object PuzzleEngine {

    /**
     * Checks if an arrow can slide completely out of the grid in its head's direction
     * without hitting any currently remaining arrow's body or head.
     */
    fun canArrowEscape(
        arrow: ArrowEntity,
        allArrows: List<ArrowEntity>,
        gridWidth: Int,
        gridHeight: Int
    ): Boolean {
        val occupied = HashSet<GridPoint>()
        for (other in allArrows) {
            if (other.id != arrow.id && !other.isEscaped) {
                occupied.addAll(other.points)
            }
        }
        var cx = arrow.head.x + arrow.direction.dx
        var cy = arrow.head.y + arrow.direction.dy
        while (cx in 0 until gridWidth && cy in 0 until gridHeight) {
            if (GridPoint(cx, cy) in occupied) {
                return false
            }
            cx += arrow.direction.dx
            cy += arrow.direction.dy
        }
        return true
    }

    /**
     * Finds how many grid steps an arrow can travel before colliding (if blocked).
     */
    fun getBlockedDistance(
        arrow: ArrowEntity,
        allArrows: List<ArrowEntity>,
        gridWidth: Int,
        gridHeight: Int
    ): Int {
        val occupied = HashSet<GridPoint>()
        for (other in allArrows) {
            if (other.id != arrow.id && !other.isEscaped) {
                occupied.addAll(other.points)
            }
        }
        var steps = 0
        var cx = arrow.head.x + arrow.direction.dx
        var cy = arrow.head.y + arrow.direction.dy
        while (cx in 0 until gridWidth && cy in 0 until gridHeight) {
            if (GridPoint(cx, cy) in occupied) {
                return steps
            }
            steps++
            cx += arrow.direction.dx
            cy += arrow.direction.dy
        }
        return steps
    }

    /**
     * Finds any currently free arrow to highlight for Hint.
     */
    fun findHintArrowId(
        arrows: List<ArrowEntity>,
        gridWidth: Int,
        gridHeight: Int
    ): Int? {
        val remaining = arrows.filter { !it.isEscaped }
        return remaining.firstOrNull { canArrowEscape(it, remaining, gridWidth, gridHeight) }?.id
    }

    /**
     * Generates a 100% mathematically solvable level for any levelNumber (1 to 10,000+).
     * Uses curated silhouette masks (Heart, Trophy, Anchor, Butterfly, Dog, Crown, Diamond, Square)
     * and builds tangled multi-turn arrows whose exit order is strictly verified.
     */
    fun generateLevel(levelNumber: Int): PuzzleLevel {
        val rng = Random(levelNumber * 9973L + 1337L)

        val (shapeName, difficulty, w, h, mask) = buildShapeSpecification(levelNumber)
        val arrows = generateSolvableArrows(w, h, mask, levelNumber, rng)

        val title = when {
            levelNumber == 1 -> "Tap Away Arrows"
            levelNumber % 10 == 0 -> "Master Shape: $shapeName"
            difficulty == "Hard" -> "Find the Free Path"
            else -> "Clear the Board"
        }

        val lives = when {
            levelNumber <= 5 -> 5
            difficulty == "Hard" -> 3
            else -> 4
        }

        return PuzzleLevel(
            levelNumber = levelNumber,
            title = title,
            shapeName = shapeName,
            difficulty = difficulty,
            gridWidth = w,
            gridHeight = h,
            activeMask = mask,
            arrows = arrows,
            maxLives = lives
        )
    }

    private data class ShapeSpec(
        val name: String,
        val difficulty: String,
        val w: Int,
        val h: Int,
        val mask: Set<GridPoint>
    )

    private fun buildShapeSpecification(level: Int): ShapeSpec {
        // Early levels start compact & approachable, scaling smoothly into rich shapes
        if (level == 1) return ShapeSpec("Classic Grid", "Normal", 6, 7, rectMask(6, 7))
        if (level == 2) return ShapeSpec("Classic Grid", "Normal", 7, 8, rectMask(7, 8))
        if (level == 3) return ShapeSpec("Diamond", "Normal", 9, 9, diamondMask(9, 9))

        val difficulty = when {
            level % 4 == 0 || level >= 25 -> "Hard"
            else -> "Normal"
        }

        val cycle = level % 8
        return when (cycle) {
            0 -> ShapeSpec("Trophy", difficulty, 13, 14, trophyMask())
            1 -> ShapeSpec("Classic Grid", difficulty, if (level < 15) 8 else 11, if (level < 15) 9 else 12, rectMask(if (level < 15) 8 else 11, if (level < 15) 9 else 12))
            2 -> ShapeSpec("Heart", difficulty, 13, 12, heartMask())
            3 -> ShapeSpec("Anchor", difficulty, 13, 14, anchorMask())
            4 -> ShapeSpec("Butterfly", difficulty, 14, 12, butterflyMask())
            5 -> ShapeSpec("Crown", difficulty, 13, 11, crownMask())
            6 -> ShapeSpec("Pup", difficulty, 14, 13, dogMask())
            else -> ShapeSpec("Leaf", difficulty, 12, 13, leafMask())
        }
    }

    /**
     * Generates tangled multi-bend arrows inside `mask` with a guaranteed valid escape sequence.
     * Algorithm:
     * 1. Maintain `placed` cells (representing arrows that will escape *later* than the new arrow).
     * 2. To add a new arrow that escapes *before* all currently `placed` arrows:
     *    - Pick an unoccupied cell in `mask` as the arrow's head, and a direction `dir` whose
     *      entire ray to the board edge does NOT intersect `placed`!
     *    - Grow the arrow's tail backwards into unoccupied cells of `mask`, avoiding its own ray
     *      (so the arrow never blocks itself!) and taking random 90-degree turns for rich tangle.
     * 3. Repeat until the shape is densely packed!
     */
    private fun generateSolvableArrows(
        w: Int,
        h: Int,
        mask: Set<GridPoint>,
        levelNumber: Int,
        rng: Random
    ): List<ArrowEntity> {
        val occupied = HashSet<GridPoint>()
        val result = mutableListOf<ArrowEntity>()
        val maxLen = when {
            levelNumber <= 3 -> 4
            levelNumber <= 10 -> 6
            levelNumber <= 30 -> 8
            else -> 11
        }
        val minLen = 2

        val candidates = mask.toMutableList()
        var arrowId = 1

        // Multiple passes to pack the grid tightly
        repeat(6) {
            candidates.shuffle(rng)
            for (headCandidate in candidates) {
                if (headCandidate in occupied) continue

                val validDirs = Direction.entries.shuffled(rng).filter { dir ->
                    isRayClearOfSet(headCandidate, dir, occupied, w, h)
                }
                if (validDirs.isEmpty()) continue

                val chosenDir = validDirs.first()
                // Forbidden cells for this arrow's tail: occupied cells + the ray in front of headCandidate
                val rayCells = getRayCells(headCandidate, chosenDir, w, h)

                val targetLen = rng.nextInt(minLen, maxLen + 1)
                val bodyFromHead = mutableListOf(headCandidate)
                val bodySet = hashSetOf(headCandidate)

                // First step backwards must go opposite to chosenDir so arrowhead points cleanly forward
                val firstBack = GridPoint(
                    headCandidate.x - chosenDir.dx,
                    headCandidate.y - chosenDir.dy
                )
                if (firstBack !in mask || firstBack in occupied || firstBack in rayCells) {
                    continue
                }
                bodyFromHead.add(firstBack)
                bodySet.add(firstBack)

                var current = firstBack
                var prevDir = chosenDir.opposite()

                while (bodyFromHead.size < targetLen) {
                    // Prefer turns (70%) over straight to create authentic maze-like bends
                    val dirs = Direction.entries.filter { d ->
                        val next = GridPoint(current.x + d.dx, current.y + d.dy)
                        next in mask && next !in occupied && next !in bodySet && next !in rayCells
                    }
                    if (dirs.isEmpty()) break

                    val nextDir = if (prevDir in dirs && rng.nextFloat() < 0.32f) {
                        prevDir
                    } else {
                        dirs[rng.nextInt(dirs.size)]
                    }
                    current = GridPoint(current.x + nextDir.dx, current.y + nextDir.dy)
                    bodyFromHead.add(current)
                    bodySet.add(current)
                    prevDir = nextDir
                }

                if (bodyFromHead.size >= minLen) {
                    // Reverse so index 0 is tail and last index is head
                    val orderedPoints = bodyFromHead.asReversed()
                    occupied.addAll(orderedPoints)
                    result.add(
                        ArrowEntity(
                            id = arrowId++,
                            points = orderedPoints,
                            direction = chosenDir
                        )
                    )
                }
            }
        }

        // Fill any leftover isolated 2-cell pairs if possible
        return result
    }

    private fun isRayClearOfSet(
        head: GridPoint,
        dir: Direction,
        blocked: Set<GridPoint>,
        w: Int,
        h: Int
    ): Boolean {
        var cx = head.x + dir.dx
        var cy = head.y + dir.dy
        while (cx in 0 until w && cy in 0 until h) {
            if (GridPoint(cx, cy) in blocked) return false
            cx += dir.dx
            cy += dir.dy
        }
        return true
    }

    private fun getRayCells(head: GridPoint, dir: Direction, w: Int, h: Int): Set<GridPoint> {
        val set = HashSet<GridPoint>()
        var cx = head.x + dir.dx
        var cy = head.y + dir.dy
        while (cx in 0 until w && cy in 0 until h) {
            set.add(GridPoint(cx, cy))
            cx += dir.dx
            cy += dir.dy
        }
        return set
    }

    // --- Curated Silhouette Masks matching user reference screenshots ---

    private fun rectMask(w: Int, h: Int): Set<GridPoint> {
        val set = HashSet<GridPoint>()
        for (y in 0 until h) for (x in 0 until w) set.add(GridPoint(x, y))
        return set
    }

    private fun diamondMask(w: Int, h: Int): Set<GridPoint> {
        val set = HashSet<GridPoint>()
        val cx = w / 2
        val cy = h / 2
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (abs(x - cx) + abs(y - cy) <= cx) set.add(GridPoint(x, y))
            }
        }
        return set
    }

    private fun heartMask(): Set<GridPoint> {
        val pattern = listOf(
            "..XXX...XXX..",
            ".XXXXX.XXXXX.",
            "XXXXXXXXXXXXX",
            "XXXXXXXXXXXXX",
            "XXXXXXXXXXXXX",
            ".XXXXXXXXXXX.",
            "..XXXXXXXXX..",
            "...XXXXXXX...",
            "....XXXXX....",
            ".....XXX.....",
            "......X......"
        )
        return parseAsciiMask(pattern)
    }

    private fun trophyMask(): Set<GridPoint> {
        val pattern = listOf(
            ".XXXXXXXXXXX.",
            "XXXXXXXXXXXXX",
            "XXXXXXXXXXXXX",
            "XXXXXXXXXXXXX",
            ".XXXXXXXXXXX.",
            "..XXXXXXXXX..",
            "...XXXXXXX...",
            ".....XXX.....",
            ".....XXX.....",
            "....XXXXX....",
            "...XXXXXXX...",
            "..XXXXXXXXX..",
            ".XXXXXXXXXXX."
        )
        return parseAsciiMask(pattern)
    }

    private fun anchorMask(): Set<GridPoint> {
        val pattern = listOf(
            ".....XXX.....",
            "....XX.XX....",
            ".....XXX.....",
            "......X......",
            "..XXXXXXXXX..",
            "......X......",
            "......X......",
            "X.....X.....X",
            "XX....X....XX",
            "XXX...X...XXX",
            ".XXXXXXXXXXX.",
            "..XXXXXXXXX..",
            "....XXXXX...."
        )
        return parseAsciiMask(pattern)
    }

    private fun butterflyMask(): Set<GridPoint> {
        val pattern = listOf(
            "XXXX.....XXXX",
            "XXXXX...XXXXX",
            "XXXXXX.XXXXXX",
            "XXXXXXXXXXXXX",
            "XXXXXXXXXXXXX",
            ".XXXXXXXXXXX.",
            "XXXXXXXXXXXXX",
            "XXXXXXXXXXXXX",
            "XXXXXX.XXXXXX",
            "XXXXX...XXXXX",
            "XXXX.....XXXX"
        )
        return parseAsciiMask(pattern)
    }

    private fun crownMask(): Set<GridPoint> {
        val pattern = listOf(
            "X.....X.....X",
            "XX...XXX...XX",
            "XXX..XXX..XXX",
            "XXXX.XXX.XXXX",
            "XXXXXXXXXXXXX",
            "XXXXXXXXXXXXX",
            "XXXXXXXXXXXXX",
            ".XXXXXXXXXXX.",
            ".XXXXXXXXXXX."
        )
        return parseAsciiMask(pattern)
    }

    private fun dogMask(): Set<GridPoint> {
        val pattern = listOf(
            "........XXXX.",
            ".......XXXXXX",
            ".......XXXXXX",
            ".......XXXX..",
            "X......XXXX..",
            "XX....XXXXX..",
            "XXXXXXXXXXX..",
            "XXXXXXXXXXX..",
            ".XXXXXXXXXX..",
            ".XXXXXXXXX...",
            ".XXX....XXX..",
            ".XXX....XXX.."
        )
        return parseAsciiMask(pattern)
    }

    private fun leafMask(): Set<GridPoint> {
        val pattern = listOf(
            "....XXXXX...",
            "..XXXXXXXX..",
            ".XXXXXXXXXX.",
            "XXXXXXXXXXXX",
            "XXXXXXXXXXXX",
            "XXXXXXXXXXXX",
            ".XXXXXXXXXX.",
            "..XXXXXXXX..",
            "...XXXXXX...",
            ".....XXXX...",
            ".......XX...",
            "........XX.."
        )
        return parseAsciiMask(pattern)
    }

    private fun parseAsciiMask(lines: List<String>): Set<GridPoint> {
        val set = HashSet<GridPoint>()
        lines.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                if (ch == 'X') set.add(GridPoint(x, y))
            }
        }
        return set
    }
}
