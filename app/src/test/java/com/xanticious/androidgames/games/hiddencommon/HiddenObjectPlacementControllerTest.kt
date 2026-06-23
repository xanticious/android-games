package com.xanticious.androidgames.games.hiddencommon

import com.xanticious.androidgames.controller.games.hiddencommon.HiddenObjectPlacementController
import com.xanticious.androidgames.controller.games.hiddencommon.HiddenPlacementConfig
import com.xanticious.androidgames.controller.games.hiddencommon.hiddenSceneDefinitions
import com.xanticious.androidgames.model.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HiddenObjectPlacementControllerTest {
    private val controller = HiddenObjectPlacementController()
    private val config = HiddenPlacementConfig(retryLimit = 10, hotspotMargin = 0.02f, visibleFractionScale = 1f)

    @Test
    fun placeObjects_requestedCount_placesCount() {
        val objects = controller.placeObjects(hiddenSceneDefinitions().first(), 5, config, Random(7))
        assertEquals(5, objects.size)
    }

    @Test
    fun placeObjects_generatedPositions_stayNormalized() {
        val objects = controller.placeObjects(hiddenSceneDefinitions().first(), 8, config, Random(8))
        assertTrue(objects.all { it.position.x in 0f..1f && it.position.y in 0f..1f })
    }

    @Test
    fun hitTest_centerPoint_returnsObject() {
        val objects = controller.placeObjects(hiddenSceneDefinitions().first(), 1, config, Random(9))
        assertNotNull(controller.hitTest(objects, objects.first().position))
    }

    @Test
    fun hitTest_ignoredObject_returnsNull() {
        val objects = controller.placeObjects(hiddenSceneDefinitions().first(), 1, config, Random(9))
        assertEquals(null, controller.hitTest(objects, objects.first().position, ignoredIds = setOf(objects.first().id)))
    }
}
