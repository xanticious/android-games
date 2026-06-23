package com.xanticious.androidgames.games.hiddenobject

import com.xanticious.androidgames.controller.games.hiddenobject.HiddenObjectController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.hiddenobject.HiddenObjectTapResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HiddenObjectControllerTest {
    private val controller = HiddenObjectController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)

    @Test
    fun generateRound_mediumDifficulty_addsTargetAndClutter() {
        val round = controller.generateRound(config, Random(11))
        assertEquals(config.clutterObjectCount + 1, round.objects.size)
    }

    @Test
    fun tap_targetCenter_returnsFound() {
        val round = controller.generateRound(config, Random(12))
        val result = controller.tap(round, config, round.target.position)
        assertEquals(HiddenObjectTapResult.FOUND, result.second)
    }

    @Test
    fun tap_emptyCorner_recordsWrongTap() {
        val round = controller.generateRound(config, Random(13))
        val result = controller.tap(round, config, Vec2(0f, 0f))
        assertEquals(1, result.first.wrongTaps)
    }

    @Test
    fun isWin_foundRound_returnsTrue() {
        val round = controller.generateRound(config, Random(14))
        assertTrue(controller.isWin(round.copy(found = true)))
    }
}
