package com.xanticious.androidgames.controller.games.hiddenobject

import com.xanticious.androidgames.controller.games.hiddencommon.HiddenObjectPlacementController
import com.xanticious.androidgames.controller.games.hiddencommon.HiddenPlacementConfig
import com.xanticious.androidgames.controller.games.hiddencommon.hiddenSceneDefinitions
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.hiddenobject.HiddenObjectConfig
import com.xanticious.androidgames.model.games.hiddenobject.HiddenObjectRound
import com.xanticious.androidgames.model.games.hiddenobject.HiddenObjectTapResult
import kotlin.random.Random

class HiddenObjectController(
    private val placement: HiddenObjectPlacementController = HiddenObjectPlacementController()
) {
    fun configFor(difficulty: GameDifficulty): HiddenObjectConfig = when (difficulty) {
        GameDifficulty.EASY -> HiddenObjectConfig(
            clutterObjectCount = 8,
            retryLimit = 14,
            hotspotMargin = 0.04f,
            visibleFractionScale = 1.25f,
            hitPadding = 0.025f,
            hintCooldownSeconds = 30f,
            timeLimitSeconds = 0f,
            showObjectLabel = true,
            wrongTapPulse = true
        )
        GameDifficulty.MEDIUM -> HiddenObjectConfig(
            clutterObjectCount = 14,
            retryLimit = 10,
            hotspotMargin = 0.025f,
            visibleFractionScale = 1f,
            hitPadding = 0.015f,
            hintCooldownSeconds = 30f,
            timeLimitSeconds = 0f,
            showObjectLabel = true,
            wrongTapPulse = true
        )
        GameDifficulty.HARD -> HiddenObjectConfig(
            clutterObjectCount = 18,
            retryLimit = 8,
            hotspotMargin = 0.01f,
            visibleFractionScale = 0.85f,
            hitPadding = 0.005f,
            hintCooldownSeconds = 60f,
            timeLimitSeconds = 0f,
            showObjectLabel = true,
            wrongTapPulse = true
        )
    }

    fun generateRound(
        config: HiddenObjectConfig,
        random: Random = Random.Default
    ): HiddenObjectRound {
        val scene = hiddenSceneDefinitions().random(random)
        val objects = placement.placeObjects(
            scene = scene,
            count = config.clutterObjectCount + 1,
            config = HiddenPlacementConfig(config.retryLimit, config.hotspotMargin, config.visibleFractionScale, config.hitPadding),
            random = random
        )
        val target = objects.random(random)
        return HiddenObjectRound(scene = scene, target = target, clutter = objects.filterNot { it.id == target.id })
    }

    fun tap(round: HiddenObjectRound, config: HiddenObjectConfig, point: Vec2): Pair<HiddenObjectRound, HiddenObjectTapResult> {
        if (round.found) return round to HiddenObjectTapResult.FOUND
        val hit = placement.hitTest(round.objects, point, hitPadding = config.hitPadding)
        return if (hit?.id == round.target.id) {
            round.copy(found = true) to HiddenObjectTapResult.FOUND
        } else {
            round.copy(wrongTaps = round.wrongTaps + 1) to HiddenObjectTapResult.WRONG
        }
    }

    fun useHint(round: HiddenObjectRound): HiddenObjectRound =
        round.copy(hintsUsed = round.hintsUsed + 1)

    fun advanceTime(round: HiddenObjectRound, dt: Float): HiddenObjectRound =
        round.copy(elapsedSeconds = round.elapsedSeconds + dt.coerceAtLeast(0f))

    fun isWin(round: HiddenObjectRound): Boolean = round.found

    fun isTimeExpired(round: HiddenObjectRound, config: HiddenObjectConfig): Boolean =
        config.timeLimitSeconds > 0f && round.elapsedSeconds >= config.timeLimitSeconds
}
