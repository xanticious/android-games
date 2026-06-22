package com.xanticious.androidgames.model.games.qix

/** Playfield dimensions in cells; the outer ring is always [CellState.BOUNDARY]. */
const val QIX_COLS = 40
const val QIX_ROWS = 30

/** State of a single playfield cell. */
enum class CellState { BOUNDARY, CLAIMED, UNCLAIMED, TRAIL }

/** Whether the player is safely on the boundary / claimed edge or actively drawing. */
enum class PlayerMode { ON_BOUNDARY, DRAWING }

/** Draw speed toggles risk vs. reward: fast draw scores more but is riskier. */
enum class DrawSpeed { SLOW, FAST }

/**
 * Integer grid coordinate on the [QIX_COLS] × [QIX_ROWS] playfield.
 * The outer ring (col 0, col [QIX_COLS]-1, row 0, row [QIX_ROWS]-1) contains
 * [CellState.BOUNDARY] cells; interior cells start from (1, 1).
 */
data class GridPos(val col: Int, val row: Int) {
    fun isValid(): Boolean = col in 0 until QIX_COLS && row in 0 until QIX_ROWS
}

/**
 * The Qix enemy: a single entity that bounces through unclaimed space.
 * Position ([x], [y]) is in floating-point cell coordinates.
 * ([dirX], [dirY]) is a unit direction vector.
 */
data class QixEntity(val x: Float, val y: Float, val dirX: Float, val dirY: Float)

/**
 * A Sparx patrol enemy that travels the fixed outer boundary.
 * [perimeterIndex] is a float offset into the clockwise perimeter list (wraps at
 * [QixState.PERIMETER_SIZE]). [direction] is +1 (clockwise) or -1 (counter-clockwise).
 */
data class SparxEntity(val perimeterIndex: Float, val direction: Int)

/**
 * Full immutable snapshot of a Qix game frame.
 *
 * [cells] is a flattened row-major [QIX_COLS] × [QIX_ROWS] list:
 * `index = row * QIX_COLS + col`.
 *
 * [claimedCount] is derived from [cells] and reflects only interior
 * [CellState.CLAIMED] cells (boundary cells are not counted).
 */
data class QixState(
    val cells: List<CellState>,
    val trail: List<GridPos>,
    val playerPos: GridPos,
    val playerMode: PlayerMode,
    val drawSpeed: DrawSpeed,
    val qix: List<QixEntity>,
    val sparx: List<SparxEntity>,
    val lives: Int,
    val score: Int,
    val invincibleTimer: Float,
    val moveTimer: Float,
    val level: Int
) {
    /** Number of interior cells currently marked [CellState.CLAIMED]. */
    val claimedCount: Int get() = cells.count { it == CellState.CLAIMED }

    /** Returns the cell state at [pos]; safely returns [CellState.BOUNDARY] out-of-bounds. */
    fun cellAt(pos: GridPos): CellState {
        if (!pos.isValid()) return CellState.BOUNDARY
        return cells[pos.row * QIX_COLS + pos.col]
    }

    companion object {
        /** Total interior (non-boundary) cells across the whole playfield. */
        val TOTAL_INNER_CELLS: Int = (QIX_COLS - 2) * (QIX_ROWS - 2)

        /**
         * Total cells in the fixed outer clockwise perimeter.
         * Formula: top-row + right-col + bottom-row + left-col (corners counted once).
         */
        val PERIMETER_SIZE: Int = 2 * (QIX_COLS - 1) + 2 * (QIX_ROWS - 1)

        /** Builds an initial game frame for the given [level]. */
        fun initial(level: Int = 1): QixState {
            val cells = List(QIX_COLS * QIX_ROWS) { i ->
                val col = i % QIX_COLS
                val row = i / QIX_COLS
                if (col == 0 || col == QIX_COLS - 1 || row == 0 || row == QIX_ROWS - 1)
                    CellState.BOUNDARY
                else
                    CellState.UNCLAIMED
            }
            // One Qix starting in the centre with a unit-length direction.
            val qix = listOf(
                QixEntity(x = QIX_COLS / 2f, y = QIX_ROWS / 2f, dirX = 0.8f, dirY = 0.6f)
            )
            // Two Sparx starting at opposite corners, moving in opposite directions.
            val sparx = listOf(
                SparxEntity(perimeterIndex = 0f, direction = 1),
                SparxEntity(perimeterIndex = PERIMETER_SIZE / 2f, direction = -1)
            )
            return QixState(
                cells = cells,
                trail = emptyList(),
                playerPos = GridPos(QIX_COLS / 2, 0),
                playerMode = PlayerMode.ON_BOUNDARY,
                drawSpeed = DrawSpeed.SLOW,
                qix = qix,
                sparx = sparx,
                lives = 3,
                score = 0,
                invincibleTimer = 0f,
                moveTimer = 0f,
                level = level
            )
        }
    }
}

/** Tuning values derived from the selected difficulty. */
data class QixConfig(
    val playerSpeed: Float,
    val qixSpeed: Float,
    val sparxSpeed: Float,
    val targetClaimedPercent: Int = 75,
    val invincibilityDuration: Float = 2f
)

/** Per-frame player input fed to [com.xanticious.androidgames.controller.games.qix.QixController]. */
data class QixInput(
    val dx: Float,
    val dy: Float,
    val drawSpeed: DrawSpeed = DrawSpeed.SLOW
)

/** Game-level event produced by one step of the controller. */
sealed interface QixEvent {
    data object None : QixEvent
    data object LifeLost : QixEvent
    data object TerritoryClaimedContinue : QixEvent
    data object TerritoryClaimedLevelComplete : QixEvent
}

/** Paired (new state, event) returned by one game step. */
data class QixStep(val state: QixState, val event: QixEvent)
