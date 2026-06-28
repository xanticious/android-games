package com.xanticious.androidgames.games.memorylanes

import com.xanticious.androidgames.controller.games.memorylanes.MemoryLanesController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.memorylanes.MemoryLanesGameState
import com.xanticious.androidgames.model.games.memorylanes.MemoryTile
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryLanesControllerTest {
    private val controller = MemoryLanesController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)

    @Test
    fun startState_sequence_hasOneTile() {
        val state = controller.startState(config, Random(1))
        assertEquals(1, state.sequence.size)
    }

    @Test
    fun remainingCounts_sequenceComposition_reportsTileCounts() {
        val counts = controller.remainingCounts(listOf(MemoryTile(1), MemoryTile(2), MemoryTile(1)), emptyList(), 4)
        assertEquals(2, counts.first { it.tile == MemoryTile(1) }.remaining)
    }

    @Test
    fun addTile_availableTile_appendsToBuiltSequence() {
        val state = MemoryLanesGameState.initial(MemoryTile(2))
        assertEquals(listOf(MemoryTile(2)), controller.addTile(state, MemoryTile(2), config).builtSequence)
    }

    @Test
    fun undoTile_nonEmptyBuiltSequence_removesLastTile() {
        val state = MemoryLanesGameState.initial(MemoryTile(2)).copy(builtSequence = listOf(MemoryTile(2)))
        assertEquals(emptyList<MemoryTile>(), controller.undoTile(state).builtSequence)
    }

    @Test
    fun canSubmit_fullSequence_returnsTrue() {
        val state = MemoryLanesGameState.initial(MemoryTile(2)).copy(builtSequence = listOf(MemoryTile(2)))
        assertTrue(controller.canSubmit(state))
    }

    @Test
    fun validate_wrongOrder_returnsIncorrect() {
        val state = MemoryLanesGameState(
            round = 2,
            sequence = listOf(MemoryTile(1), MemoryTile(2)),
            builtSequence = listOf(MemoryTile(2), MemoryTile(1))
        )
        assertFalse(controller.validate(state).correct)
    }
}
