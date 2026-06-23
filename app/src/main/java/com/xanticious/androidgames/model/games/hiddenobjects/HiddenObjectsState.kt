package com.xanticious.androidgames.model.games.hiddenobjects

import com.xanticious.androidgames.model.games.hiddencommon.HiddenSceneDefinition
import com.xanticious.androidgames.model.games.hiddencommon.PlacedHiddenObject

data class HiddenObjectsConfig(
    val objectCount: Int,
    val retryLimit: Int,
    val hotspotMargin: Float,
    val visibleFractionScale: Float,
    val hitPadding: Float,
    val hintCooldownSeconds: Float,
    val timeLimitSeconds: Float,
    val showObjectLabels: Boolean,
    val zoomAssist: Boolean
)

data class HiddenObjectsScene(
    val scene: HiddenSceneDefinition,
    val objects: List<PlacedHiddenObject>,
    val foundIds: Set<Int> = emptySet(),
    val elapsedSeconds: Float = 0f,
    val hintsUsed: Int = 0,
    val wrongTaps: Int = 0
) {
    val foundCount: Int get() = foundIds.size
    val totalCount: Int get() = objects.size
    val remainingObjects: List<PlacedHiddenObject> get() = objects.filterNot { it.id in foundIds }
}

data class HiddenObjectsTapResult(
    val scene: HiddenObjectsScene,
    val foundObject: PlacedHiddenObject?
)
