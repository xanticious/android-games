package com.xanticious.androidgames.games.qix

import com.xanticious.androidgames.controller.games.qix.QixController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.qix.CellState
import com.xanticious.androidgames.model.games.qix.DrawSpeed
import com.xanticious.androidgames.model.games.qix.GridPos
import com.xanticious.androidgames.model.games.qix.PlayerMode
import com.xanticious.androidgames.model.games.qix.QIX_COLS
import com.xanticious.androidgames.model.games.qix.QIX_ROWS
import com.xanticious.androidgames.model.games.qix.QixEntity
import com.xanticious.androidgames.model.games.qix.QixEvent
import com.xanticious.androidgames.model.games.qix.QixInput
import com.xanticious.androidgames.model.games.qix.QixState
import com.xanticious.androidgames.model.games.qix.SparxEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QixControllerTest {

    private val controller = QixController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)

    // ─── configFor ────────────────────────────────────────────────────────────

    @Test
    fun configFor_hard_hasHigherQixSpeedThanEasy() {
        val easy = controller.configFor(GameDifficulty.EASY)
        val hard = controller.configFor(GameDifficulty.HARD)
        assertTrue(hard.qixSpeed > easy.qixSpeed)
    }

    @Test
    fun configFor_hard_hasHigherSparxSpeedThanEasy() {
        val easy = controller.configFor(GameDifficulty.EASY)
        val hard = controller.configFor(GameDifficulty.HARD)
        assertTrue(hard.sparxSpeed > easy.sparxSpeed)
    }

    // ─── claimedPercent ───────────────────────────────────────────────────────

    @Test
    fun claimedPercent_initialState_isZero() {
        assertEquals(0, controller.claimedPercent(QixState.initial()))
    }

    @Test
    fun claimedPercent_allInnerClaimed_isOneHundred() {
        val base = QixState.initial()
        val cells = base.cells.map { if (it == CellState.UNCLAIMED) CellState.CLAIMED else it }
        val state = base.copy(cells = cells)
        assertEquals(100, controller.claimedPercent(state))
    }

    // ─── Player movement on boundary ─────────────────────────────────────────

    @Test
    fun playerOnBoundary_movesRightAlongTopEdge() {
        // Player at (20,0) moving right → lands on (21,0) which is also BOUNDARY.
        val state = QixState.initial().copy(moveTimer = 999f)
        val step = controller.step(state, config, 0.016f, QixInput(dx = 1f, dy = 0f))
        assertEquals(GridPos(21, 0), step.state.playerPos)
        assertEquals(PlayerMode.ON_BOUNDARY, step.state.playerMode)
    }

    @Test
    fun playerOnBoundary_movesIntoUnclaimed_startsDrawingAndMarksTRAIL() {
        // Player at (20,0) moves down → enters interior cell (20,1) = UNCLAIMED.
        val state = QixState.initial().copy(moveTimer = 999f)
        val step = controller.step(state, config, 0.016f, QixInput(dx = 0f, dy = 1f))
        assertEquals(GridPos(20, 1), step.state.playerPos)
        assertEquals(PlayerMode.DRAWING, step.state.playerMode)
        assertEquals(CellState.TRAIL, step.state.cellAt(GridPos(20, 1)))
    }

    @Test
    fun playerDrawing_extendsTrailIntoUnclaimed() {
        // Start drawing from (20,1), then continue down to (20,2).
        val base = QixState.initial()
        val cells = base.cells.toMutableList()
            .also { it[1 * QIX_COLS + 20] = CellState.TRAIL }
        val state = base.copy(
            cells = cells,
            trail = listOf(GridPos(20, 1)),
            playerPos = GridPos(20, 1),
            playerMode = PlayerMode.DRAWING,
            moveTimer = 999f
        )
        val step = controller.step(state, config, 0.016f, QixInput(dx = 0f, dy = 1f))
        assertEquals(GridPos(20, 2), step.state.playerPos)
        assertEquals(CellState.TRAIL, step.state.cellAt(GridPos(20, 2)))
        assertEquals(2, step.state.trail.size)
    }

    // ─── Territory claiming (flood-fill) ─────────────────────────────────────

    /**
     * Trail spans the entire row 15 (cols 1-38).  The player is at (38,15)
     * and steps right into the right boundary at (39,15) → territory is claimed.
     * The Qix sits at (20,5) — upper half.
     * Expected outcome:
     *   - Lower half (rows 16-28) → CLAIMED.
     *   - Trail (row 15) → CLAIMED.
     *   - Upper half (rows 1-14) → UNCLAIMED (Qix side, preserved).
     *   - Event → TerritoryClaimedContinue (50 % < 75 % target).
     */
    @Test
    fun claimTerritory_floodFill_preservesQixSideAndClaimsOther() {
        val base = QixState.initial()
        val trailCells = (1..38).map { GridPos(it, 15) }
        val cells = base.cells.toMutableList()
            .also { list -> trailCells.forEach { p -> list[p.row * QIX_COLS + p.col] = CellState.TRAIL } }
        val state = base.copy(
            cells = cells,
            trail = trailCells,
            playerPos = GridPos(38, 15),
            playerMode = PlayerMode.DRAWING,
            qix = listOf(QixEntity(x = 20f, y = 5f, dirX = 0.8f, dirY = 0.6f)),
            moveTimer = 999f
        )
        // Step right: (38,15) → (39,15) = BOUNDARY → claimTerritory fires.
        val step = controller.step(state, config, 0.016f, QixInput(dx = 1f, dy = 0f))

        assertEquals(PlayerMode.ON_BOUNDARY, step.state.playerMode)
        assertEquals(QixEvent.TerritoryClaimedContinue, step.event)
        // Qix side (upper) stays unclaimed.
        assertEquals(CellState.UNCLAIMED, step.state.cellAt(GridPos(20, 5)))
        // Non-Qix side (lower) is claimed.
        assertEquals(CellState.CLAIMED, step.state.cellAt(GridPos(20, 20)))
        // Trail cells are now claimed.
        assertEquals(CellState.CLAIMED, step.state.cellAt(GridPos(20, 15)))
    }

    @Test
    fun claimTerritory_whenTargetMet_returnsLevelCompleteEvent() {
        // Claim nearly all unclaimed cells so only a thin strip remains for the Qix;
        // then one more claim over the remaining strip should trigger LevelComplete.
        val base = QixState.initial()
        // Pre-claim rows 2-26 completely (leaving rows 1 and 27-28 unclaimed).
        val cells = base.cells.mapIndexed { idx, cell ->
            val row = idx / QIX_COLS
            val col = idx % QIX_COLS
            if (cell == CellState.UNCLAIMED && row in 2..26 && col in 1..38)
                CellState.CLAIMED
            else cell
        }
        // Place a horizontal trail on row 1 (cols 1-38) — Qix squeezed into row 27-28.
        val trailCells = (1..38).map { GridPos(it, 1) }
        val cells2 = cells.toMutableList()
            .also { list -> trailCells.forEach { p -> list[p.row * QIX_COLS + p.col] = CellState.TRAIL } }

        val state = base.copy(
            cells = cells2,
            trail = trailCells,
            playerPos = GridPos(38, 1),
            playerMode = PlayerMode.DRAWING,
            qix = listOf(QixEntity(x = 20f, y = 27f, dirX = 0.8f, dirY = 0.6f)),
            moveTimer = 999f
        )
        val step = controller.step(state, config, 0.016f, QixInput(dx = 1f, dy = 0f))
        assertEquals(QixEvent.TerritoryClaimedLevelComplete, step.event)
    }

    // ─── Qix bounce ───────────────────────────────────────────────────────────

    @Test
    fun qixMovingRight_hitsRightBoundary_reversesXDirection() {
        val state = QixState.initial().copy(
            qix = listOf(QixEntity(x = 38.8f, y = 15f, dirX = 1f, dirY = 0f))
        )
        // dt=0.1 × speed=5 = 0.5 cells, so newX=39.3 → hits boundary cell 39 → bounce.
        val step = controller.step(state, config, 0.1f, QixInput(dx = 0f, dy = 0f))
        assertTrue("Qix x-direction should reverse after boundary bounce",
            step.state.qix.first().dirX < 0f)
    }

    @Test
    fun qixMovingDown_hitsClaimedCell_reversesYDirection() {
        val base = QixState.initial()
        // Claim the row just below the Qix.
        val cells = base.cells.toMutableList()
            .also { it[16 * QIX_COLS + 20] = CellState.CLAIMED }
        val state = base.copy(
            cells = cells,
            qix = listOf(QixEntity(x = 20f, y = 15.8f, dirX = 0f, dirY = 1f))
        )
        val step = controller.step(state, config, 0.1f, QixInput(dx = 0f, dy = 0f))
        assertTrue("Qix y-direction should reverse when hitting a claimed cell",
            step.state.qix.first().dirY < 0f)
    }

    // ─── Collision detection ─────────────────────────────────────────────────

    @Test
    fun qixOnTrailCell_returnsLifeLostEvent() {
        val base = QixState.initial()
        val trailPos = GridPos(20, 15)
        val cells = base.cells.toMutableList()
            .also { it[trailPos.row * QIX_COLS + trailPos.col] = CellState.TRAIL }
        val state = base.copy(
            cells = cells,
            trail = listOf(trailPos),
            playerPos = GridPos(20, 16),
            playerMode = PlayerMode.DRAWING,
            qix = listOf(QixEntity(x = 20.5f, y = 15.5f, dirX = 0.8f, dirY = 0.6f))
        )
        val step = controller.step(state, config, 0.016f, QixInput(dx = 0f, dy = 0f))
        assertEquals(QixEvent.LifeLost, step.event)
    }

    @Test
    fun sparxOnPlayerBoundaryCell_returnsLifeLostEvent() {
        // Initial playerPos = GridPos(20,0). Perimeter index of (20,0) = 20 (top row).
        val state = QixState.initial().copy(
            sparx = listOf(SparxEntity(perimeterIndex = 20f, direction = 1))
        )
        val step = controller.step(state, config, 0.016f, QixInput(dx = 0f, dy = 0f))
        assertEquals(QixEvent.LifeLost, step.event)
    }

    @Test
    fun lifeIsDecremented_whenLifeLostEventFires() {
        val base = QixState.initial()
        val trailPos = GridPos(20, 15)
        val cells = base.cells.toMutableList()
            .also { it[trailPos.row * QIX_COLS + trailPos.col] = CellState.TRAIL }
        val state = base.copy(
            cells = cells,
            trail = listOf(trailPos),
            playerPos = GridPos(20, 16),
            playerMode = PlayerMode.DRAWING,
            qix = listOf(QixEntity(x = 20.5f, y = 15.5f, dirX = 0.8f, dirY = 0.6f))
        )
        val step = controller.step(state, config, 0.016f, QixInput(dx = 0f, dy = 0f))
        assertEquals(2, step.state.lives)  // started with 3, lost one
    }

    @Test
    fun trailClearedAndPlayerRespawned_afterLifeLost() {
        val base = QixState.initial()
        val trailPos = GridPos(20, 15)
        val cells = base.cells.toMutableList()
            .also { it[trailPos.row * QIX_COLS + trailPos.col] = CellState.TRAIL }
        val state = base.copy(
            cells = cells,
            trail = listOf(trailPos),
            playerPos = GridPos(20, 16),
            playerMode = PlayerMode.DRAWING,
            qix = listOf(QixEntity(x = 20.5f, y = 15.5f, dirX = 0.8f, dirY = 0.6f))
        )
        val step = controller.step(state, config, 0.016f, QixInput(dx = 0f, dy = 0f))
        // Trail cell should revert to unclaimed.
        assertEquals(CellState.UNCLAIMED, step.state.cellAt(trailPos))
        // Player back on boundary.
        assertEquals(PlayerMode.ON_BOUNDARY, step.state.playerMode)
        assertTrue("Invincibility should be active after respawn",
            step.state.invincibleTimer > 0f)
    }

    @Test
    fun invincibility_preventsCollisionDamage() {
        val base = QixState.initial()
        val trailPos = GridPos(20, 15)
        val cells = base.cells.toMutableList()
            .also { it[trailPos.row * QIX_COLS + trailPos.col] = CellState.TRAIL }
        val state = base.copy(
            cells = cells,
            trail = listOf(trailPos),
            playerPos = GridPos(20, 16),
            playerMode = PlayerMode.DRAWING,
            qix = listOf(QixEntity(x = 20.5f, y = 15.5f, dirX = 0.8f, dirY = 0.6f)),
            invincibleTimer = 1.0f
        )
        val step = controller.step(state, config, 0.016f, QixInput(dx = 0f, dy = 0f))
        assertEquals(QixEvent.None, step.event)
        assertEquals(3, step.state.lives)  // no life lost
    }

    // ─── Score ───────────────────────────────────────────────────────────────

    @Test
    fun fastDraw_awardsMorePointsThanSlowDraw_forSameArea() {
        fun scoreForDraw(speed: DrawSpeed): Int {
            val base = QixState.initial()
            val trailCells = (1..38).map { GridPos(it, 15) }
            val cells = base.cells.toMutableList()
                .also { list -> trailCells.forEach { p -> list[p.row * QIX_COLS + p.col] = CellState.TRAIL } }
            val state = base.copy(
                cells = cells,
                trail = trailCells,
                playerPos = GridPos(38, 15),
                playerMode = PlayerMode.DRAWING,
                qix = listOf(QixEntity(x = 20f, y = 5f, dirX = 0.8f, dirY = 0.6f)),
                drawSpeed = speed,
                moveTimer = 999f
            )
            return controller.step(state, config, 0.016f, QixInput(dx = 1f, dy = 0f, drawSpeed = speed)).state.score
        }
        assertTrue("Fast draw should score more than slow draw",
            scoreForDraw(DrawSpeed.FAST) > scoreForDraw(DrawSpeed.SLOW))
    }
}
