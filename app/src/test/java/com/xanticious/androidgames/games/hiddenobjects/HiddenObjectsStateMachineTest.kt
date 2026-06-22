package com.xanticious.androidgames.games.hiddenobjects

import com.xanticious.androidgames.state.games.hiddenobjects.HiddenObjectsPhase
import com.xanticious.androidgames.state.games.hiddenobjects.HiddenObjectsStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class HiddenObjectsStateMachineTest {
    private fun machine() = HiddenObjectsStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun startGame_movesToSetup() {
        val m = machine()
        m.startGame()
        assertEquals(HiddenObjectsPhase.SETUP, m.phase.value)
    }

    @Test
    fun sceneReady_movesToPlaying() {
        val m = machine()
        m.startGame()
        m.confirmSettings()
        m.sceneReady()
        assertEquals(HiddenObjectsPhase.PLAYING, m.phase.value)
    }

    @Test
    fun hintRequested_movesToHintCooldown() {
        val m = machine()
        m.startGame()
        m.confirmSettings()
        m.sceneReady()
        m.hintRequested()
        assertEquals(HiddenObjectsPhase.HINT_COOLDOWN, m.phase.value)
    }

    @Test
    fun allObjectsFound_movesToSceneComplete() {
        val m = machine()
        m.startGame()
        m.confirmSettings()
        m.sceneReady()
        m.allObjectsFound()
        assertEquals(HiddenObjectsPhase.SCENE_COMPLETE, m.phase.value)
    }

    @Test
    fun nextScene_fromSceneComplete_movesToGenerating() {
        val m = machine()
        m.startGame()
        m.confirmSettings()
        m.sceneReady()
        m.allObjectsFound()
        m.nextScene()
        assertEquals(HiddenObjectsPhase.GENERATING, m.phase.value)
    }
}
