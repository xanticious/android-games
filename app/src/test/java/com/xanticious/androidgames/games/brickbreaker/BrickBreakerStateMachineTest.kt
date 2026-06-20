package com.xanticious.androidgames.games.brickbreaker

import com.xanticious.androidgames.state.games.brickbreaker.BrickBreakerPhase
import com.xanticious.androidgames.state.games.brickbreaker.BrickBreakerStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class BrickBreakerStateMachineTest {

    private fun machine() = BrickBreakerStateMachine(CoroutineScope(Dispatchers.Unconfined))

    // ---- initial state ----

    @Test
    fun initialPhase_isIdle() {
        val m = machine()
        assertEquals(BrickBreakerPhase.IDLE, m.phase.value)
    }

    // ---- IDLE → LEVEL_START ----

    @Test
    fun levelStarted_movesToLevelStart() {
        val m = machine()
        m.levelStarted()
        assertEquals(BrickBreakerPhase.LEVEL_START, m.phase.value)
    }

    // ---- LEVEL_START → AIM_PHASE (turn-based path) ----

    @Test
    fun readyForAim_fromLevelStart_movesToAimPhase() {
        val m = machine()
        m.levelStarted()
        m.readyForAim()
        assertEquals(BrickBreakerPhase.AIM_PHASE, m.phase.value)
    }

    // ---- LEVEL_START → PLAYING (real-time path) ----

    @Test
    fun readyForPlay_fromLevelStart_movesToPlaying() {
        val m = machine()
        m.levelStarted()
        m.readyForPlay()
        assertEquals(BrickBreakerPhase.PLAYING, m.phase.value)
    }

    // ---- AIM_PHASE → FIRE_PHASE ----

    @Test
    fun fireTapped_fromAimPhase_movesToFirePhase() {
        val m = machine()
        m.levelStarted()
        m.readyForAim()
        m.fireTapped()
        assertEquals(BrickBreakerPhase.FIRE_PHASE, m.phase.value)
    }

    // ---- FIRE_PHASE → RESOLUTION_PHASE ----

    @Test
    fun allBallsLanded_fromFirePhase_movesToResolutionPhase() {
        val m = machine()
        m.levelStarted(); m.readyForAim(); m.fireTapped()
        m.allBallsLanded()
        assertEquals(BrickBreakerPhase.RESOLUTION_PHASE, m.phase.value)
    }

    @Test
    fun clearTapped_fromFirePhase_movesToResolutionPhase() {
        val m = machine()
        m.levelStarted(); m.readyForAim(); m.fireTapped()
        m.clearTapped()
        assertEquals(BrickBreakerPhase.RESOLUTION_PHASE, m.phase.value)
    }

    // ---- RESOLUTION_PHASE → DROP_PHASE (CLASSIC) ----

    @Test
    fun bricksRemain_fromResolution_movesToDropPhase() {
        val m = machine()
        m.levelStarted(); m.readyForAim(); m.fireTapped(); m.allBallsLanded()
        m.bricksRemain()
        assertEquals(BrickBreakerPhase.DROP_PHASE, m.phase.value)
    }

    // ---- DROP_PHASE → AIM_PHASE ----

    @Test
    fun noBricksAtBottom_fromDropPhase_movesToAimPhase() {
        val m = machine()
        m.levelStarted(); m.readyForAim(); m.fireTapped(); m.allBallsLanded(); m.bricksRemain()
        m.noBricksAtBottom()
        assertEquals(BrickBreakerPhase.AIM_PHASE, m.phase.value)
    }

    // ---- DROP_PHASE → GAME_OVER ----

    @Test
    fun bricksAtBottom_fromDropPhase_movesToGameOver() {
        val m = machine()
        m.levelStarted(); m.readyForAim(); m.fireTapped(); m.allBallsLanded(); m.bricksRemain()
        m.bricksAtBottom()
        assertEquals(BrickBreakerPhase.GAME_OVER, m.phase.value)
    }

    // ---- RESOLUTION_PHASE → AIM_PHASE (CANNON: next turn) ----

    @Test
    fun nextTurnReady_fromResolution_movesToAimPhase() {
        val m = machine()
        m.levelStarted(); m.readyForAim(); m.fireTapped(); m.allBallsLanded()
        m.nextTurnReady()
        assertEquals(BrickBreakerPhase.AIM_PHASE, m.phase.value)
    }

    // ---- RESOLUTION_PHASE → GAME_OVER (CANNON: turns exhausted) ----

    @Test
    fun turnsExhausted_fromResolution_movesToGameOver() {
        val m = machine()
        m.levelStarted(); m.readyForAim(); m.fireTapped(); m.allBallsLanded()
        m.turnsExhausted()
        assertEquals(BrickBreakerPhase.GAME_OVER, m.phase.value)
    }

    // ---- RESOLUTION_PHASE → LEVEL_COMPLETE ----

    @Test
    fun fieldCleared_fromResolution_movesToLevelComplete() {
        val m = machine()
        m.levelStarted(); m.readyForAim(); m.fireTapped(); m.allBallsLanded()
        m.fieldCleared()
        assertEquals(BrickBreakerPhase.LEVEL_COMPLETE, m.phase.value)
    }

    @Test
    fun allTargetsDestroyed_fromResolution_movesToLevelComplete() {
        val m = machine()
        m.levelStarted(); m.readyForAim(); m.fireTapped(); m.allBallsLanded()
        m.allTargetsDestroyed()
        assertEquals(BrickBreakerPhase.LEVEL_COMPLETE, m.phase.value)
    }

    // ---- PLAYING → LIFE_LOST ----

    @Test
    fun brickHitBottom_fromPlaying_movesToLifeLost() {
        val m = machine()
        m.levelStarted(); m.readyForPlay()
        m.brickHitBottom()
        assertEquals(BrickBreakerPhase.LIFE_LOST, m.phase.value)
    }

    // ---- LIFE_LOST → PLAYING ----

    @Test
    fun respawnReady_fromLifeLost_movesToPlaying() {
        val m = machine()
        m.levelStarted(); m.readyForPlay(); m.brickHitBottom()
        m.respawnReady()
        assertEquals(BrickBreakerPhase.PLAYING, m.phase.value)
    }

    // ---- LIFE_LOST → GAME_OVER ----

    @Test
    fun allLivesLost_fromLifeLost_movesToGameOver() {
        val m = machine()
        m.levelStarted(); m.readyForPlay(); m.brickHitBottom()
        m.allLivesLost()
        assertEquals(BrickBreakerPhase.GAME_OVER, m.phase.value)
    }

    // ---- PLAYING → GAME_OVER ----

    @Test
    fun allLivesLost_fromPlaying_movesToGameOver() {
        val m = machine()
        m.levelStarted(); m.readyForPlay()
        m.allLivesLost()
        assertEquals(BrickBreakerPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun timerExpired_fromPlaying_movesToGameOver() {
        val m = machine()
        m.levelStarted(); m.readyForPlay()
        m.timerExpired()
        assertEquals(BrickBreakerPhase.GAME_OVER, m.phase.value)
    }

    // ---- PLAYING → LEVEL_COMPLETE ----

    @Test
    fun levelRowsCleared_fromPlaying_movesToLevelComplete() {
        val m = machine()
        m.levelStarted(); m.readyForPlay()
        m.levelRowsCleared()
        assertEquals(BrickBreakerPhase.LEVEL_COMPLETE, m.phase.value)
    }

    @Test
    fun allTargetsDestroyed_fromPlaying_movesToLevelComplete() {
        val m = machine()
        m.levelStarted(); m.readyForPlay()
        m.allTargetsDestroyed()
        assertEquals(BrickBreakerPhase.LEVEL_COMPLETE, m.phase.value)
    }

    @Test
    fun fieldCleared_fromPlaying_movesToLevelComplete() {
        val m = machine()
        m.levelStarted(); m.readyForPlay()
        m.fieldCleared()
        assertEquals(BrickBreakerPhase.LEVEL_COMPLETE, m.phase.value)
    }

    // ---- LEVEL_COMPLETE → LEVEL_START ----

    @Test
    fun nextLevel_fromLevelComplete_movesToLevelStart() {
        val m = machine()
        m.levelStarted(); m.readyForPlay(); m.fieldCleared()
        m.nextLevel()
        assertEquals(BrickBreakerPhase.LEVEL_START, m.phase.value)
    }

    // ---- GAME_OVER → IDLE ----

    @Test
    fun restart_fromGameOver_movesToIdle() {
        val m = machine()
        m.levelStarted(); m.readyForPlay(); m.timerExpired()
        m.restart()
        assertEquals(BrickBreakerPhase.IDLE, m.phase.value)
    }

    // ---- Full turn-based CLASSIC round trip ----

    @Test
    fun classicRound_aimFireDropAim_cyclesCorrectly() {
        val m = machine()
        m.levelStarted()
        assertEquals(BrickBreakerPhase.LEVEL_START, m.phase.value)
        m.readyForAim()
        assertEquals(BrickBreakerPhase.AIM_PHASE, m.phase.value)
        m.fireTapped()
        assertEquals(BrickBreakerPhase.FIRE_PHASE, m.phase.value)
        m.allBallsLanded()
        assertEquals(BrickBreakerPhase.RESOLUTION_PHASE, m.phase.value)
        m.bricksRemain()
        assertEquals(BrickBreakerPhase.DROP_PHASE, m.phase.value)
        m.noBricksAtBottom()
        assertEquals(BrickBreakerPhase.AIM_PHASE, m.phase.value)
    }

    // ---- Full CANNON round (turn used, turns remain) ----

    @Test
    fun cannonTurn_fireAndContinue_cyclesAimPhase() {
        val m = machine()
        m.levelStarted(); m.readyForAim(); m.fireTapped(); m.allBallsLanded()
        m.nextTurnReady()
        assertEquals(BrickBreakerPhase.AIM_PHASE, m.phase.value)
    }

    // ---- Full ARCADE round trip ----

    @Test
    fun arcadeLifeLost_respawn_returnsToPlaying() {
        val m = machine()
        m.levelStarted(); m.readyForPlay(); m.brickHitBottom(); m.respawnReady()
        assertEquals(BrickBreakerPhase.PLAYING, m.phase.value)
    }
}
