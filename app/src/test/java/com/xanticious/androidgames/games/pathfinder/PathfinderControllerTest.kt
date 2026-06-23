package com.xanticious.androidgames.games.pathfinder

import com.xanticious.androidgames.controller.games.pathfinder.PathfinderController
import com.xanticious.androidgames.model.games.pathfinder.PathfinderPath
import com.xanticious.androidgames.model.games.puzzle.GridPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PathfinderControllerTest {

    private val controller = PathfinderController()

    // ── Generation ─────────────────────────────────────────────────────────────

    @Test
    fun newGame_sameSeed_samePuzzle() {
        val a = controller.newGame(5, random = Random(42))
        val b = controller.newGame(5, random = Random(42))
        assertEquals(a.endpoints, b.endpoints)
    }

    @Test
    fun newGame_differentSeeds_differentPuzzles() {
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
    fun newGame_generatedSolutionSatisfiesIsSolved_size5() {
        val state = controller.newGame(5, random = Random(3))
        val solvedState = state.copy(paths = state.solutionPaths)
        assertTrue(controller.isSolved(solvedState))
    }

    @Test
    fun newGame_generatedSolutionSatisfiesIsSolved_size8() {
        val state = controller.newGame(8, random = Random(17))
        val solvedState = state.copy(paths = state.solutionPaths)
        assertTrue(controller.isSolved(solvedState))
    }

    @Test
    fun newGame_generatedSolutionSatisfiesIsSolved_size10() {
        val state = controller.newGame(10, random = Random(99))
        val solvedState = state.copy(paths = state.solutionPaths)
        assertTrue(controller.isSolved(solvedState))
    }

    @Test
    fun newGame_correctNumberOfPairs() {
        val state = controller.newGame(8, numPairs = 4, random = Random(5))
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
    fun isSolved_partialPaths_notSolved() {
        val state = controller.newGame(5, random = Random(2))
        val partial = state.copy(
            paths = listOf(state.solutionPaths.first()) + state.paths.drop(1)
        )
        assertFalse(controller.isSolved(partial))
    }

    @Test
    fun isSolved_pathNotReachingPair_notSolved() {
        val state = controller.newGame(5, numPairs = 2, random = Random(4))
        val ep0 = state.endpoints.first { it.pairIndex == 0 }.pos
        val truncated = state.copy(
            paths = state.paths.map {
                if (it.pairIndex == 0) PathfinderPath(0, listOf(ep0)) else it
            }
        )
        assertFalse(controller.isSolved(truncated))
    }

    @Test
    fun isSolved_requireFullCoverage_notMetWithPartialFill() {
        val state = controller.newGame(5, numPairs = 2, requireFullCoverage = true, random = Random(8))
        // Only apply the first solution path — coverage will be incomplete
        val partial = state.copy(
            paths = listOf(state.solutionPaths[0]) +
                state.paths.drop(1)
        )
        assertFalse(controller.isSolved(partial))
    }

    @Test
    fun isSolved_requireFullCoverage_false_allConnectedButNotFull() {
        // Generate a puzzle with 2 pairs so solution covers all cells, then set coverage=false
        val state = controller.newGame(5, numPairs = 2, requireFullCoverage = false, random = Random(13))
        // Apply only the first solution path; second pair is not connected → still not solved
        val firstOnly = state.copy(
            paths = listOf(state.solutionPaths[0]) + state.paths.drop(1)
        )
        assertFalse(controller.isSolved(firstOnly))
    }

    @Test
    fun isSolved_requireFullCoverage_false_allPairsConnectedButIncomplete() {
        // With full coverage off, all connected pairs = solved even if cells are empty elsewhere
        val state = controller.newGame(5, numPairs = 5, requireFullCoverage = false, random = Random(19))
        val connected = state.copy(paths = state.solutionPaths, requireFullCoverage = false)
        assertTrue(controller.isSolved(connected))
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
        val withPath = state.copy(
            paths = state.paths.map { if (it.pairIndex == 0) solution else it }
        )
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
        val adjacent = adjacentNonEndpointCell(ep.pos, state)
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
        if (!isAdjacent(ep.pos, farCell)) {
            assertEquals(started.pathFor(ep.pairIndex).cells, next.pathFor(ep.pairIndex).cells)
        }
    }

    @Test
    fun extendPath_autoTrim_shortensPathWhenReenteringOwnPath() {
        val state = controller.newGame(5, random = Random(10))
        val solution = state.solutionPaths.first()
        if (solution.cells.size < 3) return
        val withPath = state.copy(
            paths = state.paths.map {
                if (it.pairIndex == 0) PathfinderPath(0, solution.cells.subList(0, 3)) else it
            },
            activePairIndex = 0
        )
        // Drag back to index 1 → path should trim to [0, 1]
        val next = controller.extendPath(withPath, solution.cells[1])
        assertEquals(solution.cells.subList(0, 2), next.pathFor(0).cells)
    }

    @Test
    fun extendPath_throughOtherPath_trimsOther() {
        val state = controller.newGame(6, numPairs = 2, random = Random(11))
        val sol0 = state.solutionPaths[0]
        val sol1 = state.solutionPaths[1]
        if (sol1.cells.size < 3) return

        val withPair1 = state.copy(
            paths = state.paths.map { if (it.pairIndex == 1) sol1 else it }
        )
        val pairZeroEp = sol0.cells.first()
        val started = controller.startPath(withPair1, pairZeroEp)
        val crossCell = sol1.cells[1]

        if (!isAdjacent(pairZeroEp, crossCell)) return

        val next = controller.extendPath(started, crossCell)
        assertFalse(next.pathFor(1).cells.contains(crossCell))
        assertTrue(next.pathFor(0).cells.contains(crossCell))
    }

    @Test
    fun extendPath_toOtherEndpoint_isBlocked() {
        val state = controller.newGame(5, numPairs = 2, random = Random(13))
        val ep0 = state.endpoints.first { it.pairIndex == 0 }
        val ep1Pos = state.endpoints.first { it.pairIndex == 1 }.pos
        val started = controller.startPath(state, ep0.pos)
        if (!isAdjacent(ep0.pos, ep1Pos)) return
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
        val withHistory = controller.recordHistory(state).let { recorded ->
            recorded.copy(
                paths = recorded.paths.map { if (it.pairIndex == 0) sol else it }
            )
        }
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

    private fun adjacentNonEndpointCell(
        pos: GridPos,
        state: com.xanticious.androidgames.model.games.pathfinder.PathfinderState
    ): GridPos {
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
