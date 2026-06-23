package com.xanticious.androidgames.controller.games.matchthree

import com.xanticious.androidgames.model.games.matchthree.CascadeResult
import com.xanticious.androidgames.model.games.matchthree.GemType
import com.xanticious.androidgames.model.games.matchthree.MatchThreeBoard
import com.xanticious.androidgames.model.games.matchthree.SwapResult
import com.xanticious.androidgames.model.games.puzzle.GridPos
import kotlin.math.abs
import kotlin.random.Random

/**
 * Pure Match Three rules: board generation, match detection, swap resolution and
 * cascade handling. No Android or Compose imports — fully JVM unit-testable.
 *
 * Every function is a self-contained transformation: it takes immutable input and
 * returns new immutable output. The [Random] parameter makes every operation
 * seedable and therefore deterministic in tests.
 */
class MatchThreeController {

    /** Board sizes offered in settings, smallest first. */
    val boardSizeOptions: List<Int> = listOf(6, 7, 8, 9)

    /** Active-gem-type counts offered in settings, fewest first. */
    val gemTypeOptions: List<Int> = listOf(4, 5, 6)

    // ── Board generation ──────────────────────────────────────────────────────

    /**
     * Generates a fresh [rows]×[cols] board using the first [gemTypes] gem types
     * with no initial matches. Fills cells left-to-right, top-to-bottom; for each
     * cell it avoids placing a gem that would immediately complete a horizontal or
     * vertical triple with the two already-placed neighbours in the same line.
     */
    fun newBoard(
        rows: Int,
        cols: Int,
        gemTypes: Int,
        random: Random = Random.Default
    ): MatchThreeBoard {
        val active = GemType.entries.take(gemTypes)
        val gems = mutableListOf<GemType?>()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val forbidden = mutableSetOf<GemType>()
                // Would complete a horizontal triple?
                if (col >= 2) {
                    val l1 = gems[row * cols + col - 1]
                    val l2 = gems[row * cols + col - 2]
                    if (l1 != null && l1 == l2) forbidden += l1
                }
                // Would complete a vertical triple?
                if (row >= 2) {
                    val u1 = gems[(row - 1) * cols + col]
                    val u2 = gems[(row - 2) * cols + col]
                    if (u1 != null && u1 == u2) forbidden += u1
                }
                val candidates = active.filter { it !in forbidden }
                    .ifEmpty { active }
                gems += candidates[random.nextInt(candidates.size)]
            }
        }
        return MatchThreeBoard(rows, cols, gems)
    }

    // ── Match detection ───────────────────────────────────────────────────────

    /**
     * Returns every [GridPos] that belongs to a horizontal or vertical run of
     * three or more identical non-null gems.
     */
    fun findMatches(board: MatchThreeBoard): Set<GridPos> {
        val matched = mutableSetOf<GridPos>()

        // Horizontal runs
        for (row in 0 until board.rows) {
            var col = 0
            while (col < board.cols) {
                val gem = board.get(GridPos(row, col))
                if (gem == null) { col++; continue }
                var len = 1
                while (col + len < board.cols && board.get(GridPos(row, col + len)) == gem) len++
                if (len >= 3) for (c in col until col + len) matched += GridPos(row, c)
                col += len
            }
        }

        // Vertical runs
        for (col in 0 until board.cols) {
            var row = 0
            while (row < board.rows) {
                val gem = board.get(GridPos(row, col))
                if (gem == null) { row++; continue }
                var len = 1
                while (row + len < board.rows && board.get(GridPos(row + len, col)) == gem) len++
                if (len >= 3) for (r in row until row + len) matched += GridPos(r, col)
                row += len
            }
        }

        return matched
    }

    /**
     * Returns `true` if at least one adjacent swap would produce a match of three
     * or more. Tries every horizontal and vertical adjacent pair.
     */
    fun hasValidMove(board: MatchThreeBoard): Boolean {
        for (row in 0 until board.rows) {
            for (col in 0 until board.cols - 1) {
                if (findMatches(swapGems(board, GridPos(row, col), GridPos(row, col + 1))).isNotEmpty())
                    return true
            }
        }
        for (row in 0 until board.rows - 1) {
            for (col in 0 until board.cols) {
                if (findMatches(swapGems(board, GridPos(row, col), GridPos(row + 1, col))).isNotEmpty())
                    return true
            }
        }
        return false
    }

    /** Returns `true` when [a] and [b] are orthogonally adjacent (not diagonal). */
    fun areAdjacent(a: GridPos, b: GridPos): Boolean {
        val dr = abs(a.row - b.row)
        val dc = abs(a.col - b.col)
        return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
    }

    // ── Swap and cascade ──────────────────────────────────────────────────────

    /**
     * Attempts to swap gems at [a] and [b].
     *
     * - Returns a [SwapResult] with `valid = false` and the original [board] when
     *   the cells are not adjacent or the swap creates no match.
     * - On a valid swap, resolves all cascades via [resolveCascades] and returns
     *   the stable board together with the total cleared count and cascade depth.
     */
    fun applySwap(
        board: MatchThreeBoard,
        a: GridPos,
        b: GridPos,
        gemTypes: Int,
        random: Random = Random.Default
    ): SwapResult {
        if (!areAdjacent(a, b)) return SwapResult(false, board, 0, 0)
        val swapped = swapGems(board, a, b)
        if (findMatches(swapped).isEmpty()) return SwapResult(false, board, 0, 0)
        val cascade = resolveCascades(swapped, gemTypes, random)
        return SwapResult(true, cascade.board, cascade.totalCleared, cascade.cascadeCount)
    }

    /**
     * Clears all current matches, drops gems down and refills from the top until
     * no matches remain. Returns the stable board with accumulated cleared/cascade
     * counts.
     *
     * The gem drop compacts each column downward (nulls move to the top), then
     * [gemTypes] random gems are generated to fill empty cells.
     */
    fun resolveCascades(
        board: MatchThreeBoard,
        gemTypes: Int,
        random: Random = Random.Default
    ): CascadeResult {
        var current = board
        var totalCleared = 0
        var cascadeCount = 0
        while (true) {
            val matches = findMatches(current)
            if (matches.isEmpty()) break
            totalCleared += matches.size
            cascadeCount++
            current = refillBoard(dropGems(clearCells(current, matches)), gemTypes, random)
        }
        return CascadeResult(current, totalCleared, cascadeCount)
    }

    /**
     * Reshuffles the existing gems (preserving per-type counts) until the result
     * has no initial matches and at least one valid move. Falls back to [newBoard]
     * after 200 failed attempts.
     */
    fun reshuffle(board: MatchThreeBoard, random: Random = Random.Default): MatchThreeBoard {
        val allGems = board.gems.filterNotNull().toMutableList()
        val gemTypes = allGems.map { it.ordinal }.toSet().size.coerceAtLeast(3)
        repeat(200) {
            allGems.shuffle(random)
            val attempt = MatchThreeBoard(board.rows, board.cols, allGems.toList())
            if (findMatches(attempt).isEmpty() && hasValidMove(attempt)) return attempt
        }
        return newBoard(board.rows, board.cols, gemTypes, random)
    }

    /**
     * Seconds of bonus time to add for a cascade event with [cascadeCount] rounds.
     * A single-round match (cascadeCount == 1) grants no bonus. Each additional
     * round adds 0.5 s, capped at 3.0 s.
     */
    fun cascadeTimeBonus(cascadeCount: Int): Float =
        ((cascadeCount - 1) * 0.5f).coerceIn(0f, 3.0f)

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun swapGems(board: MatchThreeBoard, a: GridPos, b: GridPos): MatchThreeBoard {
        val gems = board.gems.toMutableList()
        val ai = a.row * board.cols + a.col
        val bi = b.row * board.cols + b.col
        val tmp = gems[ai]; gems[ai] = gems[bi]; gems[bi] = tmp
        return board.copy(gems = gems)
    }

    private fun clearCells(board: MatchThreeBoard, cells: Set<GridPos>): MatchThreeBoard {
        val gems = board.gems.toMutableList()
        for (pos in cells) gems[pos.row * board.cols + pos.col] = null
        return board.copy(gems = gems)
    }

    /** Compacts each column so nulls float to the top (gems fall toward higher row indices). */
    private fun dropGems(board: MatchThreeBoard): MatchThreeBoard {
        val gems = board.gems.toMutableList()
        for (col in 0 until board.cols) {
            val nonNull = (0 until board.rows)
                .mapNotNull { row -> gems[row * board.cols + col] }
            val nullCount = board.rows - nonNull.size
            for (row in 0 until board.rows) {
                gems[row * board.cols + col] =
                    if (row < nullCount) null else nonNull[row - nullCount]
            }
        }
        return board.copy(gems = gems)
    }

    /** Fills every `null` cell with a random gem drawn from the first [gemTypes] types. */
    private fun refillBoard(
        board: MatchThreeBoard,
        gemTypes: Int,
        random: Random
    ): MatchThreeBoard {
        val active = GemType.entries.take(gemTypes)
        val gems = board.gems.toMutableList()
        for (i in gems.indices) {
            if (gems[i] == null) gems[i] = active[random.nextInt(active.size)]
        }
        return board.copy(gems = gems)
    }
}
