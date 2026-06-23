package com.xanticious.androidgames.controller.games.flood

import com.xanticious.androidgames.model.games.flood.FloodState
import kotlin.random.Random

/**
 * Pure Flood puzzle rules: board generation, color-application, undo, and the
 * solved check. No Android or Compose imports — the entire rule set is JVM
 * unit-testable.
 *
 * The flood region is the maximal orthogonally-connected set of same-colored
 * cells reachable from the top-left corner. Applying a color repaints that
 * region and cascade-absorbs any adjacent cells that already share the new color.
 *
 * The minimum-move solver builds a zone graph (connected monochromatic regions)
 * and runs BFS over absorbed-zone bitmasks. For boards with ≤ 60 zones the result
 * is exact (capped at 300 K states to stay fast); larger boards fall back to a
 * greedy estimate.
 */
class FloodController {

    /** Board sizes offered in settings: Small, Medium (default), Large. */
    val boardSizes: List<Int> = listOf(10, 14, 18)

    /** Color counts offered in settings, fewest (easiest) first. */
    val colorCounts: List<Int> = listOf(4, 5, 6, 7, 8)

    /** Handicap (moves above optimal) options. */
    val handicaps: List<Int> = listOf(0, 2, 4, 6)

    /** Deals a fresh random board and computes its minimum-move baseline. */
    fun newGame(
        size: Int,
        colorCount: Int,
        random: Random = Random.Default,
        handicap: Int = 4
    ): FloodState {
        val grid = List(size * size) { random.nextInt(colorCount) }
        val minMoves = minMovesFor(grid, size, colorCount)
        return FloodState(
            size = size,
            colorCount = colorCount,
            grid = grid,
            minMoves = minMoves,
            handicap = handicap
        )
    }

    /**
     * Applies [colorIndex] as the next flood move. The current region (connected
     * same-color cells from the top-left) is repainted to [colorIndex], then any
     * adjacent cells of that color are cascade-absorbed. Tapping the current
     * region color is a no-op.
     */
    fun applyColor(state: FloodState, colorIndex: Int): FloodState {
        if (colorIndex == state.currentColor) return state
        val region = floodRegionIndices(state.grid, state.size)
        val newGrid = state.grid.toMutableList()
        for (idx in region) newGrid[idx] = colorIndex
        return state.copy(
            grid = newGrid,
            moves = state.moves + 1,
            history = state.history + listOf(state.grid)
        )
    }

    /** Reverts the most recent color choice. Move count decrements. */
    fun undo(state: FloodState): FloodState {
        val previous = state.history.lastOrNull() ?: return state
        return state.copy(
            grid = previous,
            moves = state.moves - 1,
            history = state.history.dropLast(1)
        )
    }

    /** Returns true when every cell shares one color. */
    fun isSolved(state: FloodState): Boolean =
        state.grid.all { it == state.currentColor }

    /** Number of cells in the current flood region. */
    fun regionSize(state: FloodState): Int =
        floodRegionIndices(state.grid, state.size).size

    /**
     * Returns the set of flat cell indices belonging to the current flood region
     * (cells connected to index 0 with the same color as cell 0).
     */
    fun floodRegionSet(grid: List<Int>, size: Int): Set<Int> =
        floodRegionIndices(grid, size)

    /**
     * Computes the exact or estimated minimum number of color choices needed to
     * flood the given board. Public so tests can verify it independently of
     * [newGame].
     */
    fun minMovesFor(grid: List<Int>, size: Int, colorCount: Int): Int {
        val zg = buildZoneGraph(grid, size)
        if (zg.nZones <= 1) return 0
        return if (zg.nZones <= 60) bfsMinMoves(zg, colorCount) else greedyMinMoves(zg, colorCount)
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    private fun floodRegionIndices(grid: List<Int>, size: Int): Set<Int> {
        val color = grid[0]
        val visited = mutableSetOf<Int>()
        val queue = ArrayDeque<Int>()
        queue.add(0)
        visited.add(0)
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val r = cur / size
            val c = cur % size
            for ((dr, dc) in DIRS) {
                val nr = r + dr; val nc = c + dc
                if (nr in 0 until size && nc in 0 until size) {
                    val nIdx = nr * size + nc
                    if (nIdx !in visited && grid[nIdx] == color) {
                        visited.add(nIdx)
                        queue.add(nIdx)
                    }
                }
            }
        }
        return visited
    }

    // ── Zone graph ──────────────────────────────────────────────────────────

    private data class ZoneGraph(
        val nZones: Int,
        val zoneColor: IntArray,
        val zoneAdj: Array<IntArray>
    )

    private fun buildZoneGraph(grid: List<Int>, size: Int): ZoneGraph {
        val total = size * size
        val zoneId = IntArray(total) { -1 }
        val zoneColors = mutableListOf<Int>()
        var nZones = 0

        for (start in 0 until total) {
            if (zoneId[start] != -1) continue
            val id = nZones++
            zoneColors.add(grid[start])
            val q = ArrayDeque<Int>()
            q.add(start)
            zoneId[start] = id
            while (q.isNotEmpty()) {
                val cur = q.removeFirst()
                val r = cur / size; val c = cur % size
                for ((dr, dc) in DIRS) {
                    val nr = r + dr; val nc = c + dc
                    if (nr in 0 until size && nc in 0 until size) {
                        val nIdx = nr * size + nc
                        if (zoneId[nIdx] == -1 && grid[nIdx] == grid[start]) {
                            zoneId[nIdx] = id
                            q.add(nIdx)
                        }
                    }
                }
            }
        }

        val adjSets = Array(nZones) { mutableSetOf<Int>() }
        for (idx in 0 until total) {
            val r = idx / size; val c = idx % size
            for ((dr, dc) in DIRS) {
                val nr = r + dr; val nc = c + dc
                if (nr in 0 until size && nc in 0 until size) {
                    val nIdx = nr * size + nc
                    if (zoneId[idx] != zoneId[nIdx]) {
                        adjSets[zoneId[idx]].add(zoneId[nIdx])
                    }
                }
            }
        }

        return ZoneGraph(
            nZones = nZones,
            zoneColor = zoneColors.toIntArray(),
            zoneAdj = Array(nZones) { adjSets[it].toIntArray() }
        )
    }

    // ── BFS min-moves solver (exact for nZones ≤ 60) ───────────────────────

    /**
     * BFS over absorbed-zone bitmasks (Long). Each bit i represents whether
     * zone i has been absorbed into the flood region. BFS finds the shortest
     * sequence of color choices that absorbs all zones.
     */
    private fun bfsMinMoves(zg: ZoneGraph, colorCount: Int): Int {
        val goal = (1L shl zg.nZones) - 1L
        val start = 1L  // zone 0 absorbed at the start
        if (start == goal) return 0

        val visited = HashSet<Long>(4096)
        visited.add(start)
        var frontier = mutableListOf(start)
        val stateLimit = 300_000

        var dist = 0
        while (frontier.isNotEmpty()) {
            dist++
            val next = mutableListOf<Long>()
            for (state in frontier) {
                for (color in 0 until colorCount) {
                    val ns = expandZones(state, zg, color)
                    if (ns == goal) return dist
                    if (ns != state && visited.add(ns)) {
                        next.add(ns)
                        if (visited.size > stateLimit) {
                            // State space exceeded: finish with greedy from best reached state
                            return dist + greedyMinMoves(zg, colorCount, ns)
                        }
                    }
                }
            }
            frontier = next
        }
        return dist
    }

    /**
     * Expands the absorbed-zone set [absorbed] by absorbing all zones reachable
     * from the current region through zones of [color].
     */
    private fun expandZones(absorbed: Long, zg: ZoneGraph, color: Int): Long {
        var result = absorbed
        val queue = ArrayDeque<Int>()
        // Seed: non-absorbed zones adjacent to the absorbed set with the chosen color
        for (z in 0 until zg.nZones) {
            if ((absorbed ushr z) and 1L == 0L) continue
            for (adj in zg.zoneAdj[z]) {
                if ((result ushr adj) and 1L == 0L && zg.zoneColor[adj] == color) {
                    result = result or (1L shl adj)
                    queue.add(adj)
                }
            }
        }
        // BFS cascade: newly absorbed zones may expose more same-color neighbors
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            for (adj in zg.zoneAdj[cur]) {
                if ((result ushr adj) and 1L == 0L && zg.zoneColor[adj] == color) {
                    result = result or (1L shl adj)
                    queue.add(adj)
                }
            }
        }
        return result
    }

    // ── Greedy fallback (upper-bound estimate) ──────────────────────────────

    /**
     * Greedy simulation: at each step choose the color that absorbs the most new
     * zones. Returns an upper-bound move count (may exceed optimal by a few moves
     * on complex boards). Used when the zone count exceeds the BFS bitmask limit
     * or when BFS hits its state cap.
     */
    private fun greedyMinMoves(
        zg: ZoneGraph,
        colorCount: Int,
        startMask: Long? = null
    ): Int {
        // Use BitSet to handle any zone count
        val absorbed = java.util.BitSet(zg.nZones)
        if (startMask != null) {
            for (z in 0 until minOf(zg.nZones, 63)) {
                if ((startMask ushr z) and 1L != 0L) absorbed.set(z)
            }
        } else {
            absorbed.set(0)
        }

        var moves = 0
        val maxMoves = zg.nZones + colorCount + 1

        while (absorbed.cardinality() < zg.nZones && moves < maxMoves) {
            var bestColor = 0
            var bestGain = 0
            for (color in 0 until colorCount) {
                val gain = countGreedyExpand(absorbed, zg, color)
                if (gain > bestGain) { bestGain = gain; bestColor = color }
            }
            if (bestGain == 0) break
            applyGreedyExpand(absorbed, zg, bestColor)
            moves++
        }
        return moves
    }

    private fun countGreedyExpand(absorbed: java.util.BitSet, zg: ZoneGraph, color: Int): Int {
        val extra = java.util.BitSet(zg.nZones)
        val queue = ArrayDeque<Int>()
        for (z in absorbed.stream().toArray()) {
            for (adj in zg.zoneAdj[z]) {
                if (!absorbed.get(adj) && !extra.get(adj) && zg.zoneColor[adj] == color) {
                    extra.set(adj); queue.add(adj)
                }
            }
        }
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            for (adj in zg.zoneAdj[cur]) {
                if (!absorbed.get(adj) && !extra.get(adj) && zg.zoneColor[adj] == color) {
                    extra.set(adj); queue.add(adj)
                }
            }
        }
        return extra.cardinality()
    }

    private fun applyGreedyExpand(absorbed: java.util.BitSet, zg: ZoneGraph, color: Int) {
        val toAdd = mutableListOf<Int>()
        val queue = ArrayDeque<Int>()
        for (z in absorbed.stream().toArray()) {
            for (adj in zg.zoneAdj[z]) {
                if (!absorbed.get(adj) && zg.zoneColor[adj] == color) {
                    absorbed.set(adj); toAdd.add(adj); queue.add(adj)
                }
            }
        }
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            for (adj in zg.zoneAdj[cur]) {
                if (!absorbed.get(adj) && zg.zoneColor[adj] == color) {
                    absorbed.set(adj); queue.add(adj)
                }
            }
        }
    }

    companion object {
        private val DIRS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    }
}
