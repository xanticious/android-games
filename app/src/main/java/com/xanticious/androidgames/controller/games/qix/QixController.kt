package com.xanticious.androidgames.controller.games.qix

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.qix.CellState
import com.xanticious.androidgames.model.games.qix.DrawSpeed
import com.xanticious.androidgames.model.games.qix.GridPos
import com.xanticious.androidgames.model.games.qix.PlayerMode
import com.xanticious.androidgames.model.games.qix.QIX_COLS
import com.xanticious.androidgames.model.games.qix.QIX_ROWS
import com.xanticious.androidgames.model.games.qix.QixConfig
import com.xanticious.androidgames.model.games.qix.QixEvent
import com.xanticious.androidgames.model.games.qix.QixInput
import com.xanticious.androidgames.model.games.qix.QixState
import com.xanticious.androidgames.model.games.qix.QixStep
import com.xanticious.androidgames.model.games.qix.SparxEntity
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pure Qix game rules: grid-based player movement, trail drawing, territory
 * claiming via BFS flood-fill, Qix bouncing, Sparx patrol and all collision
 * detection.  No Android or Compose imports — fully JVM unit-testable.
 */
class QixController {

    /**
     * Clockwise outer perimeter list:
     *   top row (left→right) → right col (top→bottom) →
     *   bottom row (right→left) → left col (bottom→top).
     * Computed lazily once.
     */
    private val perimeter: List<GridPos> by lazy {
        buildList {
            for (c in 0 until QIX_COLS) add(GridPos(c, 0))
            for (r in 1 until QIX_ROWS) add(GridPos(QIX_COLS - 1, r))
            for (c in QIX_COLS - 2 downTo 0) add(GridPos(c, QIX_ROWS - 1))
            for (r in QIX_ROWS - 2 downTo 1) add(GridPos(0, r))
        }
    }

    /** Reverse lookup: GridPos on the boundary → its index in [perimeter]. */
    private val perimeterIndexOf: Map<GridPos, Int> by lazy {
        perimeter.withIndex().associate { (i, pos) -> pos to i }
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    fun configFor(difficulty: GameDifficulty): QixConfig = when (difficulty) {
        GameDifficulty.EASY -> QixConfig(
            playerSpeed = 8f, qixSpeed = 3f, sparxSpeed = 3f
        )
        GameDifficulty.MEDIUM -> QixConfig(
            playerSpeed = 8f, qixSpeed = 5f, sparxSpeed = 5f
        )
        GameDifficulty.HARD -> QixConfig(
            playerSpeed = 8f, qixSpeed = 8.5f, sparxSpeed = 8f
        )
    }

    /**
     * Advances the game by [dt] seconds given player [input].
     * Returns the updated [QixState] plus any [QixEvent] that occurred this frame.
     *
     * Order of operations:
     * 1. Decay invincibility timer.
     * 2. Move the player (possibly triggering a territory claim, which returns early).
     * 3. Move the Qix within unclaimed space.
     * 4. Move Sparx along the outer perimeter.
     * 5. Check collisions (skipped while invincible).
     */
    fun step(state: QixState, config: QixConfig, dt: Float, input: QixInput): QixStep {
        if (state.lives <= 0) return QixStep(state, QixEvent.None)

        var s = state.copy(
            invincibleTimer = (state.invincibleTimer - dt).coerceAtLeast(0f),
            drawSpeed = input.drawSpeed
        )

        val (afterPlayer, playerEvent) = movePlayer(s, config, dt, input)
        s = afterPlayer
        if (playerEvent != QixEvent.None) {
            return QixStep(s, playerEvent)
        }

        s = moveQix(s, config, dt)
        s = moveSparx(s, config, dt)

        if (s.invincibleTimer <= 0f && hasCollision(s)) {
            return QixStep(handleLifeLost(s, config), QixEvent.LifeLost)
        }

        return QixStep(s, QixEvent.None)
    }

    /** Percentage of interior cells claimed (0–100). */
    fun claimedPercent(state: QixState): Int =
        state.claimedCount * 100 / QixState.TOTAL_INNER_CELLS

    /**
     * Returns the [GridPos] on the outer perimeter for the given float offset.
     * The view uses this to draw Sparx at the correct cell.
     */
    fun perimeterCellAt(index: Float): GridPos {
        val size = perimeter.size
        val i = ((index.toInt() % size) + size) % size
        return perimeter[i]
    }

    // ─── Player movement ─────────────────────────────────────────────────────

    /**
     * Advances the player by one cell if the move timer has elapsed.
     * Starting to draw or reconnecting to the boundary are handled here.
     */
    private fun movePlayer(
        state: QixState,
        config: QixConfig,
        dt: Float,
        input: QixInput
    ): Pair<QixState, QixEvent> {
        val moveInterval = 1f / config.playerSpeed
        var s = state.copy(moveTimer = state.moveTimer + dt)
        if (s.moveTimer < moveInterval) return Pair(s, QixEvent.None)

        s = s.copy(moveTimer = s.moveTimer - moveInterval)

        val dir = snapToDir(input.dx, input.dy) ?: return Pair(s, QixEvent.None)
        val target = GridPos(s.playerPos.col + dir.first, s.playerPos.row + dir.second)
        if (!target.isValid()) return Pair(s, QixEvent.None)

        return when (s.playerMode) {
            PlayerMode.ON_BOUNDARY -> moveFromBoundary(s, target, config)
            PlayerMode.DRAWING -> moveFromDrawing(s, target, config)
        }
    }

    /** Player on boundary/claimed territory tries to move to [target]. */
    private fun moveFromBoundary(
        state: QixState,
        target: GridPos,
        config: QixConfig
    ): Pair<QixState, QixEvent> = when (state.cellAt(target)) {
        CellState.BOUNDARY, CellState.CLAIMED ->
            Pair(state.copy(playerPos = target), QixEvent.None)

        CellState.UNCLAIMED -> {
            // Step off boundary — start drawing.
            val newCells = state.cells.toMutableList()
                .also { it[target.row * QIX_COLS + target.col] = CellState.TRAIL }
            Pair(
                state.copy(
                    playerPos = target,
                    playerMode = PlayerMode.DRAWING,
                    trail = listOf(target),
                    cells = newCells
                ),
                QixEvent.None
            )
        }

        CellState.TRAIL -> Pair(state, QixEvent.None)
    }

    /** Player in drawing mode tries to move to [target]. */
    private fun moveFromDrawing(
        state: QixState,
        target: GridPos,
        config: QixConfig
    ): Pair<QixState, QixEvent> = when (state.cellAt(target)) {
        CellState.BOUNDARY, CellState.CLAIMED ->
            // Reconnect to the boundary — claim enclosed territory.
            claimTerritory(state.copy(playerPos = target), config)

        CellState.UNCLAIMED -> {
            val newCells = state.cells.toMutableList()
                .also { it[target.row * QIX_COLS + target.col] = CellState.TRAIL }
            Pair(
                state.copy(
                    playerPos = target,
                    trail = state.trail + target,
                    cells = newCells
                ),
                QixEvent.None
            )
        }

        CellState.TRAIL -> Pair(state, QixEvent.None)  // Can't re-enter own trail.
    }

    // ─── Territory claiming ───────────────────────────────────────────────────

    /**
     * Claims the region that does NOT contain any Qix.
     *
     * Algorithm:
     * 1. BFS from each Qix's cell through [CellState.UNCLAIMED] cells only
     *    (the active [CellState.TRAIL] acts as a wall).
     * 2. All [CellState.UNCLAIMED] cells not reachable from any Qix become
     *    [CellState.CLAIMED]; all [CellState.TRAIL] cells become [CellState.CLAIMED].
     * 3. If a Qix was completely enclosed by the trail (bonus), claim everything.
     */
    private fun claimTerritory(state: QixState, config: QixConfig): Pair<QixState, QixEvent> {
        val prevClaimed = state.claimedCount

        val qixSide: Set<GridPos> = state.qix.fold(mutableSetOf()) { acc, q ->
            acc.also { it.addAll(floodFillFromPoint(state.cells, q.x, q.y)) }
        }

        val newCells = state.cells.mapIndexed { idx, cell ->
            val col = idx % QIX_COLS
            val row = idx / QIX_COLS
            when {
                cell == CellState.TRAIL -> CellState.CLAIMED
                cell == CellState.UNCLAIMED && GridPos(col, row) !in qixSide -> CellState.CLAIMED
                else -> cell
            }
        }

        val claimed = state.copy(
            cells = newCells,
            trail = emptyList(),
            playerMode = PlayerMode.ON_BOUNDARY
        )

        val justClaimed = claimed.claimedCount - prevClaimed
        val pct = justClaimed * 100 / QixState.TOTAL_INNER_CELLS
        val multiplier = if (state.drawSpeed == DrawSpeed.FAST) 20 else 10
        val bonus = if (qixSide.isEmpty() && state.qix.isNotEmpty()) 1000 else 0
        val finalState = claimed.copy(score = state.score + pct * multiplier + bonus)

        val event = if (claimedPercent(finalState) >= config.targetClaimedPercent)
            QixEvent.TerritoryClaimedLevelComplete
        else
            QixEvent.TerritoryClaimedContinue

        return Pair(finalState, event)
    }

    /**
     * BFS through [CellState.UNCLAIMED] cells starting from (floor([startX]), floor([startY])).
     * [CellState.TRAIL], [CellState.CLAIMED], and [CellState.BOUNDARY] cells are all walls.
     * Returns an empty set if the start cell is not [CellState.UNCLAIMED] (Qix trapped).
     */
    private fun floodFillFromPoint(
        cells: List<CellState>,
        startX: Float,
        startY: Float
    ): Set<GridPos> {
        val startCol = startX.toInt().coerceIn(1, QIX_COLS - 2)
        val startRow = startY.toInt().coerceIn(1, QIX_ROWS - 2)
        if (cells[startRow * QIX_COLS + startCol] != CellState.UNCLAIMED) return emptySet()

        val visited = mutableSetOf(GridPos(startCol, startRow))
        val queue = ArrayDeque<GridPos>()
        queue.add(GridPos(startCol, startRow))

        while (queue.isNotEmpty()) {
            val pos = queue.removeFirst()
            for ((dc, dr) in DIRS) {
                val nc = pos.col + dc
                val nr = pos.row + dr
                if (nc !in 0 until QIX_COLS || nr !in 0 until QIX_ROWS) continue
                val neighbor = GridPos(nc, nr)
                if (neighbor !in visited && cells[nr * QIX_COLS + nc] == CellState.UNCLAIMED) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }
        return visited
    }

    // ─── Qix movement ────────────────────────────────────────────────────────

    /**
     * Moves each Qix at [QixConfig.qixSpeed] cells per second, bouncing off any
     * cell that is not [CellState.UNCLAIMED]. X and Y axes are resolved independently
     * so corner bounces work correctly.
     */
    private fun moveQix(state: QixState, config: QixConfig, dt: Float): QixState {
        val newQix = state.qix.map { qix ->
            var dirX = qix.dirX
            var dirY = qix.dirY
            // Keep direction unit-length.
            val len = sqrt(dirX * dirX + dirY * dirY)
            if (len > 1e-6f) { dirX /= len; dirY /= len }

            var x = qix.x
            var y = qix.y
            val spd = config.qixSpeed

            // --- X axis ---
            val newX = x + dirX * spd * dt
            val newXCell = newX.toInt().coerceIn(0, QIX_COLS - 1)
            val curYCell = y.toInt().coerceIn(0, QIX_ROWS - 1)
            if (state.cells[curYCell * QIX_COLS + newXCell] == CellState.UNCLAIMED) {
                x = newX.coerceIn(1f, (QIX_COLS - 2).toFloat())
            } else {
                dirX = -dirX
            }

            // --- Y axis ---
            val curXCell = x.toInt().coerceIn(0, QIX_COLS - 1)
            val newY = y + dirY * spd * dt
            val newYCell = newY.toInt().coerceIn(0, QIX_ROWS - 1)
            if (state.cells[newYCell * QIX_COLS + curXCell] == CellState.UNCLAIMED) {
                y = newY.coerceIn(1f, (QIX_ROWS - 2).toFloat())
            } else {
                dirY = -dirY
            }

            qix.copy(x = x, y = y, dirX = dirX, dirY = dirY)
        }
        return state.copy(qix = newQix)
    }

    // ─── Sparx patrol ────────────────────────────────────────────────────────

    private fun moveSparx(state: QixState, config: QixConfig, dt: Float): QixState {
        val perimSize = perimeter.size.toFloat()
        val newSparx = state.sparx.map { sparx ->
            var idx = sparx.perimeterIndex + sparx.direction * config.sparxSpeed * dt
            idx = ((idx % perimSize) + perimSize) % perimSize
            sparx.copy(perimeterIndex = idx)
        }
        return state.copy(sparx = newSparx)
    }

    // ─── Collision detection ──────────────────────────────────────────────────

    /**
     * Returns true if:
     * - any Qix occupies an active [CellState.TRAIL] cell, or
     * - any Sparx is within 1 perimeter step of the player while the player
     *   is standing on a [CellState.BOUNDARY] cell.
     */
    private fun hasCollision(state: QixState): Boolean {
        // Qix touches active trail.
        for (qix in state.qix) {
            val col = qix.x.toInt().coerceIn(0, QIX_COLS - 1)
            val row = qix.y.toInt().coerceIn(0, QIX_ROWS - 1)
            if (state.cellAt(GridPos(col, row)) == CellState.TRAIL) return true
        }
        // Sparx reaches player on the outer boundary ring.
        if (state.playerMode == PlayerMode.ON_BOUNDARY &&
            state.cellAt(state.playerPos) == CellState.BOUNDARY
        ) {
            val playerIdx = perimeterIndexOf[state.playerPos]?.toFloat() ?: return false
            val perimSize = perimeter.size.toFloat()
            for (sparx in state.sparx) {
                val diff = abs(sparx.perimeterIndex - playerIdx)
                if (minOf(diff, perimSize - diff) < 1.0f) return true
            }
        }
        return false
    }

    /**
     * Decrements lives, clears the active trail, and respawns the player at the
     * top boundary mid-point with invincibility active.
     */
    private fun handleLifeLost(state: QixState, config: QixConfig): QixState {
        val newCells = state.cells.map {
            if (it == CellState.TRAIL) CellState.UNCLAIMED else it
        }
        return state.copy(
            cells = newCells,
            trail = emptyList(),
            playerPos = GridPos(QIX_COLS / 2, 0),
            playerMode = PlayerMode.ON_BOUNDARY,
            lives = (state.lives - 1).coerceAtLeast(0),
            invincibleTimer = config.invincibilityDuration,
            moveTimer = 0f
        )
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Snaps analog joystick axes to the dominant 4-way direction.
     * Returns null when both axes are inside the dead zone.
     */
    private fun snapToDir(dx: Float, dy: Float): Pair<Int, Int>? {
        if (abs(dx) < 0.25f && abs(dy) < 0.25f) return null
        return if (abs(dx) >= abs(dy)) {
            if (dx > 0f) 1 to 0 else -1 to 0
        } else {
            if (dy > 0f) 0 to 1 else 0 to -1
        }
    }

    companion object {
        private val DIRS = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
    }
}
