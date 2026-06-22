package com.xanticious.androidgames.controller.games.bubblespop

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.bubblespop.ActivePowerUp
import com.xanticious.androidgames.model.games.bubblespop.BubbleColor
import com.xanticious.androidgames.model.games.bubblespop.BubblePowerUp
import com.xanticious.androidgames.model.games.bubblespop.BubbleType
import com.xanticious.androidgames.model.games.bubblespop.BubblesGridEvent
import com.xanticious.androidgames.model.games.bubblespop.BubblesGridState
import com.xanticious.androidgames.model.games.bubblespop.BubblesPopConfig
import com.xanticious.androidgames.model.games.bubblespop.BubblesSnakeEvent
import com.xanticious.androidgames.model.games.bubblespop.BubblesSnakeState
import com.xanticious.androidgames.model.games.bubblespop.BubblesVariant
import com.xanticious.androidgames.model.games.bubblespop.ChainBubble
import com.xanticious.androidgames.model.games.bubblespop.FlyingBubble
import com.xanticious.androidgames.model.games.bubblespop.GridCell
import com.xanticious.androidgames.model.games.bubblespop.SnakeFlyingBubble
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Pure Bubbles Pop rules shared by TURN_BASED, ARCADE, and SNAKE_ARCADE variants.
 * No Android or Compose imports — fully JVM-testable.
 */
class BubblesPopController {

    // ─── Constants ───────────────────────────────────────────────────────────

    companion object {
        /** Number of bubble columns on even rows. */
        const val GRID_COLS = 9
        /** Normalized bubble radius: fits GRID_COLS bubbles exactly across width [0, 1]. */
        val BUBBLE_RADIUS = 1f / (GRID_COLS * 2f)
        /**
         * Center-to-center vertical distance in hex packing for a square board.
         * Multiply by the board aspect (width / height) via [rowHeight] to keep
         * offset rows visually touching on non-square boards.
         */
        val ROW_HEIGHT = BUBBLE_RADIUS * sqrt(3f)
        const val CANNON_X = 0.5f
        const val CANNON_Y = 0.93f
        const val DANGER_LINE_Y = 0.75f
        const val BUBBLE_SPEED = 1.8f
        const val COMBO_WINDOW_SECONDS = 2f
        /** Rows of the cluster visible at the start; extra rows hide above the ceiling. */
        const val VISIBLE_ROWS = 4
        /** Turn-based: seconds the connected bubble lingers before it and its cluster pop. */
        const val POP_ANIM_SECONDS = 0.22f
        /** Chance a generated row is sparse (only 1–3 bubbles) instead of nearly full. */
        const val SPARSE_ROW_CHANCE = 0.18f
        /** Max rows a turn-based level can start with. */
        const val MAX_TURN_BASED_ROWS = 10

        // Snake track waypoints in normalized [0, 1] space: enters at the top and
        // snakes downward toward the exit near the bottom.
        val TRACK_WAYPOINTS: List<Vec2> = listOf(
            Vec2(0.12f, 0.10f),
            Vec2(0.88f, 0.10f),
            Vec2(0.88f, 0.30f),
            Vec2(0.12f, 0.30f),
            Vec2(0.12f, 0.50f),
            Vec2(0.88f, 0.50f),
            Vec2(0.88f, 0.70f),
            Vec2(0.12f, 0.70f),
        )

        /** Bottom launcher Y in normalized space (snake variant). */
        const val LAUNCHER_Y = 0.92f

        /** Center-to-center spacing of chain bubbles along the track. */
        val CHAIN_BUBBLE_SPACING = BUBBLE_RADIUS * 2.1f

        private val ALL_COLORS = BubbleColor.values()
    }

    /** Center-to-center vertical row distance for the given board [aspect] (w / h). */
    fun rowHeight(aspect: Float = 1f): Float = ROW_HEIGHT * aspect

    /** Vertical bubble radius in height-fraction units for the given [aspect]. */
    private fun vRadius(aspect: Float = 1f): Float = BUBBLE_RADIUS * aspect

    // ─── Config factory ──────────────────────────────────────────────────────

    fun configFor(difficulty: GameDifficulty, variant: BubblesVariant): BubblesPopConfig =
        when (difficulty) {
            GameDifficulty.EASY -> BubblesPopConfig(
                variant = variant,
                colorsInPlay = 5,
                startingRows = if (variant == BubblesVariant.SNAKE_ARCADE) 0 else 5,
                missesPerDescend = 3,
                descentSpeed = 0.009f,
                cannonCooldown = cooldownFor(variant),
                initialLives = livesFor(variant),
                chainLength = 40,
                initialChainSpeed = 0.04f,
                chainSpeedIncrement = 0.005f,
                backfirePenalty = 2,
                powerUpFrequency = 0.08f,
                specialBubbleLevel = 3,
            )
            GameDifficulty.MEDIUM -> BubblesPopConfig(
                variant = variant,
                colorsInPlay = 6,
                startingRows = if (variant == BubblesVariant.SNAKE_ARCADE) 0 else 6,
                missesPerDescend = 3,
                descentSpeed = 0.014f,
                cannonCooldown = cooldownFor(variant),
                initialLives = livesFor(variant),
                chainLength = 50,
                initialChainSpeed = 0.06f,
                chainSpeedIncrement = 0.008f,
                backfirePenalty = 2,
                powerUpFrequency = 0.06f,
                specialBubbleLevel = 2,
            )
            GameDifficulty.HARD -> BubblesPopConfig(
                variant = variant,
                colorsInPlay = 7,
                startingRows = if (variant == BubblesVariant.SNAKE_ARCADE) 0 else 7,
                missesPerDescend = 3,
                descentSpeed = 0.02f,
                cannonCooldown = cooldownFor(variant),
                initialLives = livesFor(variant),
                chainLength = 60,
                initialChainSpeed = 0.09f,
                chainSpeedIncrement = 0.01f,
                backfirePenalty = 3,
                powerUpFrequency = 0.04f,
                specialBubbleLevel = 2,
            )
        }

    private fun cooldownFor(variant: BubblesVariant) = when (variant) {
        BubblesVariant.TURN_BASED -> 0f
        BubblesVariant.ARCADE -> 0.4f
        BubblesVariant.SNAKE_ARCADE -> 0.3f
    }

    private fun livesFor(variant: BubblesVariant) =
        if (variant == BubblesVariant.TURN_BASED) 1 else 3

    // ─── Hex grid geometry ───────────────────────────────────────────────────

    /**
     * Normalized center position of grid cell (col, row) with the cluster shifted by [topOffset].
     * Even rows have GRID_COLS columns; odd rows have GRID_COLS-1 columns, offset by half a diameter.
     */
    fun cellPosition(col: Int, row: Int, topOffset: Float, aspect: Float = 1f): Vec2 {
        val x = if (row % 2 == 0) {
            (col + 0.5f) / GRID_COLS
        } else {
            (col + 1.0f) / GRID_COLS
        }
        val y = topOffset + row * rowHeight(aspect) + vRadius(aspect)
        return Vec2(x, y)
    }

    /** Maximum number of columns on the given row. */
    fun maxCols(row: Int) = if (row % 2 == 0) GRID_COLS else GRID_COLS - 1

    /** Six hex neighbors of (col, row) in offset-coordinate layout. */
    fun hexNeighbors(col: Int, row: Int): List<Pair<Int, Int>> =
        if (row % 2 == 0) {
            listOf(
                col - 1 to row, col + 1 to row,
                col - 1 to row - 1, col to row - 1,
                col - 1 to row + 1, col to row + 1,
            )
        } else {
            listOf(
                col - 1 to row, col + 1 to row,
                col to row - 1, col + 1 to row - 1,
                col to row + 1, col + 1 to row + 1,
            )
        }

    /**
     * BFS: find all cells same-color-connected to [start].
     * RAINBOW bubbles match any color; STONE bubbles never match.
     */
    fun findCluster(grid: Map<Pair<Int, Int>, GridCell>, start: Pair<Int, Int>): Set<Pair<Int, Int>> {
        val startCell = grid[start] ?: return emptySet()
        if (startCell.type == BubbleType.STONE) return setOf(start)
        val targetColor = startCell.color
        val visited = mutableSetOf(start)
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(start)
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            for (nb in hexNeighbors(cur.first, cur.second)) {
                if (nb in visited) continue
                val cell = grid[nb] ?: continue
                if (cell.type == BubbleType.STONE) continue
                val matches = cell.color == targetColor ||
                        cell.type == BubbleType.RAINBOW ||
                        startCell.type == BubbleType.RAINBOW
                if (matches) { visited.add(nb); queue.add(nb) }
            }
        }
        return visited
    }

    /**
     * Returns all cells not reachable from row 0 (disconnected from the ceiling).
     * These "fall" and are removed after a pop.
     */
    fun findDisconnected(grid: Map<Pair<Int, Int>, GridCell>): Set<Pair<Int, Int>> {
        val connected = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        grid.keys.filter { it.second == 0 }.forEach { connected.add(it); queue.add(it) }
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            for (nb in hexNeighbors(cur.first, cur.second)) {
                if (nb in connected || nb !in grid) continue
                connected.add(nb); queue.add(nb)
            }
        }
        return grid.keys.toSet() - connected
    }

    /** Nearest valid (col, row) hex coordinate to the continuous position (x, y). */
    fun nearestGridCoord(x: Float, y: Float, topOffset: Float, aspect: Float = 1f): Pair<Int, Int> {
        val rowF = (y - topOffset - vRadius(aspect)) / rowHeight(aspect)
        val row = rowF.roundToInt().coerceAtLeast(0)
        val colF = if (row % 2 == 0) x * GRID_COLS - 0.5f else x * GRID_COLS - 1f
        val col = colF.roundToInt().coerceIn(0, maxCols(row) - 1)
        return col to row
    }

    /** Nearest empty hex cell to (x, y), checking the closest candidate then its neighbors. */
    fun nearestEmptyCell(
        x: Float,
        y: Float,
        grid: Map<Pair<Int, Int>, GridCell>,
        topOffset: Float,
        aspect: Float = 1f,
    ): Pair<Int, Int>? {
        val candidate = nearestGridCoord(x, y, topOffset, aspect)
        if (candidate !in grid && candidate.first in 0 until maxCols(candidate.second)) return candidate
        val pos = Vec2(x, y)
        fun aspectDist(c: Int, r: Int): Float {
            val cp = cellPosition(c, r, topOffset, aspect)
            val dy = (cp.y - pos.y) / aspect
            return hypot(cp.x - pos.x, dy)
        }
        val firstRing = hexNeighbors(candidate.first, candidate.second)
            .filter { (c, r) -> r >= 0 && c in 0 until maxCols(r) && (c to r) !in grid }
        val secondRing by lazy {
            hexNeighbors(candidate.first, candidate.second)
                .flatMap { hexNeighbors(it.first, it.second) }
                .filter { (c, r) -> r >= 0 && c in 0 until maxCols(r) && (c to r) !in grid }
        }
        return (firstRing.ifEmpty { secondRing })
            .minByOrNull { (c, r) -> aspectDist(c, r) }
    }

    // ─── Grid generation ─────────────────────────────────────────────────────

    /** Rows the cluster starts with for the given variant and level. */
    fun rowsForLevel(config: BubblesPopConfig, level: Int): Int = when (config.variant) {
        BubblesVariant.TURN_BASED -> level.coerceIn(1, MAX_TURN_BASED_ROWS)
        else -> config.startingRows
    }

    fun generateGrid(
        config: BubblesPopConfig,
        level: Int,
        random: Random,
    ): Map<Pair<Int, Int>, GridCell> {
        val rows = rowsForLevel(config, level)
        val cells = mutableMapOf<Pair<Int, Int>, GridCell>()
        for (row in 0 until rows) {
            generateRow(config, level, row, random, cells)
        }
        return cells
    }

    /** Fill a single row into [cells]; rows are not always full (see requirement 6). */
    private fun generateRow(
        config: BubblesPopConfig,
        level: Int,
        row: Int,
        random: Random,
        cells: MutableMap<Pair<Int, Int>, GridCell>,
    ) {
        val colors = ALL_COLORS.take(config.colorsInPlay.coerceIn(1, ALL_COLORS.size))
        val includeSpecials = level >= config.specialBubbleLevel
        val maxC = maxCols(row)
        val count = if (random.nextFloat() < SPARSE_ROW_CHANCE) {
            // Sparse row: only 1, 2 or 3 bubbles.
            (1 + random.nextInt(3)).coerceAtMost(maxC)
        } else {
            // Nearly full: 0, 1 or 2 bubbles missing.
            (maxC - random.nextInt(3)).coerceAtLeast(1)
        }
        val chosenCols = (0 until maxC).shuffled(random).take(count)
        for (col in chosenCols) {
            val color = colors[random.nextInt(colors.size)]
            val type = when {
                includeSpecials && random.nextFloat() < config.powerUpFrequency -> BubbleType.POWER_UP
                includeSpecials && random.nextFloat() < 0.05f -> BubbleType.BOMB
                includeSpecials && random.nextFloat() < 0.08f -> BubbleType.STONE
                else -> BubbleType.NORMAL
            }
            val powerUp = if (type == BubbleType.POWER_UP) randomGridPowerUp(random) else null
            cells[col to row] = GridCell(col, row, color, type, powerUp)
        }
    }

    /** Initial top offset that keeps at most [VISIBLE_ROWS] rows on screen at start. */
    private fun initialTopOffset(rows: Int, aspect: Float): Float {
        val hidden = (rows - VISIBLE_ROWS).coerceAtLeast(0)
        return -hidden * rowHeight(aspect)
    }

    private fun randomGridPowerUp(random: Random): BubblePowerUp {
        val options = listOf(
            BubblePowerUp.LIGHTNING, BubblePowerUp.COLOR_BOMB,
            BubblePowerUp.SLOW_TIME, BubblePowerUp.WILD_SHOT, BubblePowerUp.SHIELD,
        )
        return options[random.nextInt(options.size)]
    }

    /** Pick a random bubble for the cannon queue. */
    fun randomBubble(config: BubblesPopConfig, random: Random): Pair<BubbleColor, BubbleType> {
        val colors = ALL_COLORS.take(config.colorsInPlay.coerceIn(1, ALL_COLORS.size))
        val color = colors[random.nextInt(colors.size)]
        val type = if (random.nextFloat() < 0.03f) BubbleType.RAINBOW else BubbleType.NORMAL
        return color to type
    }

    // ─── Grid-game initialization ─────────────────────────────────────────────

    fun initialGridState(
        config: BubblesPopConfig,
        random: Random = Random.Default,
        aspect: Float = 1f,
    ): BubblesGridState {
        val grid = generateGrid(config, 1, random)
        val (cannon, cannonType) = randomBubble(config, random)
        val (next, nextType) = randomBubble(config, random)
        return BubblesGridState(
            grid = grid,
            cannonBubble = cannon,
            cannonBubbleType = cannonType,
            nextBubble = next,
            nextBubbleType = nextType,
            flying = null,
            score = 0,
            level = 1,
            lives = config.initialLives,
            missStreak = 0,
            cannonCooldown = 0f,
            topOffset = initialTopOffset(rowsForLevel(config, 1), aspect),
            activePowerUps = emptyList(),
            comboMultiplier = 1f,
            comboTimer = 0f,
            wildShotActive = false,
            shieldActive = false,
            bestScore = 0,
            aspect = aspect,
        )
    }

    // ─── Grid shooting ────────────────────────────────────────────────────────

    /**
     * Place a flying bubble in the cannon pointed at [aimAngle] radians from vertical
     * (0 = straight up, positive = right). Does nothing if cooldown > 0 or already flying.
     *
     * The vertical velocity is scaled by the board aspect so the trajectory follows the
     * on-screen aim direction rather than biasing toward straight up.
     */
    fun fireCannon(state: BubblesGridState, aimAngle: Float): BubblesGridState {
        if (state.flying != null || state.cannonCooldown > 0f) return state
        val effType = if (state.wildShotActive) BubbleType.RAINBOW else state.cannonBubbleType
        return state.copy(
            flying = FlyingBubble(
                x = CANNON_X, y = CANNON_Y,
                dx = sin(aimAngle) * BUBBLE_SPEED,
                dy = -cos(aimAngle) * BUBBLE_SPEED * state.aspect,
                color = state.cannonBubble,
                type = effType,
            ),
            wildShotActive = false,
        )
    }

    // ─── Grid step ────────────────────────────────────────────────────────────

    /**
     * Advance the grid game by [dt] seconds.
     * Returns the updated state and the dominant event that occurred this frame.
     */
    fun stepGrid(
        state: BubblesGridState,
        config: BubblesPopConfig,
        dt: Float,
        random: Random = Random.Default,
    ): Pair<BubblesGridState, BubblesGridEvent> {
        var s = state
        val aspect = s.aspect

        // Decay timers
        if (s.cannonCooldown > 0f)
            s = s.copy(cannonCooldown = (s.cannonCooldown - dt).coerceAtLeast(0f))
        if (s.comboTimer > 0f) {
            val newTimer = (s.comboTimer - dt).coerceAtLeast(0f)
            s = s.copy(comboTimer = newTimer, comboMultiplier = if (newTimer == 0f) 1f else s.comboMultiplier)
        }
        s = s.copy(
            activePowerUps = s.activePowerUps
                .map { it.copy(remainingSeconds = it.remainingSeconds - dt) }
                .filter { it.remainingSeconds > 0f },
        )

        // Destruction animation: the connected bubble lingers, then pops with its cluster.
        if (s.popTimer > 0f) {
            val newTimer = s.popTimer - dt
            return if (newTimer <= 0f) applyPop(s, config, random)
            else Pair(s.copy(popTimer = newTimer), BubblesGridEvent.None)
        }

        // Arcade: continuous cluster descent + endless rows generated above.
        if (config.variant == BubblesVariant.ARCADE && s.flying == null) {
            s = s.copy(topOffset = s.topOffset + config.descentSpeed * dt)
            s = maybeInsertTopRow(s, config, random)
        }

        // Danger-line check
        val dangerResult = checkDangerLine(s, config, random)
        if (dangerResult != null) return dangerResult

        // Advance flying bubble
        val flying = s.flying ?: return Pair(s, BubblesGridEvent.None)

        var fx = flying.x + flying.dx * dt
        var fy = flying.y + flying.dy * dt
        var fdx = flying.dx
        var fdy = flying.dy

        // Side-wall bounces
        if (fx < BUBBLE_RADIUS) { fx = BUBBLE_RADIUS * 2f - fx; fdx = -fdx }
        if (fx > 1f - BUBBLE_RADIUS) { fx = (1f - BUBBLE_RADIUS) * 2f - fx; fdx = -fdx }

        // Collision with grid (aspect-corrected so the boundary matches the drawn bubbles)
        val hitCell = s.grid.values.firstOrNull { cell ->
            val cp = cellPosition(cell.col, cell.row, s.topOffset, aspect)
            hypot(fx - cp.x, (fy - cp.y) / aspect) < BUBBLE_RADIUS * 2f
        }
        val reachedCeiling = fy < vRadius(aspect)

        if (hitCell != null || reachedCeiling) {
            return attachAndResolve(s, config, fx, fy, flying, random)
        }

        return Pair(s.copy(flying = flying.copy(x = fx, y = fy, dx = fdx, dy = fdy)), BubblesGridEvent.None)
    }

    /** Arcade: once the cluster has descended a full row, push a fresh row in at the top. */
    private fun maybeInsertTopRow(
        s: BubblesGridState,
        config: BubblesPopConfig,
        random: Random,
    ): BubblesGridState {
        val rh = rowHeight(s.aspect)
        if (s.topOffset < rh) return s
        val shifted = s.grid.values.associate { c ->
            (c.col to c.row + 1) to c.copy(row = c.row + 1)
        }
        val newRow = mutableMapOf<Pair<Int, Int>, GridCell>()
        generateRow(config, s.level, 0, random, newRow)
        return s.copy(grid = shifted + newRow, topOffset = s.topOffset - rh)
    }

    private fun checkDangerLine(
        s: BubblesGridState,
        config: BubblesPopConfig,
        random: Random,
    ): Pair<BubblesGridState, BubblesGridEvent>? {
        val rh = rowHeight(s.aspect)
        val maxY = s.grid.values.maxOfOrNull { c -> s.topOffset + c.row * rh + vRadius(s.aspect) * 2f }
            ?: return null
        if (maxY < DANGER_LINE_Y) return null
        return if (s.shieldActive) {
            Pair(s.copy(shieldActive = false, topOffset = s.topOffset - rh), BubblesGridEvent.None)
        } else if (s.lives > 1) {
            val newGrid = generateGrid(config, s.level, random)
            val (c, ct) = randomBubble(config, random)
            val (n, nt) = randomBubble(config, random)
            Pair(
                s.copy(
                    lives = s.lives - 1,
                    topOffset = initialTopOffset(rowsForLevel(config, s.level), s.aspect),
                    grid = newGrid,
                    flying = null, cannonBubble = c, cannonBubbleType = ct, nextBubble = n, nextBubbleType = nt,
                ),
                BubblesGridEvent.LifeLost,
            )
        } else {
            Pair(s.copy(lives = 0), BubblesGridEvent.ClusterCrossedDangerLine)
        }
    }

    private fun attachAndResolve(
        s: BubblesGridState,
        config: BubblesPopConfig,
        fx: Float,
        fy: Float,
        flying: FlyingBubble,
        random: Random,
    ): Pair<BubblesGridState, BubblesGridEvent> {
        val attachPos = nearestEmptyCell(fx, fy, s.grid, s.topOffset, s.aspect)
            ?: (0 to 0)
        val (ac, ar) = attachPos
        val newCell = GridCell(ac, ar, flying.color, flying.type)
        val workGrid = s.grid + (attachPos to newCell)

        val cluster = findCluster(workGrid, attachPos)

        val (nc, nct) = randomBubble(config, random)

        return if (cluster.size >= 3 || flying.type == BubbleType.BOMB) {
            resolveMatch(s, config, workGrid, attachPos, cluster, flying, random)
        } else {
            resolveMiss(s, config, workGrid, nc, nct, random)
        }
    }

    private fun resolveMatch(
        s: BubblesGridState,
        config: BubblesPopConfig,
        workGrid: Map<Pair<Int, Int>, GridCell>,
        attachPos: Pair<Int, Int>,
        cluster: Set<Pair<Int, Int>>,
        flying: FlyingBubble,
        random: Random,
    ): Pair<BubblesGridState, BubblesGridEvent> {
        val (ac, ar) = attachPos
        // Expand cluster for BOMB type
        val explodeSet = if (flying.type == BubbleType.BOMB || workGrid[attachPos]?.type == BubbleType.BOMB) {
            cluster + hexNeighbors(ac, ar).filter { it in workGrid }
        } else cluster
        // Chain-detonate any BOMB in the explode set
        val extraBombs = explodeSet.flatMap { pos ->
            if (workGrid[pos]?.type == BubbleType.BOMB)
                hexNeighbors(pos.first, pos.second).filter { it in workGrid }
            else emptyList()
        }
        val finalPop = (explodeSet + extraBombs).toSet()

        val pending = s.copy(grid = workGrid, flying = null, popping = finalPop)

        // Turn-based: linger on the connected bubble before destroying the cluster.
        return if (config.variant == BubblesVariant.TURN_BASED) {
            Pair(pending.copy(popTimer = POP_ANIM_SECONDS), BubblesGridEvent.None)
        } else {
            applyPop(pending, config, random)
        }
    }

    /**
     * Remove [BubblesGridState.popping] from the grid, drop disconnected bubbles, award
     * score, advance the cannon, and resolve level / screen-clear consequences.
     */
    private fun applyPop(
        s: BubblesGridState,
        config: BubblesPopConfig,
        random: Random,
    ): Pair<BubblesGridState, BubblesGridEvent> {
        val popping = s.popping
        var resultGrid = s.grid - popping
        var score = calculatePopScore(popping.size)

        val fallen = findDisconnected(resultGrid)
        resultGrid -= fallen
        score += fallen.size * 20

        val multiplied = (score * s.comboMultiplier).toInt()
        val newCombo = minOf(s.comboMultiplier + 0.5f, 4f)
        val newScore = s.score + multiplied
        val (nc, nct) = randomBubble(config, random)

        val base = s.copy(
            flying = null, popping = emptySet(), popTimer = 0f,
            missStreak = 0,
            cannonBubble = s.nextBubble, cannonBubbleType = s.nextBubbleType,
            nextBubble = nc, nextBubbleType = nct,
            cannonCooldown = config.cannonCooldown,
            comboMultiplier = newCombo, comboTimer = COMBO_WINDOW_SECONDS,
        )

        if (resultGrid.isEmpty()) {
            return if (config.variant == BubblesVariant.TURN_BASED) {
                val levelBonus = 300 * s.level
                Pair(base.copy(grid = resultGrid, score = newScore + levelBonus), BubblesGridEvent.ClusterEmpty)
            } else {
                // Arcade: never ends on a clear — refill and keep the run going.
                val refill = generateGrid(config, s.level, random)
                Pair(
                    base.copy(
                        grid = refill, score = newScore,
                        topOffset = initialTopOffset(rowsForLevel(config, s.level), s.aspect),
                    ),
                    BubblesGridEvent.BubblePopped(popping.size, multiplied),
                )
            }
        }

        // Requirement 4: clearing every bubble on screen descends one row to reveal more.
        val anyVisible = resultGrid.values.any { c ->
            cellPosition(c.col, c.row, s.topOffset, s.aspect).y >= 0f
        }
        val revealedOffset = if (!anyVisible) s.topOffset + rowHeight(s.aspect) else s.topOffset

        return Pair(
            base.copy(grid = resultGrid, score = newScore, topOffset = revealedOffset),
            BubblesGridEvent.BubblePopped(popping.size, multiplied),
        )
    }

    private fun resolveMiss(
        s: BubblesGridState,
        config: BubblesPopConfig,
        workGrid: Map<Pair<Int, Int>, GridCell>,
        nc: BubbleColor,
        nct: BubbleType,
        random: Random,
    ): Pair<BubblesGridState, BubblesGridEvent> {
        // Requirement 3: only descend after missesPerDescend (3) misses in a row,
        // then reset the streak. A match resets the streak elsewhere.
        val reachedDescend = s.missStreak + 1 >= config.missesPerDescend
        val descend = config.variant == BubblesVariant.TURN_BASED && reachedDescend
        val newMiss = if (descend) 0 else s.missStreak + 1
        val newOffset = if (descend) s.topOffset + rowHeight(s.aspect) else s.topOffset
        val ns = s.copy(
            grid = workGrid, flying = null,
            missStreak = newMiss, topOffset = newOffset,
            cannonBubble = s.nextBubble, cannonBubbleType = s.nextBubbleType,
            nextBubble = nc, nextBubbleType = nct,
            cannonCooldown = config.cannonCooldown,
            comboMultiplier = 1f, comboTimer = 0f,
        )
        // Re-check danger after potential descent
        val dangerResult = checkDangerLine(ns, config, random)
        return dangerResult ?: Pair(ns, BubblesGridEvent.None)
    }

    fun calculatePopScore(count: Int): Int = when {
        count <= 0 -> 0
        count == 3 -> 50
        count == 4 -> 100
        count >= 5 -> 150 + (count - 5) * 30
        else -> 0
    }

    // ─── Grid: advance to next level ─────────────────────────────────────────

    fun nextLevelGrid(
        state: BubblesGridState,
        config: BubblesPopConfig,
        random: Random = Random.Default,
    ): BubblesGridState {
        val newLevel = state.level + 1
        val newColors = minOf(config.colorsInPlay + 1, ALL_COLORS.size)
        val newConfig = config.copy(colorsInPlay = newColors)
        val newGrid = generateGrid(newConfig, newLevel, random)
        val (cannon, ct) = randomBubble(newConfig, random)
        val (next, nt) = randomBubble(newConfig, random)
        return state.copy(
            grid = newGrid, level = newLevel,
            topOffset = initialTopOffset(rowsForLevel(newConfig, newLevel), state.aspect),
            flying = null, missStreak = 0, popping = emptySet(), popTimer = 0f,
            cannonBubble = cannon, cannonBubbleType = ct,
            nextBubble = next, nextBubbleType = nt,
            cannonCooldown = 0f, comboMultiplier = 1f, comboTimer = 0f,
        )
    }

    // ─── Track / snake helpers ────────────────────────────────────────────────

    fun trackLength(): Float {
        var len = 0f
        for (i in 0 until TRACK_WAYPOINTS.size - 1)
            len += TRACK_WAYPOINTS[i].distanceTo(TRACK_WAYPOINTS[i + 1])
        return len
    }

    /** Convert path-distance [t] to a (x, y) position along the S-shaped track. */
    fun trackPosition(t: Float): Vec2 {
        var rem = t.coerceAtLeast(0f)
        for (i in 0 until TRACK_WAYPOINTS.size - 1) {
            val segLen = TRACK_WAYPOINTS[i].distanceTo(TRACK_WAYPOINTS[i + 1])
            if (rem <= segLen) return TRACK_WAYPOINTS[i].lerp(TRACK_WAYPOINTS[i + 1], rem / segLen)
            rem -= segLen
        }
        return TRACK_WAYPOINTS.last()
    }

    fun generateChain(config: BubblesPopConfig, level: Int, random: Random): List<ChainBubble> {
        val colors = ALL_COLORS.take(config.colorsInPlay.coerceIn(1, ALL_COLORS.size))
        val snakePowerUps = listOf(
            BubblePowerUp.LIGHTNING, BubblePowerUp.SLOW_TIME,
            BubblePowerUp.REVERSE, BubblePowerUp.BOMB_BLAST, BubblePowerUp.COLOR_STORM,
        )
        return (0 until config.chainLength).map { i ->
            val t = i * CHAIN_BUBBLE_SPACING
            val color = colors[random.nextInt(colors.size)]
            val type = if (random.nextFloat() < config.powerUpFrequency) BubbleType.POWER_UP else BubbleType.NORMAL
            val powerUp = if (type == BubbleType.POWER_UP) snakePowerUps[random.nextInt(snakePowerUps.size)] else null
            ChainBubble(t, color, type, powerUp)
        }
    }

    fun initialSnakeState(config: BubblesPopConfig, random: Random = Random.Default, aspect: Float = 1f): BubblesSnakeState {
        val (cannon, ct) = randomBubble(config, random)
        val (next, nt) = randomBubble(config, random)
        return BubblesSnakeState(
            chain = emptyList(),
            launcherAngle = 0f,
            cannonBubble = cannon, cannonBubbleType = ct,
            nextBubble = next, nextBubbleType = nt,
            flying = null,
            swapRemaining = 3,
            score = 0, level = 1, lives = config.initialLives,
            cannonCooldown = 0f,
            chainSpeed = config.initialChainSpeed,
            activePowerUps = emptyList(),
            reverseTimer = 0f, colorStormTimer = 0f, colorStormColor = null,
            backfirePenalty = config.backfirePenalty,
            bestScore = 0,
            launcherX = 0.5f,
            spawnRemaining = config.chainLength,
            aspect = aspect,
        )
    }

    // ─── Snake shooting ───────────────────────────────────────────────────────

    /** Slide the bottom launcher to normalized x [targetX] (the player drags to move it). */
    fun moveLauncher(state: BubblesSnakeState, targetX: Float): BubblesSnakeState =
        state.copy(launcherX = targetX.coerceIn(BUBBLE_RADIUS, 1f - BUBBLE_RADIUS))

    /** Auto-fire a bubble straight up from the bottom launcher. */
    private fun autoFire(state: BubblesSnakeState, config: BubblesPopConfig, random: Random): BubblesSnakeState {
        val (nc, nct) = randomBubble(config, random)
        return state.copy(
            flying = SnakeFlyingBubble(
                x = state.launcherX, y = LAUNCHER_Y,
                dx = 0f, dy = -BUBBLE_SPEED,
                color = state.cannonBubble, type = state.cannonBubbleType,
            ),
            cannonBubble = state.nextBubble, cannonBubbleType = state.nextBubbleType,
            nextBubble = nc, nextBubbleType = nct,
            cannonCooldown = config.cannonCooldown,
            launcherAngle = 0f,
        )
    }

    // ─── Snake step ───────────────────────────────────────────────────────────

    fun stepSnake(
        state: BubblesSnakeState,
        config: BubblesPopConfig,
        dt: Float,
        random: Random = Random.Default,
    ): Pair<BubblesSnakeState, BubblesSnakeEvent> {
        var s = state
        val totalLen = trackLength()

        // Decay timers
        if (s.cannonCooldown > 0f)
            s = s.copy(cannonCooldown = (s.cannonCooldown - dt).coerceAtLeast(0f))
        if (s.reverseTimer > 0f)
            s = s.copy(reverseTimer = (s.reverseTimer - dt).coerceAtLeast(0f))
        if (s.colorStormTimer > 0f) {
            val newT = (s.colorStormTimer - dt).coerceAtLeast(0f)
            s = s.copy(colorStormTimer = newT, colorStormColor = if (newT == 0f) null else s.colorStormColor)
        }
        s = s.copy(
            activePowerUps = s.activePowerUps
                .map { it.copy(remainingSeconds = it.remainingSeconds - dt) }
                .filter { it.remainingSeconds > 0f },
        )

        // Advance chain
        val slowActive = s.activePowerUps.any { it.type == BubblePowerUp.SLOW_TIME }
        val speedMult = when {
            s.reverseTimer > 0f -> -1f
            slowActive -> 0.5f
            else -> 1f
        }
        val advancedChain = s.chain.map { it.copy(t = it.t + s.chainSpeed * speedMult * dt) }

        // Requirement 13: the snake slithers in from the top of the path over time.
        var newChain = advancedChain
        var spawnRemaining = s.spawnRemaining
        if (spawnRemaining > 0 && (newChain.isEmpty() || newChain.minOf { it.t } >= CHAIN_BUBBLE_SPACING)) {
            val colors = ALL_COLORS.take(config.colorsInPlay.coerceIn(1, ALL_COLORS.size))
            newChain = newChain + ChainBubble(0f, colors[random.nextInt(colors.size)])
            spawnRemaining -= 1
        }
        s = s.copy(chain = newChain.sortedBy { it.t }, spawnRemaining = spawnRemaining)

        // Check for bubbles that exited the vortex (t > totalLen)
        if (s.chain.any { it.t > totalLen }) {
            return if (s.lives > 1) {
                Pair(
                    s.copy(
                        chain = emptyList(), spawnRemaining = config.chainLength,
                        lives = s.lives - 1, flying = null,
                    ),
                    BubblesSnakeEvent.BubbleExited,
                )
            } else {
                Pair(s.copy(lives = 0), BubblesSnakeEvent.BubbleExited)
            }
        }

        // Requirement 14: the launcher auto-fires whenever it is ready.
        if (s.flying == null && s.cannonCooldown <= 0f) {
            s = autoFire(s, config, random)
        }

        // Advance flying bubble
        val flying = s.flying ?: return Pair(s, BubblesSnakeEvent.None)
        val fx = flying.x + flying.dx * dt
        val fy = flying.y + flying.dy * dt

        // Requirement 15: a missed shot has no penalty — it just disappears.
        if (fx < 0f || fx > 1f || fy < 0f || fy > 1f) {
            return Pair(s.copy(flying = null), BubblesSnakeEvent.None)
        }

        // Collision with chain bubbles (aspect-corrected)
        val hitIndex = s.chain.indexOfFirst { bubble ->
            val pos = trackPosition(bubble.t)
            hypot(fx - pos.x, (fy - pos.y) / s.aspect) < BUBBLE_RADIUS * 2f
        }

        if (hitIndex < 0) {
            return Pair(s.copy(flying = flying.copy(x = fx, y = fy)), BubblesSnakeEvent.None)
        }

        return resolveSnakeHit(s, config, flying, hitIndex)
    }

    /**
     * Requirement 16: a shot removes the whole contiguous run of its own color at the
     * impact point (1 or more bubbles). A non-matching hit does nothing (requirement 15).
     */
    private fun resolveSnakeHit(
        s: BubblesSnakeState,
        config: BubblesPopConfig,
        flying: SnakeFlyingBubble,
        hitIndex: Int,
    ): Pair<BubblesSnakeState, BubblesSnakeEvent> {
        val shotColor = s.colorStormColor ?: flying.color
        val run = contiguousColorRun(s.chain, hitIndex, shotColor, flying.type)
            ?: return Pair(s.copy(flying = null), BubblesSnakeEvent.None)

        val count = run.last - run.first + 1
        val baseScore = calculatePopScore(count).coerceAtLeast(count * 10)
        val remaining = contractChain(s.chain.filterIndexed { i, _ -> i !in run })
        val (finalChain, cascadeScore) = checkCascades(remaining)
        val total = baseScore + cascadeScore

        return if (finalChain.isEmpty() && s.spawnRemaining <= 0) {
            val levelBonus = 300 * s.level
            Pair(
                s.copy(chain = emptyList(), flying = null, score = s.score + total + levelBonus),
                BubblesSnakeEvent.ChainCleared,
            )
        } else {
            Pair(
                s.copy(chain = finalChain, flying = null, score = s.score + total),
                BubblesSnakeEvent.BubblePopped(count, total),
            )
        }
    }

    /** Maximal contiguous range of [color] around [index], or null if the hit bubble doesn't match. */
    private fun contiguousColorRun(
        chain: List<ChainBubble>,
        index: Int,
        color: BubbleColor,
        shotType: BubbleType,
    ): IntRange? {
        if (index < 0 || index >= chain.size) return null
        fun matches(b: ChainBubble) = b.type != BubbleType.STONE &&
                (b.color == color || b.type == BubbleType.RAINBOW || shotType == BubbleType.RAINBOW)
        if (!matches(chain[index])) return null
        var start = index
        var end = index
        while (start > 0 && matches(chain[start - 1])) start--
        while (end < chain.size - 1 && matches(chain[end + 1])) end++
        return start..end
    }

    /** Find indices of a 3+ same-color run in [chain] centered around [index]. */
    fun findChainMatch(chain: List<ChainBubble>, index: Int): IntRange? {
        if (index < 0 || index >= chain.size) return null
        val bubble = chain[index]
        if (bubble.type == BubbleType.STONE) return null
        val targetColor = bubble.color
        var start = index
        var end = index
        while (start > 0 && chainColorMatches(chain[start - 1], targetColor)) start--
        while (end < chain.size - 1 && chainColorMatches(chain[end + 1], targetColor)) end++
        return if (end - start + 1 >= 3) start..end else null
    }

    private fun chainColorMatches(bubble: ChainBubble, color: BubbleColor): Boolean =
        bubble.type != BubbleType.STONE &&
                (bubble.color == color || bubble.type == BubbleType.RAINBOW)

    /** Re-index chain bubbles so they are evenly spaced starting from 0. */
    fun contractChain(chain: List<ChainBubble>): List<ChainBubble> {
        if (chain.isEmpty()) return emptyList()
        return chain.sortedBy { it.t }.mapIndexed { i, b -> b.copy(t = i * CHAIN_BUBBLE_SPACING) }
    }

    private fun checkCascades(chain: List<ChainBubble>): Pair<List<ChainBubble>, Int> {
        var current = chain
        var totalScore = 0
        var found = true
        while (found) {
            found = false
            for (i in current.indices) {
                val match = findChainMatch(current, i)
                if (match != null) {
                    val count = match.last - match.first + 1
                    totalScore += calculatePopScore(count) + 200
                    current = contractChain(current.filterIndexed { idx, _ -> idx !in match.toSet() })
                    found = true
                    break
                }
            }
        }
        return current to totalScore
    }

    /** Swap cannon and next bubble (costs a swap). */
    fun swapBubbles(state: BubblesSnakeState): BubblesSnakeState {
        if (state.swapRemaining <= 0) return state
        return state.copy(
            cannonBubble = state.nextBubble,
            cannonBubbleType = state.nextBubbleType,
            nextBubble = state.cannonBubble,
            nextBubbleType = state.cannonBubbleType,
            swapRemaining = state.swapRemaining - 1,
        )
    }

    fun nextLevelSnake(
        state: BubblesSnakeState,
        config: BubblesPopConfig,
        random: Random = Random.Default,
    ): BubblesSnakeState {
        val newLevel = state.level + 1
        val newColors = minOf(config.colorsInPlay + 1, ALL_COLORS.size)
        val newConfig = config.copy(colorsInPlay = newColors)
        val (cannon, ct) = randomBubble(newConfig, random)
        val (next, nt) = randomBubble(newConfig, random)
        return state.copy(
            chain = emptyList(), spawnRemaining = newConfig.chainLength, level = newLevel,
            chainSpeed = state.chainSpeed + config.chainSpeedIncrement,
            cannonBubble = cannon, cannonBubbleType = ct,
            nextBubble = next, nextBubbleType = nt,
            flying = null, cannonCooldown = 0f, swapRemaining = 3,
            backfirePenalty = config.backfirePenalty + if (newLevel % 3 == 0) 1 else 0,
        )
    }
}
