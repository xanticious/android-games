package com.xanticious.androidgames.controller.games.dotart

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.dotart.DotArtBrushSize
import com.xanticious.androidgames.model.games.dotart.DotArtCanvasState
import com.xanticious.androidgames.model.games.dotart.DotArtConfig
import com.xanticious.androidgames.model.games.dotart.DotArtDot
import com.xanticious.androidgames.model.games.dotart.DotArtFillAction
import com.xanticious.androidgames.model.games.dotart.DotArtPaletteColor
import com.xanticious.androidgames.model.games.dotart.DotArtRegion
import com.xanticious.androidgames.model.games.dotart.DotArtSegment
import com.xanticious.androidgames.model.games.dotart.DotArtStroke
import kotlin.math.atan2
import kotlin.random.Random

class DotArtController {
    private val canvasMin = 0f
    private val canvasMax = 1f
    private val placementMargin = 0.08f

    fun configFor(difficulty: GameDifficulty): DotArtConfig = when (difficulty) {
        GameDifficulty.EASY -> DotArtConfig(dotCount = 15, minDotSeparation = 0.15f)
        GameDifficulty.MEDIUM -> DotArtConfig(dotCount = 25, minDotSeparation = 0.11f)
        GameDifficulty.HARD -> DotArtConfig(dotCount = 40, minDotSeparation = 0.08f)
    }

    fun startCanvas(config: DotArtConfig, random: Random): DotArtCanvasState =
        DotArtCanvasState(dots = placeDots(config, random), regions = listOf(outsideRegion()))

    fun placeDots(config: DotArtConfig, random: Random): List<DotArtDot> {
        val points = mutableListOf<Vec2>()
        var attempts = 0
        val maxAttempts = config.dotCount * 300
        while (points.size < config.dotCount && attempts < maxAttempts) {
            attempts += 1
            val candidate = Vec2(
                random.nextFloat().scaleToCanvas(),
                random.nextFloat().scaleToCanvas()
            )
            if (points.all { it.distanceTo(candidate) >= config.minDotSeparation }) {
                points += candidate
            }
        }
        if (points.size < config.dotCount) {
            points += gridFallback(config.dotCount - points.size, points)
        }
        return points.take(config.dotCount).mapIndexed { index, point -> DotArtDot(index, point) }
    }

    fun connectDots(state: DotArtCanvasState, startDotId: Int, endDotId: Int): DotArtCanvasState {
        require(startDotId != endDotId) { "A segment requires two different dots." }
        require(state.dots.any { it.id == startDotId } && state.dots.any { it.id == endDotId }) { "Both dots must exist." }
        val segment = orderedSegment(startDotId, endDotId)
        if (state.segments.any { it == segment }) return state
        val segments = state.segments + segment
        return state.copy(segments = segments, regions = closeRegions(state.dots, segments))
    }

    fun undoSegment(state: DotArtCanvasState): DotArtCanvasState {
        val segments = state.segments.dropLast(1)
        return state.copy(segments = segments, regions = closeRegions(state.dots, segments))
    }

    fun unconnectedDotCount(state: DotArtCanvasState): Int {
        val connected = state.segments.flatMap { listOf(it.startDotId, it.endDotId) }.toSet()
        return state.dots.count { it.id !in connected }
    }

    fun canAdvanceToFill(state: DotArtCanvasState): Boolean = state.dots.isNotEmpty() && unconnectedDotCount(state) == 0

    fun closeRegions(dots: List<DotArtDot>, segments: List<DotArtSegment>): List<DotArtRegion> {
        val positions = dots.associate { it.id to it.position }
        val neighbors = buildMap<Int, List<Int>> {
            dots.forEach { dot -> put(dot.id, emptyList()) }
            val mutable = dots.associate { it.id to mutableSetOf<Int>() }
            segments.forEach { segment ->
                mutable[segment.startDotId]?.add(segment.endDotId)
                mutable[segment.endDotId]?.add(segment.startDotId)
            }
            mutable.forEach { (id, links) -> put(id, links.sorted()) }
        }
        val cycles = linkedMapOf<String, List<Int>>()
        dots.map { it.id }.sorted().forEach { start ->
            findCycles(start, start, neighbors, listOf(start), cycles)
        }
        val regions = cycles.values.mapNotNull { cycle ->
            val vertices = cycle.mapNotNull { positions[it] }
            if (vertices.size >= 3 && polygonArea(vertices) > areaEpsilon) {
                val sorted = vertices.sortedAroundCenter()
                DotArtRegion(id = "region-${cycle.sorted().joinToString("-")}", vertices = sorted)
            } else null
        }.distinctBy { it.id }.sortedBy { it.id }
        return listOf(outsideRegion()) + regions
    }

    fun applyFill(
        state: DotArtCanvasState,
        regionId: String,
        color: DotArtPaletteColor,
        historyLimit: Int = 50
    ): DotArtCanvasState {
        val regions = if (state.regions.isEmpty()) closeRegions(state.dots, state.segments) else state.regions
        val region = regions.firstOrNull { it.id == regionId } ?: return state.copy(regions = regions)
        val updated = regions.map { if (it.id == regionId) it.copy(fill = color) else it }
        val action = DotArtFillAction(regionId, region.fill, color)
        return state.copy(regions = updated, fillHistory = (state.fillHistory + action).takeLast(historyLimit))
    }

    fun undoFill(state: DotArtCanvasState): DotArtCanvasState {
        val action = state.fillHistory.lastOrNull() ?: return state
        val regions = state.regions.map {
            if (it.id == action.regionId) it.copy(fill = action.previousColor) else it
        }
        return state.copy(regions = regions, fillHistory = state.fillHistory.dropLast(1))
    }

    fun recordStroke(
        state: DotArtCanvasState,
        points: List<Vec2>,
        color: DotArtPaletteColor,
        brushSize: DotArtBrushSize,
        opacity: Float = 0.8f,
        eraser: Boolean = false,
        historyLimit: Int = 50
    ): DotArtCanvasState {
        val strokePoints = points.map { it.coerceIn(canvasMin, canvasMin, canvasMax, canvasMax) }
        if (strokePoints.size < 2) return state
        val stroke = DotArtStroke(strokePoints, color, brushSize, opacity.coerceIn(0f, 1f), eraser)
        return state.copy(strokes = (state.strokes + stroke).takeLast(historyLimit))
    }

    fun undoStroke(state: DotArtCanvasState): DotArtCanvasState = state.copy(strokes = state.strokes.dropLast(1))

    fun nearestDot(dots: List<DotArtDot>, point: Vec2, threshold: Float): DotArtDot? =
        dots.asSequence()
            .map { it to it.position.distanceTo(point) }
            .filter { it.second <= threshold }
            .minByOrNull { it.second }
            ?.first

    fun regionAt(state: DotArtCanvasState, point: Vec2): DotArtRegion {
        val regions = if (state.regions.isEmpty()) closeRegions(state.dots, state.segments) else state.regions
        return regions.asSequence()
            .filter { it.id != DotArtCanvasState.OUTSIDE_REGION_ID }
            .firstOrNull { containsPoint(it.vertices, point) }
            ?: regions.first { it.id == DotArtCanvasState.OUTSIDE_REGION_ID }
    }

    fun containsPoint(vertices: List<Vec2>, point: Vec2): Boolean {
        var inside = false
        var previous = vertices.lastOrNull() ?: return false
        vertices.forEach { current ->
            val crosses = (current.y > point.y) != (previous.y > point.y)
            if (crosses) {
                val xAtY = (previous.x - current.x) * (point.y - current.y) / (previous.y - current.y) + current.x
                if (point.x < xAtY) inside = !inside
            }
            previous = current
        }
        return inside
    }

    private fun Float.scaleToCanvas(): Float = placementMargin + this * (1f - placementMargin * 2f)

    private fun gridFallback(needed: Int, existing: List<Vec2>): List<Vec2> {
        val total = needed + existing.size
        val columns = kotlin.math.ceil(kotlin.math.sqrt(total.toDouble())).toInt().coerceAtLeast(1)
        val rows = kotlin.math.ceil(total.toDouble() / columns.toDouble()).toInt().coerceAtLeast(1)
        val candidates = (0 until rows).flatMap { row ->
            (0 until columns).map { column ->
                Vec2(
                    ((column + 1f) / (columns + 1f)).coerceIn(placementMargin, 1f - placementMargin),
                    ((row + 1f) / (rows + 1f)).coerceIn(placementMargin, 1f - placementMargin)
                )
            }
        }
        val spaced = candidates.filter { candidate -> existing.none { it.distanceTo(candidate) < 0.03f } }.take(needed)
        if (spaced.size == needed) return spaced
        return spaced + candidates.filter { it !in spaced }.take(needed - spaced.size)
    }

    private fun orderedSegment(a: Int, b: Int): DotArtSegment = if (a < b) DotArtSegment(a, b) else DotArtSegment(b, a)

    private fun findCycles(
        start: Int,
        current: Int,
        neighbors: Map<Int, List<Int>>,
        path: List<Int>,
        cycles: MutableMap<String, List<Int>>
    ) {
        if (path.size > maxCycleVertices) return
        neighbors[current].orEmpty().forEach { next ->
            when {
                next == start && path.size >= 3 -> cycles[cycleKey(path)] = path
                next > start && next !in path -> findCycles(start, next, neighbors, path + next, cycles)
            }
        }
    }

    private fun cycleKey(path: List<Int>): String = path.sorted().joinToString("-")

    private fun List<Vec2>.sortedAroundCenter(): List<Vec2> {
        val center = Vec2(sumOf { it.x.toDouble() }.toFloat() / size, sumOf { it.y.toDouble() }.toFloat() / size)
        return sortedBy { atan2((it.y - center.y), (it.x - center.x)) }
    }

    private fun polygonArea(vertices: List<Vec2>): Float {
        if (vertices.size < 3) return 0f
        var area = 0f
        vertices.forEachIndexed { index, current ->
            val next = vertices[(index + 1) % vertices.size]
            area += current.x * next.y - next.x * current.y
        }
        return kotlin.math.abs(area) * 0.5f
    }

    private fun outsideRegion(): DotArtRegion = DotArtRegion(
        id = DotArtCanvasState.OUTSIDE_REGION_ID,
        vertices = listOf(Vec2(0f, 0f), Vec2(1f, 0f), Vec2(1f, 1f), Vec2(0f, 1f))
    )

    private companion object {
        const val maxCycleVertices = 8
        const val areaEpsilon = 0.0005f
    }
}
