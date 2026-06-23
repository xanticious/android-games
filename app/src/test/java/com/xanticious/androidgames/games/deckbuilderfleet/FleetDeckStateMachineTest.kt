package com.xanticious.androidgames.games.deckbuilderfleet

import com.xanticious.androidgames.state.games.deckbuilderfleet.FleetDeckStateMachine
import com.xanticious.androidgames.state.games.deckbuilderfleet.FleetPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class FleetDeckStateMachineTest {

    private fun machine() = FleetDeckStateMachine(CoroutineScope(Dispatchers.Unconfined))

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialPhase_isIdle() {
        val m = machine()
        assertEquals(FleetPhase.IDLE, m.phase.value)
    }

    // ── Setup sequence ────────────────────────────────────────────────────────

    @Test
    fun startMatch_movesToSetup() {
        val m = machine()
        m.startMatch()
        assertEquals(FleetPhase.SETUP, m.phase.value)
    }

    @Test
    fun decksBuilt_movesToTurnStart() {
        val m = machine()
        m.startMatch()
        m.decksBuilt()
        assertEquals(FleetPhase.TURN_START, m.phase.value)
    }

    @Test
    fun handDrawn_movesToPlayerActions() {
        val m = machine()
        m.startMatch()
        m.decksBuilt()
        m.handDrawn()
        assertEquals(FleetPhase.PLAYER_ACTIONS, m.phase.value)
    }

    // ── Turn cycle ────────────────────────────────────────────────────────────

    @Test
    fun turnEnded_movesToOpponentTurn() {
        val m = machine()
        m.startMatch(); m.decksBuilt(); m.handDrawn()
        m.turnEnded()
        assertEquals(FleetPhase.OPPONENT_TURN, m.phase.value)
    }

    @Test
    fun opponentResolved_movesToCheckEnd() {
        val m = machine()
        m.startMatch(); m.decksBuilt(); m.handDrawn()
        m.turnEnded()
        m.opponentResolved()
        assertEquals(FleetPhase.CHECK_END, m.phase.value)
    }

    @Test
    fun continueGame_movesFromCheckEndToTurnStart() {
        val m = machine()
        m.startMatch(); m.decksBuilt(); m.handDrawn()
        m.turnEnded(); m.opponentResolved()
        m.continueGame()
        assertEquals(FleetPhase.TURN_START, m.phase.value)
    }

    @Test
    fun handDrawnAfterContinue_movesToPlayerActions() {
        val m = machine()
        m.startMatch(); m.decksBuilt(); m.handDrawn()
        m.turnEnded(); m.opponentResolved(); m.continueGame()
        m.handDrawn()
        assertEquals(FleetPhase.PLAYER_ACTIONS, m.phase.value)
    }

    // ── Game over paths ───────────────────────────────────────────────────────

    @Test
    fun opponentHealthZero_movesToVictory() {
        val m = machine()
        m.startMatch(); m.decksBuilt(); m.handDrawn()
        m.turnEnded(); m.opponentResolved()
        m.opponentHealthZero()
        assertEquals(FleetPhase.VICTORY, m.phase.value)
    }

    @Test
    fun playerHealthZero_movesToDefeat() {
        val m = machine()
        m.startMatch(); m.decksBuilt(); m.handDrawn()
        m.turnEnded(); m.opponentResolved()
        m.playerHealthZero()
        assertEquals(FleetPhase.DEFEAT, m.phase.value)
    }

    // ── Rematch ───────────────────────────────────────────────────────────────

    @Test
    fun rematch_fromVictory_returnsToIdle() {
        val m = machine()
        m.startMatch(); m.decksBuilt(); m.handDrawn()
        m.turnEnded(); m.opponentResolved(); m.opponentHealthZero()
        m.rematch()
        assertEquals(FleetPhase.IDLE, m.phase.value)
    }

    @Test
    fun rematch_fromDefeat_returnsToIdle() {
        val m = machine()
        m.startMatch(); m.decksBuilt(); m.handDrawn()
        m.turnEnded(); m.opponentResolved(); m.playerHealthZero()
        m.rematch()
        assertEquals(FleetPhase.IDLE, m.phase.value)
    }
}
