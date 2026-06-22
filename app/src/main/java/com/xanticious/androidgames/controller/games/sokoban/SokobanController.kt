package com.xanticious.androidgames.controller.games.sokoban

import com.xanticious.androidgames.model.games.puzzle.Direction
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.step
import com.xanticious.androidgames.model.games.sokoban.SokobanCell
import com.xanticious.androidgames.model.games.sokoban.SokobanLevel
import com.xanticious.androidgames.model.games.sokoban.SokobanSnapshot
import com.xanticious.androidgames.model.games.sokoban.SokobanState

/**
 * Pure Sokoban rules: level parsing, movement, undo, and the solved check.
 * No Android or Compose imports — the entire rule set is JVM unit-testable.
 *
 * All six levels are hand-authored and guaranteed solvable. Each level string
 * uses standard Sokoban notation:
 *   '#' = wall   ' ' = floor   '.' = goal
 *   '$' = box    '*' = box on goal
 *   '@' = player '+' = player on goal
 */
class SokobanController {

    /** All levels in order; the first two are Starter, next two Classic, last two Expert. */
    val allLevels: List<SokobanLevel> = RAW_LEVELS.map { parseLevel(it) }

    // ── Level lifecycle ──────────────────────────────────────────────────────

    /** Builds the initial [SokobanState] for [level] at [levelIndex] in its set. */
    fun startLevel(level: SokobanLevel, levelIndex: Int): SokobanState = SokobanState(
        level = level,
        levelIndex = levelIndex,
        player = level.initialPlayer,
        boxes = level.initialBoxes
    )

    // ── Core rules ───────────────────────────────────────────────────────────

    /**
     * Attempts to move the player one step in [direction]:
     * - If the target cell is a wall → no-op.
     * - If the target cell holds a box, tries to push the box one further cell
     *   in the same direction; blocked if the beyond-cell is a wall or another box.
     * - Otherwise steps the player onto the target cell.
     *
     * Every successful move saves a [SokobanSnapshot] for undo.
     */
    fun move(state: SokobanState, direction: Direction): SokobanState {
        val target = state.player.step(direction)
        val targetCell = state.cell(target)
        if (targetCell == SokobanCell.WALL) return state

        val hasBox = target in state.boxes
        if (hasBox) {
            val beyond = target.step(direction)
            val beyondCell = state.cell(beyond)
            if (beyondCell == SokobanCell.WALL || beyond in state.boxes) return state

            val snapshot = SokobanSnapshot(state.player, state.boxes, state.moves, state.pushes)
            return state.copy(
                player = target,
                boxes = state.boxes - target + beyond,
                moves = state.moves + 1,
                pushes = state.pushes + 1,
                history = state.history + snapshot
            )
        }

        val snapshot = SokobanSnapshot(state.player, state.boxes, state.moves, state.pushes)
        return state.copy(
            player = target,
            moves = state.moves + 1,
            history = state.history + snapshot
        )
    }

    /**
     * Reverts the most recent move or push, fully restoring player position,
     * box positions, and the move/push counters. No-op if there is no history.
     */
    fun undo(state: SokobanState): SokobanState {
        val snap = state.history.lastOrNull() ?: return state
        return state.copy(
            player = snap.player,
            boxes = snap.boxes,
            moves = snap.moves,
            pushes = snap.pushes,
            usedUndo = true,
            history = state.history.dropLast(1)
        )
    }

    /** True when every goal cell has a box on it — the win condition. */
    fun isSolved(state: SokobanState): Boolean =
        state.level.goals.all { it in state.boxes }

    /** Number of boxes currently sitting on a goal. Used for the HUD progress display. */
    fun boxesOnGoals(state: SokobanState): Int =
        state.boxes.count { it in state.level.goals }

    // ── Level parser ─────────────────────────────────────────────────────────

    /**
     * Parses a multi-line ASCII Sokoban map string into an immutable [SokobanLevel].
     * Each row is padded to the maximum line width so the grid is rectangular.
     */
    fun parseLevel(ascii: String): SokobanLevel {
        val lines = ascii.trimIndent().lines()
        val cols = lines.maxOf { it.length }
        val rows = lines.size
        val cells = mutableListOf<SokobanCell>()
        val goals = mutableSetOf<GridPos>()
        val boxes = mutableSetOf<GridPos>()
        var player = GridPos(0, 0)

        for ((r, line) in lines.withIndex()) {
            val padded = line.padEnd(cols, ' ')
            for ((c, ch) in padded.withIndex()) {
                val pos = GridPos(r, c)
                // Determine the underlying cell type.
                when (ch) {
                    '#' -> cells += SokobanCell.WALL
                    '.', '+', '*' -> { cells += SokobanCell.GOAL; goals += pos }
                    else -> cells += SokobanCell.FLOOR
                }
                // Extract mutable overlay objects.
                when (ch) {
                    '@', '+' -> player = pos
                    '$', '*' -> boxes += pos
                }
            }
        }
        return SokobanLevel(rows, cols, cells, goals, player, boxes)
    }
}

// ── Hand-authored level data ──────────────────────────────────────────────────

/**
 * Six hand-authored, guaranteed-solvable levels in ascending difficulty.
 *
 * Levels 0–1  Starter  (EASY)   — 1 box, trivial to moderate navigation.
 * Levels 2–3  Classic  (MEDIUM) — 2 boxes, moderate navigation required.
 * Levels 4–5  Expert   (HARD)   — 3 boxes, planning required.
 *
 * Solutions (Direction sequences, verified manually):
 *   L0: R
 *   L1: R R D
 *   L2: L U D R U
 *   L3: R L L D L U
 *   L4: L U D R R U D R R U
 *   L5: U U U R U L D D R R D R U U
 */
private val RAW_LEVELS = listOf(
    // Level 0 — Starter 1: push the box right onto the goal (1 move).
    """
    #####
    #@$.#
    #####
    """,

    // Level 1 — Starter 2: navigate above the box then push it down (3 moves).
    """
    #######
    #@    #
    #  $  #
    #  .  #
    #######
    """,

    // Level 2 — Classic 1: push two boxes up to the goals (5 moves, 2 pushes).
    """
    ######
    # .. #
    # $$ #
    #  @ #
    ######
    """,

    // Level 3 — Classic 2: two boxes in a row, must navigate around (6 moves, 2 pushes).
    """
    #######
    #.    #
    #$ @$.#
    #     #
    #######
    """,

    // Level 4 — Expert 1: three vertically-aligned box/goal pairs (10 moves, 3 pushes).
    """
    #########
    #  . . .#
    #  $ $ $#
    #   @   #
    #########
    """,

    // Level 5 — Expert 2: two boxes require multi-step steering (14 moves, 3 pushes).
    """
    #########
    # .   . #
    #  $    #
    #    $  #
    #       #
    #  @    #
    #########
    """
)
