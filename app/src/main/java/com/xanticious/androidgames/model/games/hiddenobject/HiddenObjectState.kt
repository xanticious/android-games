package com.xanticious.androidgames.model.games.hiddenobject

import com.xanticious.androidgames.model.games.hiddencommon.HiddenSceneDefinition
import com.xanticious.androidgames.model.games.hiddencommon.PlacedHiddenObject

data class HiddenObjectConfig(
    val clutterObjectCount: Int,
    val retryLimit: Int,
    val hotspotMargin: Float,
    val visibleFractionScale: Float,
    val hitPadding: Float,
    val hintCooldownSeconds: Float,
    val timeLimitSeconds: Float,
    val showObjectLabel: Boolean,
    val wrongTapPulse: Boolean
)

data class HiddenObjectRound(
    val scene: HiddenSceneDefinition,
    val target: PlacedHiddenObject,
    val clutter: List<PlacedHiddenObject>,
    val found: Boolean = false,
    val elapsedSeconds: Float = 0f,
    val hintsUsed: Int = 0,
    val wrongTaps: Int = 0
) {
    val objects: List<PlacedHiddenObject> get() = (clutter + target).sortedBy { it.zIndex }
}

enum class HiddenObjectTapResult { FOUND, WRONG }
