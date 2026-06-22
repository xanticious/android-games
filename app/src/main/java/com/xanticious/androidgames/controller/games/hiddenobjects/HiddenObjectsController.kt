package com.xanticious.androidgames.controller.games.hiddenobjects

import com.xanticious.androidgames.controller.games.hiddencommon.HiddenObjectPlacementController
import com.xanticious.androidgames.controller.games.hiddencommon.HiddenPlacementConfig
import com.xanticious.androidgames.controller.games.hiddencommon.hiddenSceneDefinitions
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.hiddenobjects.HiddenObjectsConfig
import com.xanticious.androidgames.model.games.hiddenobjects.HiddenObjectsScene
import com.xanticious.androidgames.model.games.hiddenobjects.HiddenObjectsTapResult
import kotlin.random.Random

class HiddenObjectsController(
    private val placement: HiddenObjectPlacementController = HiddenObjectPlacementController()
) {
    fun configFor(difficulty: GameDifficulty): HiddenObjectsConfig = when (difficulty) {
        GameDifficulty.EASY -> HiddenObjectsConfig(
            objectCount = 10,
            retryLimit = 14,
            hotspotMargin = 0.04f,
            visibleFractionScale = 1.25f,
            hitPadding = 0.025f,
            hintCooldownSeconds = 30f,
            timeLimitSeconds = 0f,
            showObjectLabels = true,
            zoomAssist = true
        )
        GameDifficulty.MEDIUM -> HiddenObjectsConfig(
            objectCount = 15,
            retryLimit = 10,
            hotspotMargin = 0.025f,
            visibleFractionScale = 1f,
            hitPadding = 0.015f,
            hintCooldownSeconds = 30f,
            timeLimitSeconds = 0f,
            showObjectLabels = true,
            zoomAssist = true
        )
        GameDifficulty.HARD -> HiddenObjectsConfig(
            objectCount = 20,
            retryLimit = 8,
            hotspotMargin = 0.01f,
            visibleFractionScale = 0.85f,
            hitPadding = 0.005f,
            hintCooldownSeconds = 60f,
            timeLimitSeconds = 0f,
            showObjectLabels = true,
            zoomAssist = true
        )
    }

    fun generateScene(
        config: HiddenObjectsConfig,
        random: Random = Random.Default
    ): HiddenObjectsScene {
        val scene = hiddenSceneDefinitions().random(random)
        val objects = placement.placeObjects(
            scene = scene,
            count = config.objectCount,
            config = HiddenPlacementConfig(config.retryLimit, config.hotspotMargin, config.visibleFractionScale, config.hitPadding),
            random = random
        )
        return HiddenObjectsScene(scene = scene, objects = objects)
    }

    fun tap(scene: HiddenObjectsScene, config: HiddenObjectsConfig, point: Vec2): HiddenObjectsTapResult {
        val hit = placement.hitTest(
            objects = scene.objects,
            tap = point,
            ignoredIds = scene.foundIds,
            hitPadding = config.hitPadding
        )
        return if (hit == null) {
            HiddenObjectsTapResult(scene.copy(wrongTaps = scene.wrongTaps + 1), null)
        } else {
            HiddenObjectsTapResult(scene.copy(foundIds = scene.foundIds + hit.id), hit)
        }
    }

    fun useHint(scene: HiddenObjectsScene): HiddenObjectsScene =
        scene.copy(hintsUsed = scene.hintsUsed + 1)

    fun advanceTime(scene: HiddenObjectsScene, dt: Float): HiddenObjectsScene =
        scene.copy(elapsedSeconds = scene.elapsedSeconds + dt.coerceAtLeast(0f))

    fun isWin(scene: HiddenObjectsScene): Boolean = scene.foundCount == scene.totalCount

    fun isTimeExpired(scene: HiddenObjectsScene, config: HiddenObjectsConfig): Boolean =
        config.timeLimitSeconds > 0f && scene.elapsedSeconds >= config.timeLimitSeconds && !isWin(scene)
}
