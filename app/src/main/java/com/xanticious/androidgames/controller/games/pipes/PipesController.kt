package com.xanticious.androidgames.controller.games.pipes

import com.xanticious.androidgames.model.games.pipes.PipeCell
import com.xanticious.androidgames.model.games.pipes.PipeType
import com.xanticious.androidgames.model.games.pipes.PipesState
import com.xanticious.androidgames.model.games.puzzle.Direction
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.step
import kotlin.random.Random

/**
 * Pure Pipes rules: puzzle generation, rotation, connectivity and solve-check.
 * No Android or Compose imports — fully JVM unit-testable.
 *
 * Generation strategy: random Prim's spanning tree rooted at the source builds a
 * fully connected network; pipe types are inferred from per-cell degree; then
 * every cell is randomly rotated to create the playable puzzle.
 */
class PipesController {

    /** Board sizes offered in settings (Easy → Hard order). */
    val sizes: List<Int> = listOf(5, 8, 11, 14)

    // ── Rotation helpers ─────────────────────────────────────────────────────

    /** Rotates [dir] clockwise by [times] quarter-turns. */
    fun rotateCW(dir: Direction, times: Int): Direction {
        var d = dir
        repeat(times and 3) {
            d = when (d) {
                Direction.UP    -> Direction.RIGHT
                Direction.RIGHT -> Direction.DOWN
                Direction.DOWN  -> Direction.LEFT
                Direction.LEFT  -> Direction.UP
            }
        }
        return d
    }

    /**
     * Finds the rotation (0–3) that maps [type]'s canonical connectors to
     * [targetDirs]. Returns 0 if no exact match is found (should not happen for
     * well-formed inputs).
     */
    private fun rotationFor(type: PipeType, targetDirs: Set<Direction>): Int {
        for (r in 0..3) {
            if (type.baseConnectors.map { rotateCW(it, r) }.toSet() == targetDirs) return r
        }
        return 0
    }

    // ── Connector query ───────────────────────────────────────────────────────

    /** Returns the active connector directions for the cell at [pos] in [state]. */
    fun connectorsAt(state: PipesState, pos: GridPos): Set<Direction> {
        val idx = pos.row * state.size + pos.col
        val rotation = state.rotations[idx]
        return state.cells[idx].type.baseConnectors.map { rotateCW(it, rotation) }.toSet()
    }

    // ── Generation ────────────────────────────────────────────────────────────

    /**
     * Generates the solution configuration (all rotations correct) for a
     * [size]×[size] grid via a random Prim's spanning tree from the source at
     * the grid centre. The returned [PipesState] is fully solved.
     */
    fun generateSolution(size: Int, random: Random = Random.Default): PipesState {
        val source = GridPos(size / 2, size / 2)
        val connections = Array(size) { Array(size) { mutableSetOf<Direction>() } }

        // Random Prim's: grow a spanning tree from source.
        val inTree = mutableSetOf(source)
        val frontier = mutableListOf<GridPos>()
        Direction.entries.forEach { dir ->
            val nb = source.step(dir)
            if (nb.row in 0 until size && nb.col in 0 until size) frontier.add(nb)
        }

        while (frontier.isNotEmpty()) {
            val fi = random.nextInt(frontier.size)
            val cell = frontier.removeAt(fi)
            if (cell in inTree) continue

            val treeNeighborDirs = Direction.entries.filter { dir ->
                val nb = cell.step(dir)
                nb.row in 0 until size && nb.col in 0 until size && nb in inTree
            }
            val connectDir = treeNeighborDirs[random.nextInt(treeNeighborDirs.size)]
            val connectTo = cell.step(connectDir)

            connections[cell.row][cell.col].add(connectDir)
            connections[connectTo.row][connectTo.col].add(connectDir.opposite)
            inTree.add(cell)

            Direction.entries.forEach { dir ->
                val nb = cell.step(dir)
                if (nb.row in 0 until size && nb.col in 0 until size && nb !in inTree) {
                    frontier.add(nb)
                }
            }
        }

        // Derive pipe type and canonical rotation from each cell's connections.
        val cells = mutableListOf<PipeCell>()
        val rotations = mutableListOf<Int>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                val dirs = connections[r][c]
                val type = pipeTypeFor(dirs)
                cells.add(PipeCell(type))
                rotations.add(rotationFor(type, dirs))
            }
        }

        return PipesState(
            size = size,
            cells = cells,
            rotations = rotations,
            initialRotations = rotations,
            sourcePos = source
        )
    }

    /** Infers [PipeType] from the number (and layout) of connectors. */
    private fun pipeTypeFor(dirs: Set<Direction>): PipeType = when (dirs.size) {
        1    -> PipeType.END
        2    -> if (dirs.any { it.opposite in dirs }) PipeType.LINE else PipeType.ELBOW
        3    -> PipeType.TEE
        else -> PipeType.CROSS
    }

    /**
     * Deals a new playable puzzle: generates the solution then randomly shuffles
     * every cell's rotation. The same [random] seed always produces the same
     * puzzle (generation + shuffle interleaved from a single RNG).
     */
    fun newGame(size: Int, random: Random = Random.Default): PipesState {
        val solution = generateSolution(size, random)
        val shuffled = solution.rotations.map { (it + random.nextInt(4)) % 4 }
        return solution.copy(rotations = shuffled, initialRotations = shuffled, rotationCount = 0)
    }

    // ── Gameplay actions ──────────────────────────────────────────────────────

    /**
     * Rotates the cell at [pos] by one quarter-turn (clockwise by default,
     * counter-clockwise if [clockwise] is false). Pushes current rotations onto
     * the undo history.
     */
    fun rotate(state: PipesState, pos: GridPos, clockwise: Boolean = true): PipesState {
        val idx = pos.row * state.size + pos.col
        val delta = if (clockwise) 1 else 3
        val newRotations = state.rotations.toMutableList()
        newRotations[idx] = (newRotations[idx] + delta) % 4
        return state.copy(
            rotations = newRotations,
            rotationCount = state.rotationCount + 1,
            history = state.history + listOf(state.rotations)
        )
    }

    /** Reverts the last rotation. Returns [state] unchanged if history is empty. */
    fun undo(state: PipesState): PipesState {
        val previous = state.history.lastOrNull() ?: return state
        return state.copy(
            rotations = previous,
            rotationCount = state.rotationCount + 1,
            history = state.history.dropLast(1)
        )
    }

    /** Resets all rotations to the initial shuffled state, clearing history. */
    fun reset(state: PipesState): PipesState =
        state.copy(rotations = state.initialRotations, rotationCount = 0, history = emptyList())

    // ── Connectivity & solve-check ────────────────────────────────────────────

    /**
     * BFS from [PipesState.sourcePos] following only mutually-connected edges.
     * Two adjacent cells are connected when each has a connector pointing toward
     * the other.
     */
    fun connectedCells(state: PipesState): Set<GridPos> {
        val visited = mutableSetOf<GridPos>()
        val queue = ArrayDeque<GridPos>()
        queue.add(state.sourcePos)
        visited.add(state.sourcePos)
        while (queue.isNotEmpty()) {
            val pos = queue.removeFirst()
            for (dir in connectorsAt(state, pos)) {
                val nb = pos.step(dir)
                if (nb.row !in 0 until state.size || nb.col !in 0 until state.size) continue
                if (nb in visited) continue
                if (dir.opposite in connectorsAt(state, nb)) {
                    visited.add(nb)
                    queue.add(nb)
                }
            }
        }
        return visited
    }

    /**
     * Returns true when:
     * 1. No connector points off-grid or faces a non-matching neighbor (no leaks).
     * 2. Every cell is reachable from the source (fully connected network).
     */
    fun isSolved(state: PipesState): Boolean {
        for (r in 0 until state.size) {
            for (c in 0 until state.size) {
                val pos = GridPos(r, c)
                for (dir in connectorsAt(state, pos)) {
                    val nb = pos.step(dir)
                    if (nb.row !in 0 until state.size || nb.col !in 0 until state.size) return false
                    if (dir.opposite !in connectorsAt(state, nb)) return false
                }
            }
        }
        return connectedCells(state).size == state.size * state.size
    }
}
