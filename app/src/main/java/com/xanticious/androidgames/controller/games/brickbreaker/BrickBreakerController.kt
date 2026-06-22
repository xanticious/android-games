package com.xanticious.androidgames.controller.games.brickbreaker

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.reflect
import com.xanticious.androidgames.model.games.brickbreaker.ActivePowerUp
import com.xanticious.androidgames.model.games.brickbreaker.Ball
import com.xanticious.androidgames.model.games.brickbreaker.BrickBreakerConfig
import com.xanticious.androidgames.model.games.brickbreaker.BrickBreakerInput
import com.xanticious.androidgames.model.games.brickbreaker.BrickBreakerState
import com.xanticious.androidgames.model.games.brickbreaker.BrickBreakerStepResult
import com.xanticious.androidgames.model.games.brickbreaker.BrickBreakerVariant
import com.xanticious.androidgames.model.games.brickbreaker.BrickField
import com.xanticious.androidgames.model.games.brickbreaker.BrickType
import com.xanticious.androidgames.model.games.brickbreaker.Brick
import com.xanticious.androidgames.model.games.brickbreaker.DroppingPowerUp
import com.xanticious.androidgames.model.games.brickbreaker.PowerUpType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

/**
 * Config-driven pure controller for all four brick-breaker variants.
 *
 * No Android or Compose imports — fully JVM-testable.
 */
class BrickBreakerController {

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    fun configFor(variant: BrickBreakerVariant, difficulty: GameDifficulty): BrickBreakerConfig =
        when (variant) {
            BrickBreakerVariant.CLASSIC -> when (difficulty) {
                GameDifficulty.EASY -> BrickBreakerConfig(
                    variant = variant, ballSpeed = 0.6f,
                    descentSpeed = 0f, autoFireInterval = 0f, newRowInterval = 0f,
                    startingTurns = 10,
                )
                GameDifficulty.MEDIUM -> BrickBreakerConfig(
                    variant = variant, ballSpeed = 0.75f,
                    descentSpeed = 0f, autoFireInterval = 0f, newRowInterval = 0f,
                    startingTurns = 10,
                )
                GameDifficulty.HARD -> BrickBreakerConfig(
                    variant = variant, ballSpeed = 0.9f,
                    descentSpeed = 0f, autoFireInterval = 0f, newRowInterval = 0f,
                    startingTurns = 10,
                )
            }
            BrickBreakerVariant.ARCADE -> when (difficulty) {
                GameDifficulty.EASY -> BrickBreakerConfig(
                    variant = variant, ballSpeed = 0.65f,
                    descentSpeed = 0.08f, autoFireInterval = 0.35f, newRowInterval = 6f,
                    livesStart = 3, totalRowsForLevel = 10,
                )
                GameDifficulty.MEDIUM -> BrickBreakerConfig(
                    variant = variant, ballSpeed = 0.80f,
                    descentSpeed = 0.12f, autoFireInterval = 0.30f, newRowInterval = 5f,
                    livesStart = 3, totalRowsForLevel = 12,
                )
                GameDifficulty.HARD -> BrickBreakerConfig(
                    variant = variant, ballSpeed = 0.95f,
                    descentSpeed = 0.18f, autoFireInterval = 0.25f, newRowInterval = 4f,
                    livesStart = 2, totalRowsForLevel = 14,
                )
            }
            BrickBreakerVariant.CANNON -> when (difficulty) {
                GameDifficulty.EASY -> BrickBreakerConfig(
                    variant = variant, ballSpeed = 0.65f, gravity = 0.22f, maxBounces = 2,
                    descentSpeed = 0f, autoFireInterval = 0f, newRowInterval = 0f,
                    startingTurns = 12,
                )
                GameDifficulty.MEDIUM -> BrickBreakerConfig(
                    variant = variant, ballSpeed = 0.75f, gravity = 0.28f, maxBounces = 2,
                    descentSpeed = 0f, autoFireInterval = 0f, newRowInterval = 0f,
                    startingTurns = 10,
                )
                GameDifficulty.HARD -> BrickBreakerConfig(
                    variant = variant, ballSpeed = 0.85f, gravity = 0.35f, maxBounces = 2,
                    descentSpeed = 0f, autoFireInterval = 0f, newRowInterval = 0f,
                    startingTurns = 8,
                )
            }
            BrickBreakerVariant.CANNON_ARCADE -> when (difficulty) {
                GameDifficulty.EASY -> BrickBreakerConfig(
                    variant = variant, ballSpeed = 0.65f, gravity = 0.22f, maxBounces = 2,
                    descentSpeed = 0f, autoFireInterval = 0f, newRowInterval = 0f,
                    startTimerSeconds = 70f, shotCooldown = 0.55f,
                )
                GameDifficulty.MEDIUM -> BrickBreakerConfig(
                    variant = variant, ballSpeed = 0.75f, gravity = 0.28f, maxBounces = 2,
                    descentSpeed = 0f, autoFireInterval = 0f, newRowInterval = 0f,
                    startTimerSeconds = 60f, shotCooldown = 0.50f,
                )
                GameDifficulty.HARD -> BrickBreakerConfig(
                    variant = variant, ballSpeed = 0.85f, gravity = 0.35f, maxBounces = 2,
                    descentSpeed = 0f, autoFireInterval = 0f, newRowInterval = 0f,
                    startTimerSeconds = 45f, shotCooldown = 0.60f,
                )
            }
        }

    // -------------------------------------------------------------------------
    // Level generation
    // -------------------------------------------------------------------------

    fun generateLevel(config: BrickBreakerConfig, level: Int): BrickBreakerState {
        val bricks = when (config.variant) {
            BrickBreakerVariant.CLASSIC -> generateClassicFeedBricks(level)
            BrickBreakerVariant.ARCADE -> generateArcadeBricks(level)
            BrickBreakerVariant.CANNON, BrickBreakerVariant.CANNON_ARCADE ->
                settleCannonBricks(generateCannonBricks(level))
        }
        val turnsLeft = config.startingTurns + level
        val timerSecs = (config.startTimerSeconds - (level - 1) * 5f).coerceAtLeast(30f)
        return BrickBreakerState(
            bricks = bricks,
            level = level,
            ballCount = 1,
            lives = config.livesStart,
            totalRowsForLevel = config.totalRowsForLevel,
            turnsLeft = turnsLeft,
            timerSeconds = timerSecs,
            nextRowTimer = config.newRowInterval,
        )
    }

    /**
     * CLASSIC level layout.  A level requires destroying [level] rows (capped at
     * [BrickField.CLASSIC_MAX_ROWS]).  Up to [BrickField.CLASSIC_VISIBLE_ROWS]
     * rows start on screen (rows 0..N-1); any remaining rows are stacked above
     * the field at negative row indices and descend into view one row per turn.
     */
    fun generateClassicFeedBricks(level: Int): List<Brick> {
        val totalRows = level.coerceIn(1, BrickField.CLASSIC_MAX_ROWS)
        val visible = minOf(totalRows, BrickField.CLASSIC_VISIBLE_ROWS)
        val maxHp = level * 3
        val cols = BrickField.COLS
        val useSteelFrom = 4
        val bricks = mutableListOf<Brick>()
        repeat(totalRows) { i ->
            // i = 0 is the deepest on-screen row; higher i stacks upward/off screen.
            val row = (visible - 1) - i
            // Every 3rd row is a sparse row containing only 1-2 bricks.
            if (i % 3 == 2) {
                val count = Random.nextInt(1, 3)
                val chosenCols = (0 until cols).shuffled().take(count)
                for (col in chosenCols) {
                    val hp = Random.nextInt(1, maxHp + 1)
                    val r = Random.nextFloat()
                    val type = when {
                        level >= useSteelFrom && r < 0.10f -> BrickType.STEEL
                        r < 0.22f -> BrickType.POWERUP
                        else -> BrickType.STANDARD
                    }
                    val pu = if (type == BrickType.POWERUP) randomBallStrengthPowerUp() else null
                    val steelHp = if (type == BrickType.STEEL) hp * 2 else hp
                    bricks += Brick(col, row, steelHp, steelHp, type, pu)
                }
                return@repeat
            }
            repeat(cols) { col ->
                if (Random.nextFloat() > 0.15f) {  // ~85% fill
                    val hp = Random.nextInt(1, maxHp + 1)
                    val r = Random.nextFloat()
                    val type = when {
                        level >= useSteelFrom && r < 0.10f -> BrickType.STEEL
                        r < 0.22f -> BrickType.POWERUP
                        else -> BrickType.STANDARD
                    }
                    val pu = if (type == BrickType.POWERUP) randomBallStrengthPowerUp() else null
                    val steelHp = if (type == BrickType.STEEL) hp * 2 else hp
                    bricks += Brick(col, row, steelHp, steelHp, type, pu)
                }
            }
        }
        return bricks
    }

    private fun generateArcadeBricks(level: Int): List<Brick> {
        val maxHp = level * 3
        val cols = BrickField.COLS
        val numRows = (2 + level).coerceAtMost(6)
        val bricks = mutableListOf<Brick>()
        val useSteelFrom = 4
        repeat(numRows) { row ->
            repeat(cols) { col ->
                if (Random.nextFloat() > 0.15f) {  // ~85% fill
                    val hp = Random.nextInt(1, maxHp + 1)
                    val r = Random.nextFloat()
                    val type = when {
                        level >= useSteelFrom && r < 0.10f -> BrickType.STEEL
                        r < 0.045f -> BrickType.POWERUP
                        else -> BrickType.STANDARD
                    }
                    val pu = if (type == BrickType.POWERUP) PowerUpType.EXTRA_STRENGTH else null
                    val steelHp = if (type == BrickType.STEEL) hp * 2 else hp
                    bricks += Brick(col, row, steelHp, steelHp, type, pu)
                }
            }
        }
        return bricks
    }

    /**
     * Connected polyomino piece templates (cell offsets, origin at top-left).
     * Sizes range from a single cell up to six connected cells, including
     * rectangles and "C" / "S" / "L" / "T" silhouettes.
     */
    private val cannonPieceShapes: List<List<Pair<Int, Int>>> = listOf(
        // 1x1
        listOf(0 to 0),
        // 1x2 vertical, 2x1 horizontal
        listOf(0 to 0, 0 to 1),
        listOf(0 to 0, 1 to 0),
        // 1x3 vertical, 3x1 horizontal
        listOf(0 to 0, 0 to 1, 0 to 2),
        listOf(0 to 0, 1 to 0, 2 to 0),
        // 1x4 vertical
        listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3),
        // 2x2 square
        listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1),
        // 2x3 rectangle
        listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1, 0 to 2, 1 to 2),
        // L
        listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2),
        // T
        listOf(0 to 0, 1 to 0, 2 to 0, 1 to 1),
        // S
        listOf(1 to 0, 2 to 0, 0 to 1, 1 to 1),
        // C / U
        listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2, 2 to 0, 2 to 1, 2 to 2),
    )

    private fun generateCannonBricks(level: Int): List<Brick> {
        val maxHp = (level * 2).coerceAtMost(6)
        val cols = BrickField.COLS
        val bottomRow = BrickField.CANNON_BOTTOM_ROW
        // Leave the leftmost columns clear so the cannon has firing room.
        val firstCol = 2
        val occupied = HashSet<Pair<Int, Int>>()
        val raw = mutableListOf<Brick>()
        val pieceCount = (2 + level).coerceAtMost(6)

        repeat(pieceCount) {
            val shape = cannonPieceShapes.random()
            val shapeW = shape.maxOf { it.first } + 1
            val shapeH = shape.maxOf { it.second } + 1
            val maxCol = cols - shapeW
            if (maxCol < firstCol) return@repeat
            val baseCol = Random.nextInt(firstCol, maxCol + 1)
            // Stack on top of whatever already occupies these columns.
            val shapeCols = shape.map { baseCol + it.first }.distinct()
            val highestOccupied = shapeCols.minOfOrNull { c ->
                (0..bottomRow).firstOrNull { r -> occupied.contains(c to r) } ?: (bottomRow + 1)
            } ?: (bottomRow + 1)
            val bottomOfShape = highestOccupied - 1
            val topRow = bottomOfShape - (shapeH - 1)
            if (topRow < 0) return@repeat
            val cells = shape.map { (baseCol + it.first) to (topRow + it.second) }
            if (cells.any { occupied.contains(it) }) return@repeat
            for ((c, r) in cells) {
                occupied += c to r
                raw += Brick(c, r, 1, 1, BrickType.STANDARD)
            }
        }

        // Assign HP and promote some bricks to TARGET / STEEL / POWERUP.
        val targetCount = (1 + level).coerceAtMost(8)
        val shuffled = raw.indices.shuffled()
        val targetIdx = shuffled.take(targetCount).toHashSet()
        return raw.mapIndexed { i, brick ->
            val hp = Random.nextInt(1, maxHp + 1)
            val rnd = Random.nextFloat()
            val type = when {
                i in targetIdx -> BrickType.TARGET
                level >= 3 && rnd < 0.08f -> BrickType.STEEL
                rnd < 0.12f -> BrickType.POWERUP
                else -> BrickType.STANDARD
            }
            val pu = if (type == BrickType.POWERUP) randomPowerUpCannon() else null
            val finalHp = if (type == BrickType.STEEL) hp * 2 else hp
            brick.copy(hp = finalHp, maxHp = finalHp, type = type, powerUp = pu)
        }
    }

    /**
     * CANNON variants: pull every brick down to the ground.  Each connected
     * group of bricks falls as a rigid body until one of its cells rests on the
     * ground row or on top of another group, so breaking a lower brick lets the
     * bricks above it fall.
     */
    fun settleCannonBricks(bricks: List<Brick>): List<Brick> {
        if (bricks.isEmpty()) return bricks
        val bottomRow = BrickField.CANNON_BOTTOM_ROW
        var current = bricks
        repeat(BrickField.ROWS * 2) {
            val components = connectedComponents(current)
            val occupied = HashMap<Pair<Int, Int>, Int>()
            current.forEachIndexed { idx, b -> occupied[b.col to b.row] = idx }
            // Map each brick index to its component id.
            val componentOf = IntArray(current.size) { -1 }
            components.forEachIndexed { cid, comp -> comp.forEach { componentOf[it] = cid } }

            var moved = false
            // Process components nearest the ground first.
            val order = components.indices.sortedByDescending { cid ->
                components[cid].maxOf { current[it].row }
            }
            val mutableState = current.toMutableList()
            val occ = HashMap<Pair<Int, Int>, Int>().apply { putAll(occupied) }
            for (cid in order) {
                val members = components[cid]
                val canMove = members.all { idx ->
                    val b = mutableState[idx]
                    if (b.row + 1 > bottomRow) return@all false
                    val below = (b.col to (b.row + 1))
                    val occupant = occ[below]
                    occupant == null || componentOf[occupant] == cid
                }
                if (canMove) {
                    members.forEach { idx -> occ.remove(mutableState[idx].col to mutableState[idx].row) }
                    members.forEach { idx ->
                        val b = mutableState[idx]
                        mutableState[idx] = b.copy(row = b.row + 1)
                        occ[mutableState[idx].col to mutableState[idx].row] = idx
                    }
                    moved = true
                }
            }
            current = mutableState
            if (!moved) return current
        }
        return current
    }

    /** 4-connected components of the brick grid, returned as lists of indices. */
    private fun connectedComponents(bricks: List<Brick>): List<List<Int>> {
        val cellToIndex = HashMap<Pair<Int, Int>, Int>()
        bricks.forEachIndexed { idx, b -> cellToIndex[b.col to b.row] = idx }
        val visited = BooleanArray(bricks.size)
        val result = mutableListOf<List<Int>>()
        for (start in bricks.indices) {
            if (visited[start]) continue
            val comp = mutableListOf<Int>()
            val stack = ArrayDeque<Int>()
            stack.addLast(start)
            visited[start] = true
            while (stack.isNotEmpty()) {
                val cur = stack.removeLast()
                comp += cur
                val b = bricks[cur]
                val neighbours = listOf(
                    b.col - 1 to b.row, b.col + 1 to b.row,
                    b.col to b.row - 1, b.col to b.row + 1,
                )
                for (n in neighbours) {
                    val ni = cellToIndex[n] ?: continue
                    if (!visited[ni]) {
                        visited[ni] = true
                        stack.addLast(ni)
                    }
                }
            }
            result += comp
        }
        return result
    }


    /** CLASSIC / ARCADE power-ups: each is worth one ball or one strength point. */
    private fun randomBallStrengthPowerUp(): PowerUpType =
        if (Random.nextFloat() < 0.5f) PowerUpType.EXTRA_BALL else PowerUpType.EXTRA_STRENGTH

    private fun randomPowerUpCannon(): PowerUpType {
        val pool = listOf(
            PowerUpType.EXPLODE,
            PowerUpType.MULTI_SHOT,
            PowerUpType.POWER_SHOT,
            PowerUpType.CLEAR_SCREEN,
        )
        return pool.random()
    }

    // -------------------------------------------------------------------------
    // Turn-based helpers (CLASSIC / CANNON)
    // -------------------------------------------------------------------------

    /** Launch one ball in a classic volley from the paddle position. */
    fun launchClassicBall(state: BrickBreakerState, config: BrickBreakerConfig): BrickBreakerState {
        if (state.ballsToFire <= 0) return state
        val angleDeg = state.cannonAngleDeg.coerceIn(10f, 170f)
        val rad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val vx = cos(rad)
        val vy = -sin(rad)  // negative because screen y grows down
        val speed = config.ballSpeed * (if (state.hasPowerShot) 1f else 1f)  // speed unchanged
        val ball = Ball(
            pos = Vec2(state.paddleX, BrickField.CANNON_Y),
            vel = Vec2(vx, vy).normalized() * speed
        )
        val count = if (state.hasMultiShot) 3 else 1
        val extraBalls = (1 until count).map { i ->
            val spreadDeg = (i - count / 2f) * 8f
            val a = Math.toRadians((angleDeg + spreadDeg).toDouble()).toFloat()
            Ball(
                pos = Vec2(state.paddleX, BrickField.CANNON_Y),
                vel = Vec2(cos(a), -sin(a)).normalized() * speed
            )
        }
        return state.copy(
            balls = state.balls + ball + extraBalls,
            ballsToFire = state.ballsToFire - 1,
            fireTimer = config.volleyInterval
        )
    }

    /** Prepare state to begin an aim phase (classic volley reset). */
    fun beginVolley(state: BrickBreakerState, config: BrickBreakerConfig): BrickBreakerState =
        state.copy(
            balls = emptyList(),
            ballsToFire = state.ballCount.coerceAtLeast(1),
            fireTimer = 0f,
        )

    /** Launch a single cannon ball (CANNON / CANNON_ARCADE). */
    fun launchCannonBall(state: BrickBreakerState, config: BrickBreakerConfig): BrickBreakerState {
        val angleDeg = state.cannonAngleDeg.coerceIn(5f, 85f)
        val rad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val vx = cos(rad)
        val vy = -sin(rad)
        val speed = config.ballSpeed
        val damage = if (state.hasPowerShot) config.baseDamage * 2 else config.baseDamage
        val origin = Vec2(BrickField.CANNON_X, BrickField.GROUND_Y - BrickField.BALL_RADIUS)
        val primary = Ball(pos = origin, vel = Vec2(vx, vy) * speed)
        val extra = if (state.hasMultiShot) {
            listOf(Ball(pos = origin, vel = Vec2(vx, vy + 0.06f).normalized() * speed))
        } else emptyList()
        return state.copy(
            balls = state.balls + primary + extra,
            fireCooldown = config.shotCooldown
        )
    }

    // -------------------------------------------------------------------------
    // Main step function
    // -------------------------------------------------------------------------

    /**
     * Advance the game simulation by [dt] seconds.
     *
     * Handles:
     * - Ball movement + wall / brick collisions
     * - Automatic ball launching (CLASSIC volley & ARCADE auto-fire)
     * - Brick descent (ARCADE)
     * - Power-up drift + collection
     * - Timer countdown (CANNON_ARCADE)
     * - Active power-up timers
     */
    fun step(
        state: BrickBreakerState,
        config: BrickBreakerConfig,
        dt: Float,
        input: BrickBreakerInput,
    ): BrickBreakerStepResult {
        var s = state

        // Tick active power-up timers.
        s = tickPowerUps(s, dt)

        // Update player input (paddle / aim).
        s = when (config.variant) {
            BrickBreakerVariant.CLASSIC ->
                s.copy(
                    paddleX = input.paddleX.coerceIn(0.05f, 0.95f),
                    cannonAngleDeg = input.aimAngleDeg.coerceIn(10f, 170f),
                )
            BrickBreakerVariant.ARCADE ->
                s.copy(paddleX = input.paddleX.coerceIn(0.05f, 0.95f))
            BrickBreakerVariant.CANNON, BrickBreakerVariant.CANNON_ARCADE ->
                s.copy(cannonAngleDeg = input.aimAngleDeg.coerceIn(5f, 85f))
        }

        // CLASSIC: tick volley timer and launch next ball.
        if (config.variant == BrickBreakerVariant.CLASSIC) {
            val ft = s.fireTimer - dt
            s = s.copy(fireTimer = ft)
            if (ft <= 0f && s.ballsToFire > 0) {
                s = launchClassicBall(s, config)
            }
        }

        // ARCADE: auto-fire cannon (always multi-shot 3-ball spread).
        if (config.variant == BrickBreakerVariant.ARCADE) {
            val ft = (s.fireTimer - dt).coerceAtLeast(0f)
            s = s.copy(fireTimer = ft)
            if (ft <= 0f) {
                val effectiveInterval = if (s.hasRapidFire)
                    config.autoFireInterval * config.rapidFireMultiplier
                else config.autoFireInterval
                s = s.copy(fireTimer = effectiveInterval)
                s = spawnArcadeVolley(s, config)
            }
        }

        // CANNON_ARCADE: fire-cooldown tick + fire when requested.
        if (config.variant == BrickBreakerVariant.CANNON_ARCADE) {
            val cd = (s.fireCooldown - dt).coerceAtLeast(0f)
            s = s.copy(fireCooldown = cd, timerSeconds = (s.timerSeconds - dt).coerceAtLeast(0f))
            if (input.fireRequested && cd <= 0f) {
                s = launchCannonBall(s, config)
            }
        }

        // Move all balls, check collisions.
        val (newBalls, newBricks, newDrops, scoreGain) = moveBalls(s, config, dt)
        s = s.copy(
            balls = newBalls,
            bricks = newBricks,
            score = s.score + scoreGain,
        )
        // CANNON variants: gravity settles the towers after any brick is broken.
        if (config.variant == BrickBreakerVariant.CANNON ||
            config.variant == BrickBreakerVariant.CANNON_ARCADE
        ) {
            s = s.copy(bricks = settleCannonBricks(s.bricks))
        }
        // CLASSIC collects ball / strength power-ups instantly (no falling icon).
        // ARCADE applies every power-up instantly at the destroyed brick — no
        // catch-to-collect.  Remaining variants let the icon drift down.
        s = when (config.variant) {
            BrickBreakerVariant.CLASSIC -> collectInstantly(s, newDrops.map { it.type })
            BrickBreakerVariant.ARCADE -> {
                var t = s
                for (drop in newDrops) t = applyPowerUp(t, config, drop.type, drop.pos.x, drop.pos.y)
                t
            }
            else -> s.copy(droppingPowerUps = s.droppingPowerUps + newDrops)
        }

        // ARCADE: descend bricks and spawn new rows (power-ups already collected
        // instantly above when their brick was destroyed).
        var brickAtBottom = false
        var levelRowsCleared = false
        if (config.variant == BrickBreakerVariant.ARCADE) {
            val (sAfterDescent, hit) = descendBricks(s, config, dt)
            s = sAfterDescent
            brickAtBottom = hit

            // Row generation.
            val rowTimer = s.nextRowTimer - dt
            if (rowTimer <= 0f && s.rowsGenerated < s.totalRowsForLevel) {
                s = addTopRow(s, config)
                s = s.copy(nextRowTimer = config.newRowInterval, rowsGenerated = s.rowsGenerated + 1)
            } else {
                s = s.copy(nextRowTimer = rowTimer.coerceAtLeast(0f))
            }

            // Level completion check.
            if (s.rowsGenerated >= s.totalRowsForLevel && s.bricks.isEmpty()) {
                levelRowsCleared = true
            }
        }

        // Tick dropping power-ups (age + position).
        s = tickDroppingPowerUps(s, dt)

        val allBallsDone = s.balls.isEmpty() &&
                (s.ballsToFire <= 0 || config.variant == BrickBreakerVariant.CANNON_ARCADE)
        val allTargetsCleared = allTargetsDestroyed(s)
        val fieldCleared = allBricksGone(s)
        val visibleCleared = config.variant == BrickBreakerVariant.CLASSIC && noVisibleBricks(s)
        val timerExpired = config.variant == BrickBreakerVariant.CANNON_ARCADE && s.timerSeconds <= 0f
        val livesGone = config.variant == BrickBreakerVariant.ARCADE && brickAtBottom && s.lives <= 1

        return BrickBreakerStepResult(
            state = s,
            allBallsDone = allBallsDone,
            brickReachedBottom = brickAtBottom,
            allTargetsCleared = allTargetsCleared,
            fieldCleared = fieldCleared,
            timerExpired = timerExpired,
            levelRowsCleared = levelRowsCleared,
            livesGone = livesGone,
            visibleCleared = visibleCleared,
        )
    }

    // -------------------------------------------------------------------------
    // Ball physics
    // -------------------------------------------------------------------------

    private data class BallMoveResult(
        val balls: List<Ball>,
        val bricks: List<Brick>,
        val newDrops: List<DroppingPowerUp>,
        val scoreGain: Int,
    )

    private fun moveBalls(
        state: BrickBreakerState,
        config: BrickBreakerConfig,
        dt: Float,
    ): BallMoveResult {
        val survivingBalls = mutableListOf<Ball>()
        var bricks = state.bricks.toMutableList()
        val newDrops = mutableListOf<DroppingPowerUp>()
        var score = 0
        val damage = (if (state.hasPowerShot) config.baseDamage * 2 else config.baseDamage) *
                state.strength.coerceAtLeast(1)
        val radius = if (state.hasWideShot) BrickField.BALL_RADIUS * 2f else BrickField.BALL_RADIUS

        for (ball in state.balls) {
            var b = ball
            // Apply gravity (cannon variants).
            if (config.gravity > 0f) {
                b = b.copy(vel = Vec2(b.vel.x, b.vel.y + config.gravity * dt))
            }
            // Integrate position.
            b = b.copy(pos = b.pos + b.vel * dt)

            // Wall bounces.
            var bounces = b.bounces
            var pos = b.pos
            var vel = b.vel

            if (pos.x < radius) { pos = Vec2(radius, pos.y); vel = Vec2(-vel.x, vel.y); bounces++ }
            if (pos.x > 1f - radius) { pos = Vec2(1f - radius, pos.y); vel = Vec2(-vel.x, vel.y); bounces++ }
            if (pos.y < radius) { pos = Vec2(pos.x, radius); vel = Vec2(vel.x, -vel.y); bounces++ }

            b = b.copy(pos = pos, vel = vel, bounces = bounces)

            val isCannon = config.variant == BrickBreakerVariant.CANNON ||
                    config.variant == BrickBreakerVariant.CANNON_ARCADE
            // CANNON variants terminate when the ball reaches the ground line;
            // until then it keeps bouncing off the right wall and ceiling.
            // Other variants terminate when the ball exits the bottom.
            val exitBottom = if (isCannon) {
                b.pos.y > BrickField.GROUND_Y
            } else {
                b.pos.y > 1f + radius
            }
            if (exitBottom) continue

            // Brick collisions.
            val (newBall, newBricks, drops, pts) = collideBall(b, bricks, config, damage, radius)
            b = newBall
            bricks = newBricks.toMutableList()
            newDrops += drops
            score += pts

            survivingBalls += b
        }
        return BallMoveResult(survivingBalls, bricks, newDrops, score)
    }

    private data class CollideResult(
        val ball: Ball,
        val bricks: List<Brick>,
        val drops: List<DroppingPowerUp>,
        val score: Int,
    )

    private fun collideBall(
        ball: Ball,
        bricks: List<Brick>,
        config: BrickBreakerConfig,
        damage: Int,
        radius: Float,
    ): CollideResult {
        var b = ball
        val remaining = bricks.toMutableList()
        val drops = mutableListOf<DroppingPowerUp>()
        var score = 0

        val cols = BrickField.COLS.toFloat()
        val rowH = BrickField.ROW_HEIGHT
        val pad = BrickField.BRICK_PAD
        val top = BrickField.TOP_MARGIN

        for (i in remaining.indices) {
            val brick = remaining[i]
            val left = brick.col / cols
            val right = (brick.col + 1) / cols - pad
            val brickTop = top + brick.row * rowH
            val brickBottom = brickTop + rowH - pad

            // Treat the brick as a rounded rectangle: clamp the ball centre to an
            // inner rectangle inset by the corner radius, then collide against a
            // surface that sits [cr] beyond it.  Grazing a corner yields a
            // diagonal normal, so the ball bounces off at an angle.
            val cr = BrickField.BRICK_CORNER_RADIUS
            val midX = (left + right) / 2f
            val midY = (brickTop + brickBottom) / 2f
            val innerLeft = (left + cr).coerceAtMost(midX)
            val innerRight = (right - cr).coerceAtLeast(midX)
            val innerTop = (brickTop + cr).coerceAtMost(midY)
            val innerBottom = (brickBottom - cr).coerceAtLeast(midY)

            val nearX = b.pos.x.coerceIn(innerLeft, innerRight)
            val nearY = b.pos.y.coerceIn(innerTop, innerBottom)
            val dx = b.pos.x - nearX
            val dy = b.pos.y - nearY
            val distSq = dx * dx + dy * dy
            val reach = radius + cr
            if (distSq >= reach * reach) continue

            // Determine collision normal.
            val normal = if (distSq < 1e-10f) Vec2(0f, -1f) else Vec2(dx, dy).normalized()
            b = b.copy(vel = reflect(b.vel, normal))

            // Push ball outside brick.
            val dist = kotlin.math.sqrt(distSq)
            val push = reach - dist + 0.001f
            b = b.copy(pos = b.pos + normal * push)

            // Steel bricks: standard damage only works if PowerShot is active.
            val effectiveDamage = when {
                brick.type == BrickType.STEEL && !config.variant.isPowerShotApplicable() -> 0
                else -> damage
            }

            if (effectiveDamage <= 0) continue

            val newHp = brick.hp - effectiveDamage
            if (newHp <= 0) {
                // Destroyed.
                remaining[i] = brick.copy(hp = 0)
                score += brickScore(brick, config)
                if (brick.type == BrickType.POWERUP && brick.powerUp != null) {
                    val brickCenterX = (brick.col + 0.5f) / cols
                    val brickCenterY = top + brick.row * rowH + rowH / 2f
                    drops += DroppingPowerUp(brick.powerUp, Vec2(brickCenterX, brickCenterY))
                }
            } else {
                remaining[i] = brick.copy(hp = newHp)
            }
            break  // one brick hit per ball per frame
        }
        val clearedBricks = remaining.filter { it.hp > 0 }
        return CollideResult(b, clearedBricks, drops, score)
    }

    private fun BrickBreakerVariant.isPowerShotApplicable() = true  // power shot applies to steel

    private fun brickScore(brick: Brick, config: BrickBreakerConfig): Int = when (brick.type) {
        BrickType.STANDARD -> brick.maxHp * config.scorePerHpUnit
        BrickType.POWERUP -> (brick.maxHp * config.scorePerHpUnit * config.powerUpBrickMultiplier).toInt()
        BrickType.STEEL -> (brick.maxHp * config.scorePerHpUnit * config.steelBrickMultiplier).toInt()
        BrickType.TARGET -> 300
    }

    // -------------------------------------------------------------------------
    // ARCADE helpers
    // -------------------------------------------------------------------------

    private fun spawnArcadeVolley(state: BrickBreakerState, config: BrickBreakerConfig): BrickBreakerState {
        val speed = config.ballSpeed
        val x = state.paddleX
        val y = BrickField.CANNON_Y
        val newBalls = listOf(
            Ball(Vec2(x, y), Vec2(0f, -speed)),
            Ball(Vec2(x, y), Vec2(-0.18f, -speed).normalized() * speed),
            Ball(Vec2(x, y), Vec2(0.18f, -speed).normalized() * speed),
        )
        return state.copy(balls = state.balls + newBalls)
    }

    private data class DescentResult(val state: BrickBreakerState, val brickReachedBottom: Boolean)

    private fun descendBricks(
        state: BrickBreakerState,
        config: BrickBreakerConfig,
        dt: Float,
    ): DescentResult {
        var offset = state.descentOffset + config.descentSpeed * dt
        var bricks = state.bricks
        var lives = state.lives
        var brickReachedBottom = false

        while (offset >= 1f) {
            offset -= 1f
            bricks = bricks.map { it.copy(row = it.row + 1) }
            val bottomRow = BrickField.ROWS
            if (bricks.any { it.row >= bottomRow }) {
                bricks = bricks.filter { it.row < bottomRow }
                lives = (lives - 1).coerceAtLeast(0)
                brickReachedBottom = true
                // Push remaining bricks up by half the field on life loss.
                if (lives > 0) {
                    val shift = BrickField.ROWS / 2
                    bricks = bricks.map { it.copy(row = (it.row - shift).coerceAtLeast(0)) }
                }
            }
        }
        return DescentResult(state.copy(descentOffset = offset, bricks = bricks, lives = lives), brickReachedBottom)
    }

    private fun addTopRow(state: BrickBreakerState, config: BrickBreakerConfig): BrickBreakerState {
        val level = state.level
        val maxHp = (level * 2).coerceAtMost(8)
        val newBricks = (0 until BrickField.COLS).mapNotNull { col ->
            if (Random.nextFloat() > 0.15f) {
                val hp = Random.nextInt(1, maxHp + 1)
                val r = Random.nextFloat()
                val type = when {
                    level >= 4 && r < 0.10f -> BrickType.STEEL
                    r < 0.05f -> BrickType.POWERUP
                    else -> BrickType.STANDARD
                }
                val pu = if (type == BrickType.POWERUP) PowerUpType.EXTRA_STRENGTH else null
                val steelHp = if (type == BrickType.STEEL) hp * 2 else hp
                Brick(col, 0, steelHp, steelHp, type, pu)
            } else null
        }
        // Shift existing bricks down by 1 conceptually via descentOffset — already handled.
        // For newly spawned rows, they appear at row 0 and the descent will move them.
        return state.copy(bricks = newBricks + state.bricks)
    }

    // -------------------------------------------------------------------------
    // Power-ups
    // -------------------------------------------------------------------------

    fun applyPowerUp(
        state: BrickBreakerState,
        config: BrickBreakerConfig,
        type: PowerUpType,
        x: Float,
        y: Float,
    ): BrickBreakerState {
        return when (type) {
            PowerUpType.EXPLODE -> {
                val r = 0.15f
                val destroyed = state.bricks.filter { brick ->
                    val brickCx = (brick.col + 0.5f) / BrickField.COLS.toFloat()
                    val brickCy = BrickField.TOP_MARGIN + brick.row * BrickField.ROW_HEIGHT + BrickField.ROW_HEIGHT / 2f
                    val dx = brickCx - x; val dy = brickCy - y
                    val inRange = dx * dx + dy * dy < r * r
                    inRange && brick.type != BrickType.STEEL
                }
                val explodeScore = destroyed.sumOf { brickScore(it, config) }
                state.copy(
                    bricks = state.bricks - destroyed.toSet(),
                    score = state.score + explodeScore,
                )
            }
            PowerUpType.CLEAR_SCREEN -> {
                val cleared = state.bricks.filter { it.type == BrickType.STANDARD || it.type == BrickType.POWERUP }
                val clearScore = cleared.size * config.clearScreenBonusPerBrick
                state.copy(bricks = state.bricks - cleared.toSet(), score = state.score + clearScore)
            }
            PowerUpType.MULTI_SHOT -> addOrRefreshPowerUp(state, type, config.multiShotDuration)
            PowerUpType.POWER_SHOT -> addOrRefreshPowerUp(state, type, config.powerShotDuration)
            PowerUpType.WIDE_SHOT -> addOrRefreshPowerUp(state, type, config.wideShotDuration)
            PowerUpType.RAPID_FIRE -> addOrRefreshPowerUp(state, type, config.rapidFireDuration)
            PowerUpType.TIME_BONUS -> state.copy(timerSeconds = state.timerSeconds + config.timeBonusSeconds)
            PowerUpType.EXTRA_BALL -> state.copy(ballCount = state.ballCount + 1)
            PowerUpType.EXTRA_STRENGTH -> state.copy(strength = state.strength + 1)
        }
    }

    /**
     * Apply a batch of instantly-collected power-ups (CLASSIC).  Only the
     * ball / strength bank power-ups have an effect here; anything else is
     * resolved through the normal drop path.
     */
    fun collectInstantly(state: BrickBreakerState, types: List<PowerUpType>): BrickBreakerState {
        if (types.isEmpty()) return state
        var ballCount = state.ballCount
        var strength = state.strength
        for (type in types) when (type) {
            PowerUpType.EXTRA_BALL -> ballCount += 1
            PowerUpType.EXTRA_STRENGTH -> strength += 1
            else -> {}
        }
        return state.copy(ballCount = ballCount, strength = strength)
    }

    /** Award the ball / strength power-ups still sitting in off-screen rows. */
    fun collectOffscreenPowerUps(state: BrickBreakerState): BrickBreakerState {
        val pending = state.bricks
            .filter { it.type == BrickType.POWERUP && it.powerUp != null }
            .mapNotNull { it.powerUp }
        return collectInstantly(state, pending)
    }

    private fun addOrRefreshPowerUp(
        state: BrickBreakerState,
        type: PowerUpType,
        duration: Float,
    ): BrickBreakerState {
        val existing = state.activePowerUps.firstOrNull { it.type == type }
        val updated = if (existing != null) {
            state.activePowerUps.map { if (it.type == type) it.copy(remainingSeconds = duration) else it }
        } else {
            if (state.activePowerUps.size >= 2) {
                state.activePowerUps.drop(1) + listOf(ActivePowerUp(type, duration))
            } else {
                state.activePowerUps + listOf(ActivePowerUp(type, duration))
            }
        }
        return state.copy(activePowerUps = updated)
    }

    private fun tickPowerUps(state: BrickBreakerState, dt: Float): BrickBreakerState {
        val active = state.activePowerUps
            .map { it.copy(remainingSeconds = it.remainingSeconds - dt) }
            .filter { it.remainingSeconds > 0f }
        return state.copy(activePowerUps = active)
    }

    private fun tickDroppingPowerUps(state: BrickBreakerState, dt: Float): BrickBreakerState {
        val updated = state.droppingPowerUps
            .map { it.copy(pos = it.pos + it.vel * dt, age = it.age + dt) }
            .filter { it.age < BrickField.POWERUP_MAX_AGE && it.pos.y < 1.1f }
        return state.copy(droppingPowerUps = updated)
    }

    // -------------------------------------------------------------------------
    // CLASSIC drop phase
    // -------------------------------------------------------------------------

    /** Move all bricks down one row and return whether any reached the bottom. */
    fun dropBricks(state: BrickBreakerState): Pair<BrickBreakerState, Boolean> {
        val shifted = state.bricks.map { it.copy(row = it.row + 1) }
        val atBottom = shifted.any { it.row >= BrickField.ROWS }
        val survivors = shifted.filter { it.row < BrickField.ROWS }
        return Pair(state.copy(bricks = survivors), atBottom)
    }

    // -------------------------------------------------------------------------
    // Win / loss queries
    // -------------------------------------------------------------------------

    fun allBricksGone(state: BrickBreakerState): Boolean = state.bricks.isEmpty()

    /** CLASSIC: true when no brick is currently on screen (row >= 0). */
    fun noVisibleBricks(state: BrickBreakerState): Boolean =
        state.bricks.none { it.row >= 0 }

    fun allTargetsDestroyed(state: BrickBreakerState): Boolean =
        state.bricks.none { it.type == BrickType.TARGET }

    fun allBallsLanded(state: BrickBreakerState): Boolean =
        state.balls.isEmpty() && state.ballsToFire <= 0

    fun targetBrickCount(state: BrickBreakerState): Int =
        state.bricks.count { it.type == BrickType.TARGET }

    /**
     * Whether any brick sits within one row of the bottom boundary — i.e. the
     * next descent would push it across the death line and end the game.
     *
     * [descentOffset] is the fractional real-time descent (ARCADE); pass 0 for
     * the turn-based CLASSIC variant where bricks sink one whole row per turn.
     */
    fun bricksNearBoundary(state: BrickBreakerState, descentOffset: Float = 0f): Boolean =
        state.bricks.any { it.row + descentOffset >= BrickField.ROWS - 1 }

    // -------------------------------------------------------------------------
    // Trajectory preview (for cannon variants)
    // -------------------------------------------------------------------------

    /**
     * Returns a list of normalized points tracing the ball's arc (for drawing a
     * dotted preview).  Stops after [maxBounces] wall contacts or leaving the field.
     */
    fun trajectoryPreview(config: BrickBreakerConfig, angleDeg: Float): List<Vec2> {
        val points = mutableListOf<Vec2>()
        val rad = Math.toRadians(angleDeg.toDouble().coerceIn(5.0, 85.0)).toFloat()
        var pos = Vec2(BrickField.CANNON_X, BrickField.GROUND_Y - BrickField.BALL_RADIUS)
        var vel = Vec2(cos(rad), -sin(rad)) * config.ballSpeed
        val dt = 0.015f
        repeat(300) {
            vel = Vec2(vel.x, vel.y + config.gravity * dt)
            pos += vel * dt
            if (pos.x < BrickField.BALL_RADIUS) { pos = Vec2(BrickField.BALL_RADIUS, pos.y); vel = Vec2(-vel.x, vel.y) }
            if (pos.x > 1f - BrickField.BALL_RADIUS) { pos = Vec2(1f - BrickField.BALL_RADIUS, pos.y); vel = Vec2(-vel.x, vel.y) }
            if (pos.y < BrickField.BALL_RADIUS) { pos = Vec2(pos.x, BrickField.BALL_RADIUS); vel = Vec2(vel.x, -vel.y) }
            points += pos
            if (pos.y > BrickField.GROUND_Y) return points
        }
        return points
    }

    /**
     * Returns the classic aim trajectory (straight line with first bounce).
     */
    fun classicTrajectoryPreview(paddleX: Float, angleDeg: Float): List<Vec2> {
        val points = mutableListOf<Vec2>()
        val rad = Math.toRadians(angleDeg.toDouble().coerceIn(10.0, 170.0)).toFloat()
        var pos = Vec2(paddleX, BrickField.CANNON_Y)
        var vel = Vec2(cos(rad), -sin(rad)).normalized() * 0.5f
        val dt = 0.02f
        repeat(80) {
            pos += vel * dt
            if (pos.x < 0.02f) { pos = Vec2(0.02f, pos.y); vel = Vec2(-vel.x, vel.y) }
            if (pos.x > 0.98f) { pos = Vec2(0.98f, pos.y); vel = Vec2(-vel.x, vel.y) }
            if (pos.y < 0.02f) { pos = Vec2(pos.x, 0.02f); vel = Vec2(vel.x, -vel.y) }
            points += pos
            if (pos.y > BrickField.CANNON_Y) return points
        }
        return points
    }

    // -------------------------------------------------------------------------
    // Scoring helpers
    // -------------------------------------------------------------------------

    fun levelCompleteBonus(state: BrickBreakerState, config: BrickBreakerConfig): Int =
        config.levelClearBonus * state.level

    fun turnsRemainingBonus(state: BrickBreakerState): Int =
        state.turnsLeft * 200

    fun timeRemainingBonus(state: BrickBreakerState): Int =
        state.timerSeconds.toInt() * 100
}
