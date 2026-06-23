package com.xanticious.androidgames.model.games.flood

/**
 * Immutable Flood puzzle board state (`design/puzzle-games/flood/flood-design.md`).
 *
 * The board is an N×N grid of color-indexed cells flattened row-major into [grid].
 * The "flood region" always originates at the top-left cell (index 0) and is
 * the maximal connected set of cells sharing the color [currentColor].
 *
 * There is no fail state: the player keeps choosing colors until the board is
 * uniformly one color. Performance relative to [minMoves] (the optimal solution)
 * is tracked via [par] = [minMoves] + [handicap].
 */
data class FloodState(
    val size: Int,
    val colorCount: Int,
    /** Flattened N×N grid; each entry is a 0-based color index. */
    val grid: List<Int>,
    val moves: Int = 0,
    /** Exact minimum number of color choices needed to solve this board. */
    val minMoves: Int = 0,
    /** Personal target: how many moves above optimal the player aims for (0/2/4/6). */
    val handicap: Int = 4,
    /** Previous grid snapshots, earliest-first; supports unlimited undo. */
    val history: List<List<Int>> = emptyList()
) {
    val canUndo: Boolean get() = history.isNotEmpty()

    /** The par target shown to the player: optimal + personal handicap. */
    val par: Int get() = minMoves + handicap

    /** Color of the current flood region — the color of the top-left cell. */
    val currentColor: Int get() = grid[0]
}
