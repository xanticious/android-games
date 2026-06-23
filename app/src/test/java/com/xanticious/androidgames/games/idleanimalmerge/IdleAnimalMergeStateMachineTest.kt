package com.xanticious.androidgames.games.idleanimalmerge

import com.xanticious.androidgames.state.games.idleanimalmerge.IdleAnimalMergePhase
import com.xanticious.androidgames.state.games.idleanimalmerge.IdleAnimalMergeStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class IdleAnimalMergeStateMachineTest {

    private fun machine() = IdleAnimalMergeStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_isIdle() {
        val m = machine()
        assertEquals(IdleAnimalMergePhase.IDLE, m.phase.value)
    }

    @Test
    fun startGame_movesToPlaying() {
        val m = machine()
        m.startGame()
        assertEquals(IdleAnimalMergePhase.PLAYING, m.phase.value)
    }

    @Test
    fun openHowToPlay_fromIdle_movesToHowToPlay() {
        val m = machine()
        m.openHowToPlay()
        assertEquals(IdleAnimalMergePhase.HOW_TO_PLAY, m.phase.value)
    }

    @Test
    fun dismissHowToPlay_fromHowToPlay_returnsToIdle() {
        val m = machine()
        m.openHowToPlay()
        m.dismissHowToPlay()
        assertEquals(IdleAnimalMergePhase.IDLE, m.phase.value)
    }

    @Test
    fun hourlySpawn_fromPlaying_movesToAnimalArrived() {
        val m = machine()
        m.startGame()
        m.hourlySpawn()
        assertEquals(IdleAnimalMergePhase.ANIMAL_ARRIVED, m.phase.value)
    }

    @Test
    fun animalPlaced_fromAnimalArrived_returnsToPlaying() {
        val m = machine()
        m.startGame()
        m.hourlySpawn()
        m.animalPlaced()
        assertEquals(IdleAnimalMergePhase.PLAYING, m.phase.value)
    }

    @Test
    fun fieldCapacityReached_fromPlaying_movesToFieldFull() {
        val m = machine()
        m.startGame()
        m.fieldCapacityReached()
        assertEquals(IdleAnimalMergePhase.FIELD_FULL, m.phase.value)
    }

    @Test
    fun fieldCapacityReached_fromAnimalArrived_movesToFieldFull() {
        val m = machine()
        m.startGame()
        m.hourlySpawn()
        m.fieldCapacityReached()
        assertEquals(IdleAnimalMergePhase.FIELD_FULL, m.phase.value)
    }

    @Test
    fun spaceFreed_fromFieldFull_returnsToPlaying() {
        val m = machine()
        m.startGame()
        m.fieldCapacityReached()
        m.spaceFreed()
        assertEquals(IdleAnimalMergePhase.PLAYING, m.phase.value)
    }

    @Test
    fun openHowToPlay_fromPlaying_movesToHowToPlay() {
        val m = machine()
        m.startGame()
        m.openHowToPlay()
        assertEquals(IdleAnimalMergePhase.HOW_TO_PLAY, m.phase.value)
    }

    @Test
    fun fullFlow_spawn_place_fieldFull_free_returnsToPlaying() {
        val m = machine()
        m.startGame()
        m.hourlySpawn()
        m.animalPlaced()
        m.fieldCapacityReached()
        m.spaceFreed()
        assertEquals(IdleAnimalMergePhase.PLAYING, m.phase.value)
    }
}
