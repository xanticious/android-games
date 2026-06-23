package com.xanticious.androidgames.games.hiddenobjects

import com.xanticious.androidgames.controller.games.hiddenobjects.HiddenObjectsController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HiddenObjectsControllerTest {
    private val controller = HiddenObjectsController()
    private val config = controller.configFor(GameDifficulty.EASY)

    @Test
    fun generateScene_easyDifficulty_placesConfiguredCount() {
        val scene = controller.generateScene(config, Random(21))
        assertEquals(config.objectCount, scene.objects.size)
    }

    @Test
    fun tap_objectCenter_incrementsFoundCount() {
        val scene = controller.generateScene(config, Random(22))
        val result = controller.tap(scene, config, scene.objects.first().position)
        assertEquals(1, result.scene.foundCount)
    }

    @Test
    fun tap_foundObjectAgain_doesNotIncrementFoundCount() {
        val scene = controller.generateScene(config, Random(23))
        val first = controller.tap(scene, config, scene.objects.first().position).scene
        val second = controller.tap(first, config, scene.objects.first().position).scene
        assertEquals(1, second.foundCount)
    }

    @Test
    fun isWin_allIdsFound_returnsTrue() {
        val scene = controller.generateScene(config, Random(24))
        assertTrue(controller.isWin(scene.copy(foundIds = scene.objects.map { it.id }.toSet())))
    }

    @Test
    fun tap_emptyCorner_recordsWrongTap() {
        val scene = controller.generateScene(config, Random(25))
        val result = controller.tap(scene, config, Vec2(0f, 0f))
        assertEquals(1, result.scene.wrongTaps)
    }
}
