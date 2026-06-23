package com.xanticious.androidgames.games.piratetreasuremaze

import com.xanticious.androidgames.state.games.piratetreasuremaze.PirateTreasureMazePhase
import com.xanticious.androidgames.state.games.piratetreasuremaze.PirateTreasureMazeStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class PirateTreasureMazeStateMachineTest {
    private fun machine() = PirateTreasureMazeStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun startGame_movesToSettings() {
        val machine = machine()
        machine.startGame()
        assertEquals(PirateTreasureMazePhase.SETTINGS, machine.phase.value)
    }

    @Test
    fun mazeReady_afterSettings_movesToPlaying() {
        val machine = machine()
        machine.startGame()
        machine.confirmSettings()
        machine.mazeReady()
        assertEquals(PirateTreasureMazePhase.PLAYING, machine.phase.value)
    }

    @Test
    fun mazeCompleted_whilePlaying_movesToComplete() {
        val machine = machine()
        machine.startGame()
        machine.confirmSettings()
        machine.mazeReady()
        machine.mazeCompleted()
        assertEquals(PirateTreasureMazePhase.MAZE_COMPLETE, machine.phase.value)
    }

    @Test
    fun newMaze_afterComplete_movesToGenerating() {
        val machine = machine()
        machine.startGame()
        machine.confirmSettings()
        machine.mazeReady()
        machine.mazeCompleted()
        machine.newMaze()
        assertEquals(PirateTreasureMazePhase.GENERATING, machine.phase.value)
    }
}
