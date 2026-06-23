package com.xanticious.androidgames.games.dotart

import com.xanticious.androidgames.controller.games.dotart.DotArtController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.dotart.DotArtBrushSize
import com.xanticious.androidgames.model.games.dotart.DotArtCanvasState
import com.xanticious.androidgames.model.games.dotart.DotArtDot
import com.xanticious.androidgames.model.games.dotart.DotArtPaletteColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DotArtControllerTest {
    private val controller = DotArtController()

    @Test
    fun placeDots_hardConfig_createsConfiguredCount() {
        val dots = controller.placeDots(controller.configFor(GameDifficulty.HARD), Random(7))
        assertEquals(40, dots.size)
    }

    @Test
    fun connectDots_twoDots_addsSegment() {
        val state = DotArtCanvasState(dots = listOf(DotArtDot(0, Vec2(0.1f, 0.1f)), DotArtDot(1, Vec2(0.9f, 0.9f))))
        val connected = controller.connectDots(state, 0, 1)
        assertEquals(1, connected.segments.size)
    }

    @Test
    fun closeRegions_squareGraph_findsInteriorRegion() {
        val state = squareState()
            .let { controller.connectDots(it, 0, 1) }
            .let { controller.connectDots(it, 1, 2) }
            .let { controller.connectDots(it, 2, 3) }
            .let { controller.connectDots(it, 3, 0) }
        assertTrue(state.regions.any { it.id != DotArtCanvasState.OUTSIDE_REGION_ID })
    }

    @Test
    fun applyFill_closedRegion_setsSelectedColor() {
        val state = squareState()
            .let { controller.connectDots(it, 0, 1) }
            .let { controller.connectDots(it, 1, 2) }
            .let { controller.connectDots(it, 2, 3) }
            .let { controller.connectDots(it, 3, 0) }
        val region = state.regions.first { it.id != DotArtCanvasState.OUTSIDE_REGION_ID }
        val filled = controller.applyFill(state, region.id, DotArtPaletteColor.CORAL)
        assertEquals(DotArtPaletteColor.CORAL, filled.regions.first { it.id == region.id }.fill)
    }

    @Test
    fun recordStroke_twoPoints_addsStroke() {
        val state = squareState()
        val stroked = controller.recordStroke(state, listOf(Vec2(0.1f, 0.1f), Vec2(0.2f, 0.2f)), DotArtPaletteColor.SUN, DotArtBrushSize.MEDIUM)
        assertEquals(1, stroked.strokes.size)
    }

    private fun squareState() = DotArtCanvasState(
        dots = listOf(
            DotArtDot(0, Vec2(0.25f, 0.25f)),
            DotArtDot(1, Vec2(0.75f, 0.25f)),
            DotArtDot(2, Vec2(0.75f, 0.75f)),
            DotArtDot(3, Vec2(0.25f, 0.75f))
        )
    )
}
