package com.xanticious.androidgames.model.games.pipes

import com.xanticious.androidgames.model.games.puzzle.Direction
import com.xanticious.androidgames.model.games.puzzle.GridPos

/**
 * Pipe piece shapes. Each type defines its connectors in canonical orientation
 * (rotation = 0); rotating clockwise shifts every connector 90° CW.
 *
 * END   (1 connector): dead-end cap, points UP at rot=0.
 * LINE  (2, straight): straight pipe, UP↔DOWN at rot=0.
 * ELBOW (2, bent):     L-shaped bend, UP+RIGHT at rot=0.
 * TEE   (3):           T-junction, missing DOWN at rot=0.
 * CROSS (4):           all four sides; rotation has no visual effect.
 */
enum class PipeType {
    END, LINE, ELBOW, TEE, CROSS;

    /** Active connector directions at rotation = 0 (canonical orientation). */
    val baseConnectors: Set<Direction>
        get() = when (this) {
            END   -> setOf(Direction.UP)
            LINE  -> setOf(Direction.UP, Direction.DOWN)
            ELBOW -> setOf(Direction.UP, Direction.RIGHT)
            TEE   -> setOf(Direction.UP, Direction.LEFT, Direction.RIGHT)
            CROSS -> setOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)
        }
}

/** Immutable description of a single cell — just its pipe shape. */
data class PipeCell(val type: PipeType)

/**
 * Full state of a Pipes puzzle (`design/puzzle-games/pipes`).
 *
 * @property cells         Pipe shapes in row-major order; never changes after creation.
 * @property rotations     Current clockwise quarter-turn count (0–3) for each cell.
 * @property initialRotations  The shuffled starting rotations, used by reset.
 * @property sourcePos     The cell that acts as the network source.
 * @property rotationCount Running count of rotations performed (for the HUD).
 * @property history       Undo stack: each entry is a full snapshot of [rotations].
 */
data class PipesState(
    val size: Int,
    val cells: List<PipeCell>,
    val rotations: List<Int>,
    val initialRotations: List<Int>,
    val sourcePos: GridPos,
    val rotationCount: Int = 0,
    val history: List<List<Int>> = emptyList()
) {
    val canUndo: Boolean get() = history.isNotEmpty()
}
