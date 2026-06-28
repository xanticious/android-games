package com.xanticious.androidgames.games.mahjongsolitaire

import com.xanticious.androidgames.controller.games.mahjongsolitaire.MahjongSolitaireController
import com.xanticious.androidgames.model.games.mahjongsolitaire.MahjongLayout
import com.xanticious.androidgames.model.games.mahjongsolitaire.TileFace
import com.xanticious.androidgames.model.games.mahjongsolitaire.TileSlot
import com.xanticious.androidgames.model.games.mahjongsolitaire.TileSuit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MahjongSolitaireControllerTest {

    private val controller = MahjongSolitaireController()

    // ── Tile set ──────────────────────────────────────────────────────────────

    @Test
    fun standardTileSet_has144Tiles() {
        assertEquals(144, controller.standardTileSet().size)
    }

    @Test
    fun standardTileSet_has72MatchingPairs() {
        val groups = controller.standardTileSet().groupBy { it.matchGroup }
        // Every group must have an even count (can form pairs)
        groups.values.forEach { tiles ->
            assertEquals(0, tiles.size % 2)
        }
        val totalPairs = groups.values.sumOf { it.size / 2 }
        assertEquals(72, totalPairs)
    }

    @Test
    fun turtleLayout_has144Slots() {
        assertEquals(144, controller.layoutPositions(MahjongLayout.TURTLE).size)
    }

    @Test
    fun pyramidLayout_has144Slots() {
        assertEquals(144, controller.layoutPositions(MahjongLayout.PYRAMID).size)
    }

    @Test
    fun fortressLayout_has144Slots() {
        assertEquals(144, controller.layoutPositions(MahjongLayout.FORTRESS).size)
    }

    @Test
    fun dragonLayout_has144Slots() {
        assertEquals(144, controller.layoutPositions(MahjongLayout.DRAGON).size)
    }

    // ── Freedom check ─────────────────────────────────────────────────────────

    @Test
    fun isFree_topUncoveredSideOpen_returnsTrue() {
        // Single tile in isolation — nothing on top, both sides open.
        val slot = TileSlot(0, 4, 4, 0, TileFace(TileSuit.DOTS, 1))
        assertTrue(controller.isFree(slot, listOf(slot)))
    }

    @Test
    fun isFree_coveredFromAbove_returnsFalse() {
        val bottom = TileSlot(0, 4, 4, 0, TileFace(TileSuit.DOTS, 1))
        val top = TileSlot(1, 4, 4, 1, TileFace(TileSuit.DOTS, 2))
        // 'bottom' is covered because 'top' is at layer+1 with |Δx|=0<2 and |Δy|=0<2
        assertFalse(controller.isFree(bottom, listOf(bottom, top)))
    }

    @Test
    fun isFree_bothSidesBlocked_returnsFalse() {
        val left = TileSlot(0, 2, 4, 0, TileFace(TileSuit.DOTS, 1))
        val mid = TileSlot(1, 4, 4, 0, TileFace(TileSuit.DOTS, 2))
        val right = TileSlot(2, 6, 4, 0, TileFace(TileSuit.DOTS, 3))
        // mid: left blocked by (2,4) → left.x+2=4=mid.x; right blocked by (6,4)
        assertFalse(controller.isFree(mid, listOf(left, mid, right)))
    }

    @Test
    fun isFree_oneSideOpen_returnsTrue() {
        val left = TileSlot(0, 2, 4, 0, TileFace(TileSuit.DOTS, 1))
        val mid = TileSlot(1, 4, 4, 0, TileFace(TileSuit.DOTS, 2))
        // mid: left blocked by (2,4), right side OPEN → free
        assertTrue(controller.isFree(mid, listOf(left, mid)))
    }

    // ── Matching ──────────────────────────────────────────────────────────────

    @Test
    fun canMatch_identicalFaces_returnsTrue() {
        val a = TileFace(TileSuit.DOTS, 5)
        val b = TileFace(TileSuit.DOTS, 5)
        assertTrue(controller.canMatch(a, b))
    }

    @Test
    fun canMatch_differentRankSameSuit_returnsFalse() {
        assertFalse(controller.canMatch(TileFace(TileSuit.DOTS, 3), TileFace(TileSuit.DOTS, 4)))
    }

    @Test
    fun canMatch_flowerMatchesFlower() {
        val f1 = TileFace(TileSuit.FLOWER, 1)
        val f2 = TileFace(TileSuit.FLOWER, 3)
        assertTrue(controller.canMatch(f1, f2))
    }

    @Test
    fun canMatch_flowerDoesNotMatchWind() {
        assertFalse(
            controller.canMatch(TileFace(TileSuit.FLOWER, 1), TileFace(TileSuit.WIND, 1))
        )
    }

    @Test
    fun canMatch_seasonMatchesSeason() {
        assertTrue(
            controller.canMatch(TileFace(TileSuit.SEASON, 2), TileFace(TileSuit.SEASON, 4))
        )
    }

    @Test
    fun canMatch_seasonDoesNotMatchFlower() {
        assertFalse(
            controller.canMatch(TileFace(TileSuit.SEASON, 1), TileFace(TileSuit.FLOWER, 1))
        )
    }

    // ── New game ──────────────────────────────────────────────────────────────

    @Test
    fun newGame_turtleLayout_has144ActiveTiles() {
        val state = controller.newGame(MahjongLayout.TURTLE, Random(42))
        assertEquals(144, state.activeTiles.size)
    }

    @Test
    fun newGame_sameSeed_producesIdenticalDeal() {
        val a = controller.newGame(MahjongLayout.TURTLE, Random(99))
        val b = controller.newGame(MahjongLayout.TURTLE, Random(99))
        assertEquals(a.slots.map { it.face }, b.slots.map { it.face })
    }

    @Test
    fun newGame_differentSeeds_producesDifferentDeals() {
        val a = controller.newGame(MahjongLayout.TURTLE, Random(1))
        val b = controller.newGame(MahjongLayout.TURTLE, Random(2))
        assertFalse(a.slots.map { it.face } == b.slots.map { it.face })
    }

    @Test
    fun newGame_guaranteedSolvable_hasAvailableMatchesAtStart() {
        val state = controller.newGame(MahjongLayout.TURTLE, Random(7), guaranteedSolvable = true)
        assertTrue(controller.availableMatches(state).isNotEmpty())
    }

    @Test
    fun newGame_guaranteedSolvable_canBeFullyClearedGreedily() {
        // Verify that following the reverse-solve solution path clears the board.
        // The reverse-solve guarantees exactly this sequence works (step N's pair is
        // free after removing all previous pairs), so we follow it directly.
        var state = controller.newGame(MahjongLayout.TURTLE, Random(42), guaranteedSolvable = true)
        assertEquals("Solution path should have 72 steps", 72, state.solutionPath.size)
        for ((idA, idB) in state.solutionPath) {
            state = controller.tryMatch(controller.selectTile(state, idA), idB)
        }
        assertTrue("Board was not solved after following solution path", controller.isSolved(state))
    }

    // ── selectTile / tryMatch ─────────────────────────────────────────────────

    @Test
    fun selectTile_freeSlot_setsSelectedId() {
        val state = controller.newGame(MahjongLayout.TURTLE, Random(42))
        val freeTile = controller.availableMatches(state).first().first
        val next = controller.selectTile(state, freeTile.id)
        assertEquals(freeTile.id, next.selectedId)
    }

    @Test
    fun selectTile_alreadySelected_deselects() {
        val state = controller.newGame(MahjongLayout.TURTLE, Random(42))
        val freeTile = controller.availableMatches(state).first().first
        val selected = controller.selectTile(state, freeTile.id)
        val deselected = controller.selectTile(selected, freeTile.id)
        assertNull(deselected.selectedId)
    }

    @Test
    fun tryMatch_matchingFreePair_removesBothTiles() {
        val state = controller.newGame(MahjongLayout.TURTLE, Random(42))
        val (a, b) = controller.availableMatches(state).first()
        val after = controller.tryMatch(controller.selectTile(state, a.id), b.id)
        assertNull(after.slots.first { it.id == a.id }.face)
        assertNull(after.slots.first { it.id == b.id }.face)
        assertEquals(1, after.pairsRemoved)
    }

    @Test
    fun tryMatch_mismatch_clearsSelection() {
        // Build a minimal 4-tile state with two non-matching free tiles
        val slotA = TileSlot(0, 0, 4, 0, TileFace(TileSuit.DOTS, 1))
        val slotB = TileSlot(1, 4, 4, 0, TileFace(TileSuit.BAMBOO, 2))
        val slotC = TileSlot(2, 8, 4, 0, TileFace(TileSuit.DOTS, 1))
        val slotD = TileSlot(3, 12, 4, 0, TileFace(TileSuit.BAMBOO, 2))
        val state = com.xanticious.androidgames.model.games.mahjongsolitaire.MahjongSolitaireState(
            slots = listOf(slotA, slotB, slotC, slotD)
        )
        // Select A (DOTS 1), then tap B (BAMBOO 2) — mismatch
        val withSelection = state.copy(selectedId = slotA.id)
        val after = controller.tryMatch(withSelection, slotB.id)
        assertNull(after.selectedId)
        // Both tiles still present
        assertNotNull(after.slots.first { it.id == slotA.id }.face)
        assertNotNull(after.slots.first { it.id == slotB.id }.face)
    }

    // ── Undo ─────────────────────────────────────────────────────────────────

    @Test
    fun undo_restoresMostRecentlyRemovedPair() {
        val state = controller.newGame(MahjongLayout.TURTLE, Random(42))
        val (a, b) = controller.availableMatches(state).first()
        val afterMatch = controller.tryMatch(controller.selectTile(state, a.id), b.id)
        val restored = controller.undo(afterMatch)
        assertNotNull(restored.slots.first { it.id == a.id }.face)
        assertNotNull(restored.slots.first { it.id == b.id }.face)
        assertEquals(state.activeTiles.size, restored.activeTiles.size)
    }

    @Test
    fun undo_onEmptyHistory_returnsUnchangedState() {
        val state = controller.newGame(MahjongLayout.TURTLE, Random(42))
        val unchanged = controller.undo(state)
        assertEquals(state.slots, unchanged.slots)
    }

    // ── Solve / stuck ─────────────────────────────────────────────────────────

    @Test
    fun isSolved_emptyBoard_returnsTrue() {
        val state = controller.newGame(MahjongLayout.TURTLE, Random(42))
        val cleared = state.copy(slots = state.slots.map { it.copy(face = null) })
        assertTrue(controller.isSolved(cleared))
    }

    @Test
    fun isStuck_withAvailableMatch_returnsFalse() {
        val state = controller.newGame(MahjongLayout.TURTLE, Random(42))
        assertFalse(controller.isStuck(state))
    }

    // ── Hint ─────────────────────────────────────────────────────────────────

    @Test
    fun showHint_setsHintIds() {
        val state = controller.newGame(MahjongLayout.TURTLE, Random(42))
        val hinted = controller.showHint(state)
        assertNotNull(hinted.hintIds)
    }
}
