package com.xanticious.androidgames.games.bubblespop

import com.xanticious.androidgames.controller.games.bubblespop.BubblesPopController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.bubblespop.BubbleColor
import com.xanticious.androidgames.model.games.bubblespop.BubbleType
import com.xanticious.androidgames.model.games.bubblespop.BubblesGridEvent
import com.xanticious.androidgames.model.games.bubblespop.BubblesVariant
import com.xanticious.androidgames.model.games.bubblespop.ChainBubble
import com.xanticious.androidgames.model.games.bubblespop.FlyingBubble
import com.xanticious.androidgames.model.games.bubblespop.GridCell
import kotlin.math.PI
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BubblesPopControllerTest {

    private val controller = BubblesPopController()
    private val rng = Random(42L)

    // ─── configFor ───────────────────────────────────────────────────────────

    @Test
    fun configFor_easyTurnBased_hasFiveColors() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.TURN_BASED)
        assertEquals(5, config.colorsInPlay)
    }

    @Test
    fun configFor_hardTurnBased_hasSevenColors() {
        val config = controller.configFor(GameDifficulty.HARD, BubblesVariant.TURN_BASED)
        assertEquals(7, config.colorsInPlay)
    }

    @Test
    fun configFor_turnBased_hasOneLive() {
        val config = controller.configFor(GameDifficulty.MEDIUM, BubblesVariant.TURN_BASED)
        assertEquals(1, config.initialLives)
    }

    @Test
    fun configFor_arcade_hasThreeLives() {
        val config = controller.configFor(GameDifficulty.MEDIUM, BubblesVariant.ARCADE)
        assertEquals(3, config.initialLives)
    }

    @Test
    fun configFor_arcade_hasNonZeroCooldown() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.ARCADE)
        assertTrue(config.cannonCooldown > 0f)
    }

    @Test
    fun configFor_turnBased_hasZeroCooldown() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.TURN_BASED)
        assertEquals(0f, config.cannonCooldown, 1e-6f)
    }

    // ─── cellPosition ────────────────────────────────────────────────────────

    @Test
    fun cellPosition_evenRow_centerIsHalfColWidth() {
        val pos = controller.cellPosition(0, 0, 0f)
        val expected = 0.5f / BubblesPopController.GRID_COLS
        assertEquals(expected, pos.x, 1e-5f)
    }

    @Test
    fun cellPosition_oddRow_centerIsOffsetByHalfColumn() {
        val pos = controller.cellPosition(0, 1, 0f)
        val expected = 1.0f / BubblesPopController.GRID_COLS
        assertEquals(expected, pos.x, 1e-5f)
    }

    @Test
    fun cellPosition_topOffset_shiftsYDown() {
        val offset = 0.1f
        val pos = controller.cellPosition(0, 0, offset)
        assertTrue(pos.y > offset)
    }

    // ─── hexNeighbors ────────────────────────────────────────────────────────

    @Test
    fun hexNeighbors_evenRow_returnsSixNeighbors() {
        assertEquals(6, controller.hexNeighbors(3, 2).size)
    }

    @Test
    fun hexNeighbors_oddRow_returnsSixNeighbors() {
        assertEquals(6, controller.hexNeighbors(3, 1).size)
    }

    @Test
    fun hexNeighbors_evenRow_includesAboveLeft() {
        val nbs = controller.hexNeighbors(4, 2)
        assertTrue(nbs.contains(3 to 1))
    }

    @Test
    fun hexNeighbors_oddRow_includesAboveRight() {
        val nbs = controller.hexNeighbors(4, 1)
        assertTrue(nbs.contains(5 to 0))
    }

    // ─── maxCols ─────────────────────────────────────────────────────────────

    @Test
    fun maxCols_evenRow_equalsGridCols() {
        assertEquals(BubblesPopController.GRID_COLS, controller.maxCols(0))
    }

    @Test
    fun maxCols_oddRow_equalsGridColsMinusOne() {
        assertEquals(BubblesPopController.GRID_COLS - 1, controller.maxCols(1))
    }

    // ─── findCluster ─────────────────────────────────────────────────────────

    @Test
    fun findCluster_singleCell_returnsItself() {
        val grid = mapOf((3 to 2) to GridCell(3, 2, BubbleColor.CYAN))
        val cluster = controller.findCluster(grid, 3 to 2)
        assertEquals(setOf(3 to 2), cluster)
    }

    @Test
    fun findCluster_threeConnectedSameColor_returnsAll() {
        val grid = mapOf(
            (0 to 0) to GridCell(0, 0, BubbleColor.RED),
            (1 to 0) to GridCell(1, 0, BubbleColor.RED),
            (2 to 0) to GridCell(2, 0, BubbleColor.RED),
        )
        val cluster = controller.findCluster(grid, 0 to 0)
        assertEquals(3, cluster.size)
    }

    @Test
    fun findCluster_differentColors_stopsAtBoundary() {
        val grid = mapOf(
            (0 to 0) to GridCell(0, 0, BubbleColor.RED),
            (1 to 0) to GridCell(1, 0, BubbleColor.CYAN),
        )
        val cluster = controller.findCluster(grid, 0 to 0)
        assertEquals(1, cluster.size)
    }

    @Test
    fun findCluster_stoneBubble_returnsSingletonRegardlessOfNeighbors() {
        val grid = mapOf(
            (0 to 0) to GridCell(0, 0, BubbleColor.RED, BubbleType.STONE),
            (1 to 0) to GridCell(1, 0, BubbleColor.RED),
        )
        val cluster = controller.findCluster(grid, 0 to 0)
        assertEquals(1, cluster.size)
        assertTrue(cluster.contains(0 to 0))
    }

    @Test
    fun findCluster_rainbowBubble_expandsToAllNeighbors() {
        val grid = mapOf(
            (0 to 0) to GridCell(0, 0, BubbleColor.RED, BubbleType.RAINBOW),
            (1 to 0) to GridCell(1, 0, BubbleColor.CYAN),
            (2 to 0) to GridCell(2, 0, BubbleColor.YELLOW),
        )
        val cluster = controller.findCluster(grid, 0 to 0)
        assertTrue(cluster.size > 1)
    }

    // ─── findDisconnected ────────────────────────────────────────────────────

    @Test
    fun findDisconnected_allCellsInRow0_returnsEmpty() {
        val grid = mapOf(
            (0 to 0) to GridCell(0, 0, BubbleColor.RED),
            (1 to 0) to GridCell(1, 0, BubbleColor.CYAN),
        )
        assertTrue(controller.findDisconnected(grid).isEmpty())
    }

    @Test
    fun findDisconnected_unconnectedLowerCell_returnsIt() {
        // Cell at row 2 with no connection to row 0
        val grid = mapOf(
            (5 to 2) to GridCell(5, 2, BubbleColor.RED),
        )
        val disconnected = controller.findDisconnected(grid)
        assertEquals(setOf(5 to 2), disconnected)
    }

    @Test
    fun findDisconnected_chainFromRow0_allConnected() {
        val grid = mapOf(
            (0 to 0) to GridCell(0, 0, BubbleColor.RED),
            // Even row 0, col 0 → neighbor at col 0, row 1 is (0, 1) for even row
            (0 to 1) to GridCell(0, 1, BubbleColor.CYAN),
        )
        assertTrue(controller.findDisconnected(grid).isEmpty())
    }

    // ─── calculatePopScore ───────────────────────────────────────────────────

    @Test
    fun calculatePopScore_three_returns50() {
        assertEquals(50, controller.calculatePopScore(3))
    }

    @Test
    fun calculatePopScore_four_returns100() {
        assertEquals(100, controller.calculatePopScore(4))
    }

    @Test
    fun calculatePopScore_five_returns150() {
        assertEquals(150, controller.calculatePopScore(5))
    }

    @Test
    fun calculatePopScore_six_returns180() {
        assertEquals(180, controller.calculatePopScore(6))
    }

    @Test
    fun calculatePopScore_zero_returnsZero() {
        assertEquals(0, controller.calculatePopScore(0))
    }

    // ─── fireCannon ──────────────────────────────────────────────────────────

    @Test
    fun fireCannon_noFlyingNoCooldown_setsFlyingBubble() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.TURN_BASED)
        val state = controller.initialGridState(config, rng)
        val fired = controller.fireCannon(state, 0f)
        assertNotNull(fired.flying)
    }

    @Test
    fun fireCannon_straightUp_flyingHasNegativeDy() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.TURN_BASED)
        val state = controller.initialGridState(config, rng)
        val fired = controller.fireCannon(state, 0f)
        assertTrue(fired.flying!!.dy < 0f)
    }

    @Test
    fun fireCannon_withCooldown_doesNotFire() {
        val config = controller.configFor(GameDifficulty.MEDIUM, BubblesVariant.ARCADE)
        val state = controller.initialGridState(config, rng).copy(cannonCooldown = 0.3f)
        val result = controller.fireCannon(state, 0f)
        assertNull(result.flying)
    }

    @Test
    fun fireCannon_alreadyFlying_doesNotRefire() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.TURN_BASED)
        val state = controller.initialGridState(config, rng)
        val fired = controller.fireCannon(state, 0f)
        val refired = controller.fireCannon(fired, (PI / 6).toFloat())
        assertEquals(fired.flying, refired.flying) // unchanged
    }

    // ─── stepGrid: flying bubble movement ────────────────────────────────────

    @Test
    fun stepGrid_withFlyingBubble_advancesPosition() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.TURN_BASED)
        var state = controller.initialGridState(config, rng)
        state = controller.fireCannon(state, 0f)
        val before = state.flying!!.y
        val (after, _) = controller.stepGrid(state, config, 0.016f, rng)
        assertTrue(after.flying!!.y < before) // moved upward
    }

    @Test
    fun stepGrid_flyingBubbleBounceOffLeftWall_reversesDx() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.TURN_BASED)
        val state = controller.initialGridState(config, rng).copy(
            flying = FlyingBubble(
                x = BubblesPopController.BUBBLE_RADIUS * 0.5f,
                y = 0.5f,
                dx = -BubblesPopController.BUBBLE_SPEED,
                dy = 0f,
                color = BubbleColor.CYAN,
            )
        )
        val (after, _) = controller.stepGrid(state, config, 0.016f, rng)
        if (after.flying != null) assertTrue(after.flying.dx > 0f)
    }

    @Test
    fun stepGrid_emptyGrid_noMatchEvent() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.TURN_BASED)
        val state = controller.initialGridState(config, rng).copy(grid = emptyMap(), flying = null)
        val (_, event) = controller.stepGrid(state, config, 0.016f, rng)
        assertEquals(BubblesGridEvent.None, event)
    }

    @Test
    fun stepGrid_noFlyingBubble_returnsNoneEvent() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.TURN_BASED)
        val state = controller.initialGridState(config, rng)
        // Ensure no flying bubble
        val noFlying = state.copy(flying = null, grid = emptyMap())
        val (_, event) = controller.stepGrid(noFlying, config, 0.016f, rng)
        assertEquals(BubblesGridEvent.None, event)
    }

    // ─── generateGrid ────────────────────────────────────────────────────────

    @Test
    fun generateGrid_turnBasedLevelOne_generatesOneRow() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.TURN_BASED)
        val grid = controller.generateGrid(config, 1, rng)
        val rows = grid.keys.map { it.second }.toSet()
        assertEquals(1, rows.size)
    }

    @Test
    fun generateGrid_usesOnlyConfigColors() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.TURN_BASED)
        val grid = controller.generateGrid(config, 1, rng)
        val usedColors = grid.values.filter { it.type == BubbleType.NORMAL }.map { it.color }.toSet()
        assertTrue(usedColors.size <= config.colorsInPlay)
    }

    // ─── Track / snake helpers ────────────────────────────────────────────────

    @Test
    fun trackLength_isPositive() {
        assertTrue(controller.trackLength() > 0f)
    }

    @Test
    fun trackPosition_zero_returnsFirstWaypoint() {
        val pos = controller.trackPosition(0f)
        val first = BubblesPopController.TRACK_WAYPOINTS.first()
        assertEquals(first.x, pos.x, 1e-5f)
        assertEquals(first.y, pos.y, 1e-5f)
    }

    @Test
    fun trackPosition_totalLength_returnsLastWaypoint() {
        val len = controller.trackLength()
        val pos = controller.trackPosition(len)
        val last = BubblesPopController.TRACK_WAYPOINTS.last()
        assertEquals(last.x, pos.x, 1e-4f)
        assertEquals(last.y, pos.y, 1e-4f)
    }

    @Test
    fun trackPosition_midpoint_isInsideBounds() {
        val pos = controller.trackPosition(controller.trackLength() / 2f)
        assertTrue(pos.x in 0f..1f)
        assertTrue(pos.y in 0f..1f)
    }

    // ─── findChainMatch ──────────────────────────────────────────────────────

    @Test
    fun findChainMatch_threeConsecutiveSameColor_returnsFullRange() {
        val chain = listOf(
            ChainBubble(0f, BubbleColor.RED),
            ChainBubble(0.1f, BubbleColor.RED),
            ChainBubble(0.2f, BubbleColor.RED),
        )
        val match = controller.findChainMatch(chain, 1)
        assertEquals(0..2, match)
    }

    @Test
    fun findChainMatch_onlyTwoSameColor_returnsNull() {
        val chain = listOf(
            ChainBubble(0f, BubbleColor.RED),
            ChainBubble(0.1f, BubbleColor.RED),
            ChainBubble(0.2f, BubbleColor.CYAN),
        )
        assertNull(controller.findChainMatch(chain, 0))
    }

    @Test
    fun findChainMatch_stoneInMiddle_isNotIncluded() {
        val chain = listOf(
            ChainBubble(0f, BubbleColor.RED),
            ChainBubble(0.1f, BubbleColor.RED, BubbleType.STONE),
            ChainBubble(0.2f, BubbleColor.RED),
        )
        assertNull(controller.findChainMatch(chain, 1))
    }

    @Test
    fun findChainMatch_emptyChain_returnsNull() {
        assertNull(controller.findChainMatch(emptyList(), 0))
    }

    // ─── contractChain ───────────────────────────────────────────────────────

    @Test
    fun contractChain_emptyInput_returnsEmpty() {
        assertTrue(controller.contractChain(emptyList()).isEmpty())
    }

    @Test
    fun contractChain_preservesOrder() {
        val chain = listOf(
            ChainBubble(0.5f, BubbleColor.RED),
            ChainBubble(0.1f, BubbleColor.CYAN),
        )
        val contracted = controller.contractChain(chain)
        assertEquals(BubbleColor.CYAN, contracted[0].color)
        assertEquals(BubbleColor.RED, contracted[1].color)
    }

    @Test
    fun contractChain_firstBubbleHasZeroT() {
        val chain = listOf(
            ChainBubble(3f, BubbleColor.RED),
            ChainBubble(5f, BubbleColor.CYAN),
        )
        val contracted = controller.contractChain(chain)
        assertEquals(0f, contracted[0].t, 1e-5f)
    }

    // ─── generateChain ───────────────────────────────────────────────────────

    @Test
    fun generateChain_producesConfiguredLength() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.SNAKE_ARCADE)
        val chain = controller.generateChain(config, 1, rng)
        assertEquals(config.chainLength, chain.size)
    }

    @Test
    fun generateChain_bubblesHaveIncreasingT() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.SNAKE_ARCADE)
        val chain = controller.generateChain(config, 1, rng)
        for (i in 1 until chain.size) {
            assertTrue(chain[i].t > chain[i - 1].t)
        }
    }

    // ─── swapBubbles ─────────────────────────────────────────────────────────

    @Test
    fun swapBubbles_withSwapsRemaining_exchangesCannon() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.SNAKE_ARCADE)
        val state = controller.initialSnakeState(config, rng)
        val before = state.cannonBubble
        val swapped = controller.swapBubbles(state)
        assertEquals(before, swapped.nextBubble)
    }

    @Test
    fun swapBubbles_noSwapsRemaining_doesNotSwap() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.SNAKE_ARCADE)
        val state = controller.initialSnakeState(config, rng).copy(swapRemaining = 0)
        val swapped = controller.swapBubbles(state)
        assertEquals(state.cannonBubble, swapped.cannonBubble)
    }

    // ─── rowsForLevel (requirement 7) ─────────────────────────────────────────

    @Test
    fun rowsForLevel_turnBasedLevelOne_returnsOne() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.TURN_BASED)
        assertEquals(1, controller.rowsForLevel(config, 1))
    }

    @Test
    fun rowsForLevel_turnBasedLevelThree_returnsThree() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.TURN_BASED)
        assertEquals(3, controller.rowsForLevel(config, 3))
    }

    @Test
    fun rowsForLevel_turnBasedBeyondMax_capsAtTen() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.TURN_BASED)
        assertEquals(BubblesPopController.MAX_TURN_BASED_ROWS, controller.rowsForLevel(config, 25))
    }

    @Test
    fun rowsForLevel_arcade_usesStartingRows() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.ARCADE)
        assertEquals(config.startingRows, controller.rowsForLevel(config, 7))
    }

    // ─── partial rows (requirement 6) ─────────────────────────────────────────

    @Test
    fun generateGrid_someRowIsNotCompletelyFull() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.TURN_BASED)
        val grid = controller.generateGrid(config, BubblesPopController.MAX_TURN_BASED_ROWS, kotlin.random.Random(7))
        val full = (0 until BubblesPopController.MAX_TURN_BASED_ROWS).sumOf { controller.maxCols(it) }
        assertTrue(grid.size < full)
    }

    @Test
    fun generateGrid_everyRowHasAtLeastOneBubble() {
        val config = controller.configFor(GameDifficulty.MEDIUM, BubblesVariant.TURN_BASED)
        val grid = controller.generateGrid(config, 5, kotlin.random.Random(3))
        val rows = grid.keys.map { it.second }.toSet()
        assertEquals(5, rows.size)
    }

    // ─── aspect-aware aim (requirement 5) ─────────────────────────────────────

    @Test
    fun fireCannon_wideAspect_scalesVerticalVelocity() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.TURN_BASED)
        val state = controller.initialGridState(config, rng).copy(aspect = 2f)
        val fired = controller.fireCannon(state, 0f)
        assertEquals(-BubblesPopController.BUBBLE_SPEED * 2f, fired.flying!!.dy, 1e-4f)
    }

    // ─── snake launcher + slither-in (requirements 11, 13) ────────────────────

    @Test
    fun moveLauncher_clampsWithinBounds() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.SNAKE_ARCADE)
        val state = controller.initialSnakeState(config, rng)
        val moved = controller.moveLauncher(state, 5f)
        assertTrue(moved.launcherX <= 1f - BubblesPopController.BUBBLE_RADIUS)
    }

    @Test
    fun initialSnakeState_startsWithEmptyChain() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.SNAKE_ARCADE)
        val state = controller.initialSnakeState(config, rng)
        assertTrue(state.chain.isEmpty())
    }

    @Test
    fun initialSnakeState_spawnRemainingEqualsChainLength() {
        val config = controller.configFor(GameDifficulty.EASY, BubblesVariant.SNAKE_ARCADE)
        val state = controller.initialSnakeState(config, rng)
        assertEquals(config.chainLength, state.spawnRemaining)
    }
}
