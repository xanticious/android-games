package com.xanticious.androidgames.controller.games.lightup

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.lightup.LightUpCell
import com.xanticious.androidgames.model.games.lightup.LightUpState
import com.xanticious.androidgames.model.games.puzzle.Direction
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.step
import kotlin.random.Random

/**
 * Pure Light Up (Akari) rules: puzzle generation, bulb/mark placement,
 * illumination, conflict detection, and solved checking.
 *
 * All functions are free of Android or Compose imports; the whole rule set is
 * JVM unit-testable. Randomness is injected via [kotlin.random.Random] for
 * determinism.
 */
class LightUpController {

    /** Grid dimensions for each difficulty level (rows to cols). */
    fun sizeFor(difficulty: GameDifficulty): Pair<Int, Int> = when (difficulty) {
        GameDifficulty.EASY   -> 7  to 7
        GameDifficulty.MEDIUM -> 10 to 10
        GameDifficulty.HARD   -> 14 to 14
    }

    /**
     * Generates a fresh solvable puzzle for [difficulty].
     *
     * Algorithm:
     * 1. Randomly scatter walls (~15–25 % of cells, scaled by difficulty).
     * 2. Greedy bulb placement: any unlit white cell receives a bulb.
     *    Invariant: an unlit cell cannot be in the line-of-sight of any placed
     *    bulb, so placing one there is always conflict-free. This guarantees all
     *    white cells are illuminated after the pass completes.
     * 3. Derive wall clue numbers from adjacent solution bulbs; keep each number
     *    with a difficulty-scaled probability to tune how much information is given.
     * 4. Return the puzzle grid with cells cleared of bulbs; [LightUpState.solutionBulbs]
     *    records the full solution for hint delivery.
     */
    fun newGame(difficulty: GameDifficulty, random: Random = Random.Default): LightUpState {
        val (rows, cols) = sizeFor(difficulty)
        val total = rows * cols

        val wallFraction = when (difficulty) {
            GameDifficulty.EASY   -> 0.15
            GameDifficulty.MEDIUM -> 0.20
            GameDifficulty.HARD   -> 0.25
        }
        val wallCount = (total * wallFraction).toInt()

        // Step 1: Place walls at random positions.
        val isWall = BooleanArray(total)
        val shuffled = (0 until total).toMutableList().also { it.shuffle(random) }
        repeat(wallCount) { isWall[shuffled[it]] = true }

        // Step 2: Greedy bulb placement — always succeeds (see KDoc above).
        val lit = BooleanArray(total) { isWall[it] }
        val solutionBulbs = mutableSetOf<Int>()

        for (idx in 0 until total) {
            if (isWall[idx] || lit[idx]) continue
            solutionBulbs.add(idx)
            lit[idx] = true
            val row = idx / cols
            val col = idx % cols
            for (dir in Direction.entries) {
                var r = row + dir.dRow
                var c = col + dir.dCol
                while (r in 0 until rows && c in 0 until cols && !isWall[r * cols + c]) {
                    lit[r * cols + c] = true
                    r += dir.dRow
                    c += dir.dCol
                }
            }
        }

        // Step 3: Derive wall numbers; keep based on difficulty-scaled probability.
        val clueProbability = when (difficulty) {
            GameDifficulty.EASY   -> 0.70
            GameDifficulty.MEDIUM -> 0.50
            GameDifficulty.HARD   -> 0.30
        }

        val cells = Array(total) { idx ->
            if (!isWall[idx]) {
                LightUpCell(isWall = false)
            } else {
                val row = idx / cols
                val col = idx % cols
                val adjBulbs = Direction.entries.count { dir ->
                    val nr = row + dir.dRow
                    val nc = col + dir.dCol
                    nr in 0 until rows && nc in 0 until cols &&
                        solutionBulbs.contains(nr * cols + nc)
                }
                LightUpCell(
                    isWall = true,
                    wallNumber = if (random.nextDouble() < clueProbability) adjBulbs else null
                )
            }
        }

        return LightUpState(
            rows = rows,
            cols = cols,
            cells = cells.toList(),
            solutionBulbs = solutionBulbs
        )
    }

    /**
     * Toggles a bulb on [pos]:
     * - If [pos] already has a bulb, removes it.
     * - Otherwise places a bulb (clearing any existing mark).
     * - No-op on wall cells or out-of-bounds positions.
     */
    fun toggleBulb(state: LightUpState, pos: GridPos): LightUpState {
        if (!state.inBounds(pos)) return state
        val cell = state.cellAt(pos)
        if (cell.isWall) return state
        val updated = if (cell.hasBulb) cell.copy(hasBulb = false)
                      else cell.copy(hasBulb = true, hasMark = false)
        return state.withUpdate(pos, updated)
    }

    /**
     * Toggles an X-mark on [pos]:
     * - If [pos] has a bulb, replaces it with a mark.
     * - If [pos] has a mark, clears it.
     * - Otherwise places a mark.
     * - No-op on wall cells or out-of-bounds positions.
     */
    fun toggleMark(state: LightUpState, pos: GridPos): LightUpState {
        if (!state.inBounds(pos)) return state
        val cell = state.cellAt(pos)
        if (cell.isWall) return state
        val updated = when {
            cell.hasBulb -> cell.copy(hasBulb = false, hasMark = true)
            cell.hasMark -> cell.copy(hasMark = false)
            else         -> cell.copy(hasMark = true)
        }
        return state.withUpdate(pos, updated)
    }

    /**
     * Returns every white cell currently illuminated by at least one bulb,
     * including each bulb's own cell.
     */
    fun computeLit(state: LightUpState): Set<GridPos> {
        val lit = mutableSetOf<GridPos>()
        for (row in 0 until state.rows) {
            for (col in 0 until state.cols) {
                val pos = GridPos(row, col)
                if (!state.cellAt(pos).hasBulb) continue
                lit.add(pos)
                for (dir in Direction.entries) {
                    var cur = pos.step(dir)
                    while (state.inBounds(cur) && !state.cellAt(cur).isWall) {
                        lit.add(cur)
                        cur = cur.step(dir)
                    }
                }
            }
        }
        return lit
    }

    /**
     * Returns the set of conflicting cells:
     * - Pairs of bulbs that illuminate each other (both positions are included).
     * - Numbered wall cells whose actual adjacent-bulb count differs from
     *   [LightUpCell.wallNumber].
     */
    fun conflicts(state: LightUpState): Set<GridPos> {
        val conflicting = mutableSetOf<GridPos>()

        val bulbs: Set<GridPos> = state.cells.indices
            .asSequence()
            .filter { state.cells[it].hasBulb }
            .map { GridPos(it / state.cols, it % state.cols) }
            .toHashSet()

        // Bulb-sees-bulb: scan each beam from every bulb.
        for (bulb in bulbs) {
            for (dir in Direction.entries) {
                var cur = bulb.step(dir)
                while (state.inBounds(cur) && !state.cellAt(cur).isWall) {
                    if (cur in bulbs) {
                        conflicting += bulb
                        conflicting += cur
                    }
                    cur = cur.step(dir)
                }
            }
        }

        // Numbered wall constraint violations.
        for (row in 0 until state.rows) {
            for (col in 0 until state.cols) {
                val pos = GridPos(row, col)
                val cell = state.cellAt(pos)
                if (!cell.isWall || cell.wallNumber == null) continue
                val adjBulbs = Direction.entries.count { dir ->
                    val nb = pos.step(dir)
                    state.inBounds(nb) && state.cellAt(nb).hasBulb
                }
                if (adjBulbs != cell.wallNumber) conflicting += pos
            }
        }

        return conflicting
    }

    /**
     * Returns true when all three win conditions hold simultaneously:
     * 1. Every white cell is illuminated by at least one bulb.
     * 2. No two bulbs illuminate each other.
     * 3. Every numbered wall has exactly the required number of adjacent bulbs.
     */
    fun isSolved(state: LightUpState): Boolean {
        if (conflicts(state).isNotEmpty()) return false
        val lit = computeLit(state)
        return (0 until state.rows).all { row ->
            (0 until state.cols).all { col ->
                val pos = GridPos(row, col)
                state.cellAt(pos).isWall || pos in lit
            }
        }
    }

    /** Reverts the most recent bulb or mark change, if any history exists. */
    fun undo(state: LightUpState): LightUpState {
        val prev = state.history.lastOrNull() ?: return state
        return state.copy(cells = prev, history = state.history.dropLast(1))
    }

    /**
     * Places one bulb from [LightUpState.solutionBulbs] that has not yet been
     * placed by the player. No-op when all solution bulbs are already present.
     */
    fun hint(state: LightUpState): LightUpState {
        val idx = state.solutionBulbs.firstOrNull { !state.cells[it].hasBulb } ?: return state
        return toggleBulb(state, GridPos(idx / state.cols, idx % state.cols))
    }

    // ---- private helpers ----

    private fun LightUpState.withUpdate(pos: GridPos, newCell: LightUpCell): LightUpState {
        val idx = pos.row * cols + pos.col
        val updated = cells.toMutableList().also { it[idx] = newCell }
        return copy(cells = updated, history = history + listOf(cells))
    }
}
