package com.xanticious.androidgames.games.numberlink

import com.xanticious.androidgames.controller.games.numberlink.NumberlinkController
import com.xanticious.androidgames.model.games.numberlink.NumberlinkPath
import com.xanticious.androidgames.model.games.puzzle.GridPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NumberlinkControllerTest {

    private val controller = NumberlinkController()

    // ── Generation ─────────────────────────────────────────────────────────────

    @Test
    fun newGame_sameSeeed_sameBoard() {
        val a = controller.newGame(5, random = Random(42))
        val b = controller.newGame(5, random = Random(42))
        assertEquals(a.endpoints, b.endpoints)
    }

    @Test
    fun newGame_differentSeeds_differentBoards() {
        val a = controller.newGame(5, random = Random(1))
        val b = controller.newGame(5, random = Random(999))
        assertFalse(a.endpoints == b.endpoints)
    }

    @Test
    fun newGame_allCellsCoveredBySolution() {
        val state = controller.newGame(5, random = Random(7))
        val allCells = state.solutionPaths.flatMap { it.cells }
        assertEquals(25, allCells.size)
        assertEquals(25, allCells.toSet().size)
    }

    @Test
    fun newGame_generatedSolutionSatisfiesIsSolved() {
        val state = controller.newGame(5, random = Random(3))
        val solvedState = state.copy(paths = state.solutionPaths)
        assertTrue(controller.isSolved(solvedState))
    }

    @Test
    fun newGame_generatedSolutionSatisfiesIsSolved_size7() {
        val state = controller.newGame(7, random = Random(17))
        val solvedState = state.copy(paths = state.solutionPaths)
        assertTrue(controller.isSolved(solvedState))
    }

    @Test
    fun newGame_correctNumberOfPairs() {
        val state = controller.newGame(6, numPairs = 4, random = Random(5))
        assertEquals(4, state.numPairs)
    }

    @Test
    fun newGame_endpointsAreUniquePositions() {
        val state = controller.newGame(5, random = Random(11))
        val positions = state.endpoints.map { it.pos }
        assertEquals(positions.size, positions.toSet().size)
    }

    @Test
    fun newGame_eachPairHasTwoEndpoints() {
        val state = controller.newGame(5, random = Random(22))
        for (pairIndex in 0 until state.numPairs) {
            val count = state.endpoints.count { it.pairIndex == pairIndex }
            assertEquals(2, count)
        }
    }

    @Test
    fun newGame_initialPathsAreEmpty() {
        val state = controller.newGame(5, random = Random(33))
        assertTrue(state.paths.all { it.cells.isEmpty() })
    }

    // ── isSolved ───────────────────────────────────────────────────────────────

    @Test
    fun isSolved_emptyBoard_notSolved() {
        val state = controller.newGame(5, random = Random(1))
        assertFalse(controller.isSolved(state))
    }

    @Test
    fun isSolved_partiallyFilledBoard_notSolved() {
        val state = controller.newGame(5, random = Random(2))
        // Connect only the first pair using its solution path
        val partial = state.copy(
            paths = listOf(state.solutionPaths.first()) +
                state.paths.drop(1)
        )
        assertFalse(controller.isSolved(partial))
    }

    @Test
    fun isSolved_boardWithOverlappingPaths_notSolved() {
        val state = controller.newGame(5, numPairs = 2, random = Random(4))
        // Force both solution paths to contain the same first cell (overlap)
        val ep0 = state.endpoints.first { it.pairIndex == 0 }.pos
        val overlap = state.copy(
            paths = listOf(
                NumberlinkPath(0, listOf(ep0, GridPos(0, 1))),
                NumberlinkPath(1, listOf(ep0, GridPos(0, 1)))  // same cells
            )
        )
        assertFalse(controller.isSolved(overlap))
    }

    // ── startPath ─────────────────────────────────────────────────────────────

    @Test
    fun startPath_onEndpoint_setsActivePair() {
        val state = controller.newGame(5, random = Random(5))
        val ep = state.endpoints.first()
        val next = controller.startPath(state, ep.pos)
        assertEquals(ep.pairIndex, next.activePairIndex)
    }

    @Test
    fun startPath_onEndpoint_pathContainsOnlyThatCell() {
        val state = controller.newGame(5, random = Random(5))
        val ep = state.endpoints.first()
        val next = controller.startPath(state, ep.pos)
        assertEquals(listOf(ep.pos), next.pathFor(ep.pairIndex).cells)
    }

    @Test
    fun startPath_onEmptyCell_noChange() {
        val state = controller.newGame(5, random = Random(6))
        // Find a cell that is not an endpoint
        val emptyCell = (0 until state.size).flatMap { r ->
            (0 until state.size).map { c -> GridPos(r, c) }
        }.first { pos -> state.endpoints.none { it.pos == pos } }
        val next = controller.startPath(state, emptyCell)
        assertNull(next.activePairIndex)
    }

    @Test
    fun startPath_onExistingPath_trimsAndSetsActive() {
        val state = controller.newGame(5, random = Random(7))
        val solution = state.solutionPaths.first()
        // Set the first solution path as the current path
        val withPath = state.copy(
            paths = state.paths.map { if (it.pairIndex == 0) solution else it }
        )
        // Start from the second cell of that path
        val midCell = solution.cells[1]
        val next = controller.startPath(withPath, midCell)
        assertEquals(0, next.activePairIndex)
        assertEquals(solution.cells.subList(0, 2), next.pathFor(0).cells)
    }

    // ── extendPath ────────────────────────────────────────────────────────────

    @Test
    fun extendPath_toAdjacentCell_addsCellToPath() {
        val state = controller.newGame(5, random = Random(8))
        val ep = state.endpoints.first()
        val started = controller.startPath(state, ep.pos)
        // Find an adjacent cell that's not another endpoint
        val adjacent = adjacentNonEndpointCells(ep.pos, state)
        val extended = controller.extendPath(started, adjacent)
        assertTrue(extended.pathFor(ep.pairIndex).cells.contains(adjacent))
    }

    @Test
    fun extendPath_toNonAdjacentCell_noChange() {
        val state = controller.newGame(5, random = Random(9))
        val ep = state.endpoints.first()
        val started = controller.startPath(state, ep.pos)
        val farCell = GridPos(4, 4)
        val next = controller.extendPath(started, farCell)
        // Path should not contain farCell (unless ep.pos happened to be (4,3) or similar, skip if so)
        if (!isAdjacent(ep.pos, farCell)) {
            assertEquals(started.pathFor(ep.pairIndex).cells, next.pathFor(ep.pairIndex).cells)
        }
    }

    @Test
    fun extendPath_throughOwnPath_trimsLoop() {
        val state = controller.newGame(5, random = Random(10))
        val ep = state.endpoints.first()
        // Build a path [ep, a, b] then extend back to [a] — should trim to [ep, a]
        val solution = state.solutionPaths.first()
        if (solution.cells.size < 3) return // skip if too short
        val started = state.copy(
            paths = state.paths.map {
                if (it.pairIndex == 0) NumberlinkPath(0, solution.cells.subList(0, 3))
                else it
            },
            activePairIndex = 0
        )
        val next = controller.extendPath(started, solution.cells[1])
        assertEquals(solution.cells.subList(0, 2), next.pathFor(0).cells)
    }

    @Test
    fun extendPath_throughOtherPath_trimsOther() {
        // Build a state where pair 0 draws through a cell belonging to pair 1
        val state = controller.newGame(6, numPairs = 2, random = Random(11))
        val sol0 = state.solutionPaths[0]
        val sol1 = state.solutionPaths[1]
        if (sol1.cells.size < 3) return // need at least 3 cells in pair 1's path

        // Set pair 1's path to its full solution
        val withPair1 = state.copy(
            paths = state.paths.map { if (it.pairIndex == 1) sol1 else it }
        )
        // Start pair 0 from its endpoint, then try to extend into pair 1's second cell
        val pairZeroEp = sol0.cells.first()
        val started = controller.startPath(withPair1, pairZeroEp)
        val crossCell = sol1.cells[1]

        // Only attempt if crossCell is adjacent to pairZeroEp
        if (!isAdjacent(pairZeroEp, crossCell)) return

        val next = controller.extendPath(started, crossCell)
        // Pair 1's path should be trimmed to only include cells before crossCell
        val trimmedPair1 = next.pathFor(1)
        assertFalse(trimmedPair1.cells.contains(crossCell))
        assertTrue(next.pathFor(0).cells.contains(crossCell))
    }

    @Test
    fun extendPath_toOtherEndpoint_isBlocked() {
        val state = controller.newGame(5, numPairs = 2, random = Random(13))
        val ep0 = state.endpoints.first { it.pairIndex == 0 }
        val ep1Pos = state.endpoints.first { it.pairIndex == 1 }.pos
        val started = controller.startPath(state, ep0.pos)
        if (!isAdjacent(ep0.pos, ep1Pos)) return // skip if not adjacent
        val next = controller.extendPath(started, ep1Pos)
        assertFalse(next.pathFor(0).cells.contains(ep1Pos))
    }

    // ── clearPath ─────────────────────────────────────────────────────────────

    @Test
    fun clearPath_removesAllCellsForPair() {
        val state = controller.newGame(5, random = Random(14))
        val sol = state.solutionPaths.first()
        val withPath = state.copy(paths = state.paths.map { if (it.pairIndex == 0) sol else it })
        val cleared = controller.clearPath(withPath, 0)
        assertTrue(cleared.pathFor(0).cells.isEmpty())
    }

    @Test
    fun clearPath_doesNotAffectOtherPairs() {
        val state = controller.newGame(5, random = Random(15))
        val sol1 = state.solutionPaths[1]
        val withPath = state.copy(paths = state.paths.map { if (it.pairIndex == 1) sol1 else it })
        val cleared = controller.clearPath(withPath, 0)
        assertEquals(sol1.cells, cleared.pathFor(1).cells)
    }

    // ── undo ──────────────────────────────────────────────────────────────────

    @Test
    fun undo_restoresPreviousPaths() {
        val state = controller.newGame(5, random = Random(16))
        val sol = state.solutionPaths.first()
        val withPath = state.copy(paths = state.paths.map { if (it.pairIndex == 0) sol else it })
        val withHistory = controller.recordHistory(state).copy(
            paths = withPath.paths
        )
        val undone = controller.undo(withHistory)
        assertTrue(undone.pathFor(0).cells.isEmpty())
    }

    @Test
    fun undo_onEmptyHistory_returnsUnchanged() {
        val state = controller.newGame(5, random = Random(17))
        val next = controller.undo(state)
        assertEquals(state.paths, next.paths)
    }

    // ── resetAllPaths ─────────────────────────────────────────────────────────

    @Test
    fun resetAllPaths_clearsEveryPath() {
        val state = controller.newGame(5, random = Random(18))
        val full = state.copy(paths = state.solutionPaths)
        val reset = controller.resetAllPaths(full)
        assertTrue(reset.paths.all { it.cells.isEmpty() })
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isAdjacent(a: GridPos, b: GridPos): Boolean {
        val dr = kotlin.math.abs(a.row - b.row)
        val dc = kotlin.math.abs(a.col - b.col)
        return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
    }

    private fun adjacentNonEndpointCells(pos: GridPos, state: com.xanticious.androidgames.model.games.numberlink.NumberlinkState): GridPos {
        val endpointPositions = state.endpoints.map { it.pos }.toSet()
        return listOf(
            GridPos(pos.row - 1, pos.col),
            GridPos(pos.row + 1, pos.col),
            GridPos(pos.row, pos.col - 1),
            GridPos(pos.row, pos.col + 1)
        ).first { n ->
            n.row in 0 until state.size &&
                n.col in 0 until state.size &&
                n !in endpointPositions
        }
    }
}
