package com.xanticious.androidgames.games.bombergrid

import com.xanticious.androidgames.state.games.bombergrid.BomberGridPhase
import com.xanticious.androidgames.state.games.bombergrid.BomberGridStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class BomberGridStateMachineTest {

    private fun machine() = BomberGridStateMachine(CoroutineScope(Dispatchers.Unconfined))

    // ── Boot sequence ────────────────────────────────────────────────────────

    @Test
    fun initialPhase_isIdle() {
        val m = machine()
        assertEquals(BomberGridPhase.IDLE, m.phase.value)
    }

    @Test
    fun startMatch_movesToGeneratingTerrain() {
        val m = machine()
        m.startMatch()
        assertEquals(BomberGridPhase.GENERATING_TERRAIN, m.phase.value)
    }

    @Test
    fun terrainReady_movesToSelectingActor() {
        val m = machine()
        m.startMatch()
        m.terrainReady()
        assertEquals(BomberGridPhase.SELECTING_ACTOR, m.phase.value)
    }

    // ── Turn start ───────────────────────────────────────────────────────────

    @Test
    fun actorSelected_movesToMovePhase() {
        val m = machine()
        m.startMatch()
        m.terrainReady()
        m.actorSelected()
        assertEquals(BomberGridPhase.MOVE_PHASE, m.phase.value)
    }

    // ── Move phase ───────────────────────────────────────────────────────────

    @Test
    fun movementChanged_staysInMovePhase() {
        val m = machine()
        m.startMatch()
        m.terrainReady()
        m.actorSelected()
        m.movementChanged()
        assertEquals(BomberGridPhase.MOVE_PHASE, m.phase.value)
    }

    @Test
    fun movementChanged_multipleTimesInARow_staysInMovePhase() {
        val m = machine()
        m.startMatch()
        m.terrainReady()
        m.actorSelected()
        repeat(3) { m.movementChanged() }
        assertEquals(BomberGridPhase.MOVE_PHASE, m.phase.value)
    }

    @Test
    fun moveConfirmed_movesToAimFirePhase() {
        val m = machine()
        m.startMatch()
        m.terrainReady()
        m.actorSelected()
        m.moveConfirmed()
        assertEquals(BomberGridPhase.AIM_FIRE_PHASE, m.phase.value)
    }

    // ── Aim & fire phase ─────────────────────────────────────────────────────

    @Test
    fun aimChanged_staysInAimFirePhase() {
        val m = machine()
        m.startMatch()
        m.terrainReady()
        m.actorSelected()
        m.moveConfirmed()
        m.aimChanged()
        assertEquals(BomberGridPhase.AIM_FIRE_PHASE, m.phase.value)
    }

    @Test
    fun aimChanged_multipleTimesInARow_staysInAimFirePhase() {
        val m = machine()
        m.startMatch()
        m.terrainReady()
        m.actorSelected()
        m.moveConfirmed()
        repeat(5) { m.aimChanged() }
        assertEquals(BomberGridPhase.AIM_FIRE_PHASE, m.phase.value)
    }

    @Test
    fun bombFired_movesToResolvingExplosion() {
        val m = machine()
        m.startMatch()
        m.terrainReady()
        m.actorSelected()
        m.moveConfirmed()
        m.bombFired()
        assertEquals(BomberGridPhase.RESOLVING_EXPLOSION, m.phase.value)
    }

    // ── Resolution phases ────────────────────────────────────────────────────

    @Test
    fun terrainSettled_movesToResolvingFalls() {
        val m = machine()
        m.startMatch()
        m.terrainReady()
        m.actorSelected()
        m.moveConfirmed()
        m.bombFired()
        m.terrainSettled()
        assertEquals(BomberGridPhase.RESOLVING_FALLS, m.phase.value)
    }

    @Test
    fun characterHit_movesToResolvingFalls() {
        val m = machine()
        m.startMatch()
        m.terrainReady()
        m.actorSelected()
        m.moveConfirmed()
        m.bombFired()
        m.characterHit()
        assertEquals(BomberGridPhase.RESOLVING_FALLS, m.phase.value)
    }

    @Test
    fun turnAdvanced_fromResolvingFalls_movesToSelectingActor() {
        val m = machine()
        m.startMatch()
        m.terrainReady()
        m.actorSelected()
        m.moveConfirmed()
        m.bombFired()
        m.terrainSettled()
        m.turnAdvanced()
        assertEquals(BomberGridPhase.SELECTING_ACTOR, m.phase.value)
    }

    @Test
    fun teamEliminated_fromResolvingFalls_movesToGameOver() {
        val m = machine()
        m.startMatch()
        m.terrainReady()
        m.actorSelected()
        m.moveConfirmed()
        m.bombFired()
        m.terrainSettled()
        m.teamEliminated()
        assertEquals(BomberGridPhase.GAME_OVER, m.phase.value)
    }

    // ── Game over ────────────────────────────────────────────────────────────

    @Test
    fun rematch_fromGameOver_movesToIdle() {
        val m = machine()
        m.startMatch()
        m.terrainReady()
        m.actorSelected()
        m.moveConfirmed()
        m.bombFired()
        m.terrainSettled()
        m.teamEliminated()
        m.rematch()
        assertEquals(BomberGridPhase.IDLE, m.phase.value)
    }

    @Test
    fun goToMenu_fromGameOver_movesToIdle() {
        val m = machine()
        m.startMatch()
        m.terrainReady()
        m.actorSelected()
        m.moveConfirmed()
        m.bombFired()
        m.characterHit()
        m.teamEliminated()
        m.goToMenu()
        assertEquals(BomberGridPhase.IDLE, m.phase.value)
    }

    @Test
    fun fullTurnCycle_viaTerrainSettled() {
        val m = machine()
        m.startMatch()
        m.terrainReady()
        // Turn 1
        m.actorSelected()
        m.moveConfirmed()
        m.bombFired()
        m.terrainSettled()
        m.turnAdvanced()
        // Turn 2
        m.actorSelected()
        m.moveConfirmed()
        m.bombFired()
        m.characterHit()
        m.turnAdvanced()
        assertEquals(BomberGridPhase.SELECTING_ACTOR, m.phase.value)
    }

    @Test
    fun idleAfterRematch_canStartNewMatch() {
        val m = machine()
        m.startMatch()
        m.terrainReady()
        m.actorSelected()
        m.moveConfirmed()
        m.bombFired()
        m.terrainSettled()
        m.teamEliminated()
        m.rematch()
        // Should be back in Idle; start a fresh match
        m.startMatch()
        assertEquals(BomberGridPhase.GENERATING_TERRAIN, m.phase.value)
    }
}
