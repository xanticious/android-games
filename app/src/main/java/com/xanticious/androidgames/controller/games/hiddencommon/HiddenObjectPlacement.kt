package com.xanticious.androidgames.controller.games.hiddencommon

import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.hiddencommon.HiddenObjectAsset
import com.xanticious.androidgames.model.games.hiddencommon.HiddenRect
import com.xanticious.androidgames.model.games.hiddencommon.HiddenSceneDefinition
import com.xanticious.androidgames.model.games.hiddencommon.PlacedHiddenObject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

data class HiddenPlacementConfig(
    val retryLimit: Int,
    val hotspotMargin: Float,
    val visibleFractionScale: Float,
    val hitPadding: Float = 0f
)

class HiddenObjectPlacementController {

    fun placeObjects(
        scene: HiddenSceneDefinition,
        count: Int,
        config: HiddenPlacementConfig,
        random: Random,
        startId: Int = 1
    ): List<PlacedHiddenObject> {
        val selected = scene.objectPool.shuffled(random).take(count.coerceAtMost(scene.objectPool.size))
        val hotspotUsage = IntArray(scene.hotspots.size)
        val placed = mutableListOf<PlacedHiddenObject>()
        selected.forEachIndexed { index, asset ->
            val objectId = startId + index
            val placement = placeAsset(asset, objectId, index, scene, hotspotUsage, placed, config, random)
            placed += placement
        }
        return refreshVisibleFractions(placed, config.visibleFractionScale)
    }

    fun hitTest(
        objects: List<PlacedHiddenObject>,
        tap: Vec2,
        ignoredIds: Set<Int> = emptySet(),
        hitPadding: Float = 0f
    ): PlacedHiddenObject? = objects
        .asSequence()
        .filterNot { it.id in ignoredIds }
        .sortedByDescending { it.zIndex }
        .firstOrNull { containsVisiblePoint(it, tap, hitPadding) }

    fun containsVisiblePoint(obj: PlacedHiddenObject, point: Vec2, hitPadding: Float = 0f): Boolean {
        val radians = -obj.rotationDegrees * PI.toFloat() / 180f
        val dx = point.x - obj.position.x
        val dy = point.y - obj.position.y
        val localX = dx * cos(radians) - dy * sin(radians)
        val localY = dx * sin(radians) + dy * cos(radians)
        val half = obj.size / 2f + hitPadding
        if (abs(localX) > half || abs(localY) > half) return false
        val ellipse = (localX * localX) / (half * half) + (localY * localY) / (half * half)
        return ellipse <= 1f
    }

    fun boundsFor(obj: PlacedHiddenObject): HiddenRect {
        val half = obj.size / 2f
        return HiddenRect(obj.position.x - half, obj.position.y - half, obj.position.x + half, obj.position.y + half)
    }

    private fun placeAsset(
        asset: HiddenObjectAsset,
        objectId: Int,
        zIndex: Int,
        scene: HiddenSceneDefinition,
        hotspotUsage: IntArray,
        placed: List<PlacedHiddenObject>,
        config: HiddenPlacementConfig,
        random: Random
    ): PlacedHiddenObject {
        var best = samplePlacement(asset, objectId, zIndex, scene, hotspotUsage, config, random)
        var bestVisible = visibleFraction(best, placed)
        repeat(config.retryLimit) {
            val candidate = samplePlacement(asset, objectId, zIndex, scene, hotspotUsage, config, random)
            val candidateVisible = visibleFraction(candidate, placed)
            if (candidateVisible > bestVisible) {
                best = candidate
                bestVisible = candidateVisible
            }
            val threshold = asset.minimumVisibleFraction * config.visibleFractionScale
            if (candidateVisible >= threshold) {
                best = candidate
                bestVisible = candidateVisible
                return@repeat
            }
        }
        val hotspotIndex = scene.hotspots.indices.minBy { hotspotUsage[it] }
        hotspotUsage[hotspotIndex] += 1
        return best.copy(visibleFraction = bestVisible)
    }

    private fun samplePlacement(
        asset: HiddenObjectAsset,
        objectId: Int,
        zIndex: Int,
        scene: HiddenSceneDefinition,
        hotspotUsage: IntArray,
        config: HiddenPlacementConfig,
        random: Random
    ): PlacedHiddenObject {
        val hotspotIndex = scene.hotspots.indices
            .filter { hotspotUsage[it] < scene.hotspots[it].maxObjects }
            .ifEmpty { scene.hotspots.indices.toList() }
            .random(random)
        val hotspot = scene.hotspots[hotspotIndex].rect.inset(config.hotspotMargin)
        val size = random.nextFloatIn(asset.sizeMin, asset.sizeMax)
        val half = size / 2f
        val x = random.nextFloatIn(hotspot.left + half, hotspot.right - half)
        val y = random.nextFloatIn(hotspot.top + half, hotspot.bottom - half)
        val rotation = random.nextFloatIn(asset.rotationMin, asset.rotationMax)
        return PlacedHiddenObject(objectId, asset, Vec2(x, y), size, rotation, zIndex, visibleFraction = 1f)
    }

    private fun refreshVisibleFractions(
        objects: List<PlacedHiddenObject>,
        visibleFractionScale: Float
    ): List<PlacedHiddenObject> = objects.mapIndexed { index, obj ->
        val laterObjects = objects.drop(index + 1)
        val threshold = obj.asset.minimumVisibleFraction * visibleFractionScale
        obj.copy(visibleFraction = max(visibleFraction(obj, laterObjects), threshold))
    }

    private fun visibleFraction(obj: PlacedHiddenObject, blockers: List<PlacedHiddenObject>): Float {
        val area = obj.size * obj.size
        val occluded = blockers.sumOf { blocker -> intersectionArea(boundsFor(obj), boundsFor(blocker)).toDouble() }.toFloat()
        return (1f - (occluded / area) * OCCLUSION_WEIGHT).coerceIn(0f, 1f)
    }

    private fun intersectionArea(a: HiddenRect, b: HiddenRect): Float {
        val w = (minOf(a.right, b.right) - maxOf(a.left, b.left)).coerceAtLeast(0f)
        val h = (minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)).coerceAtLeast(0f)
        return w * h
    }

    private fun Random.nextFloatIn(min: Float, max: Float): Float {
        val safeMax = max(max, min)
        return min + nextFloat() * (safeMax - min)
    }

    private companion object {
        const val OCCLUSION_WEIGHT = 0.55f
    }
}
