package com.xanticious.androidgames.controller.games.hiddencommon

import com.xanticious.androidgames.model.games.hiddencommon.HiddenHotspot
import com.xanticious.androidgames.model.games.hiddencommon.HiddenObjectAsset
import com.xanticious.androidgames.model.games.hiddencommon.HiddenObjectKind
import com.xanticious.androidgames.model.games.hiddencommon.HiddenRect
import com.xanticious.androidgames.model.games.hiddencommon.HiddenSceneDefinition

fun hiddenSceneDefinitions(): List<HiddenSceneDefinition> {
    val pool = HiddenObjectKind.entries.mapIndexed { index, kind ->
        val sizeMin = 0.055f + (index % 3) * 0.008f
        HiddenObjectAsset(
            kind = kind,
            sizeMin = sizeMin,
            sizeMax = sizeMin + 0.04f,
            rotationMin = -35f,
            rotationMax = 35f,
            minimumVisibleFraction = 0.25f + (index % 2) * 0.05f
        )
    }
    return listOf(
        HiddenSceneDefinition(
            id = "reef-garden",
            name = "Reef Garden",
            hotspots = listOf(
                HiddenHotspot(HiddenRect(0.06f, 0.12f, 0.44f, 0.52f), 7),
                HiddenHotspot(HiddenRect(0.48f, 0.10f, 0.94f, 0.46f), 7),
                HiddenHotspot(HiddenRect(0.10f, 0.56f, 0.90f, 0.92f), 10)
            ),
            objectPool = pool
        ),
        HiddenSceneDefinition(
            id = "shipwreck-cove",
            name = "Shipwreck Cove",
            hotspots = listOf(
                HiddenHotspot(HiddenRect(0.08f, 0.18f, 0.38f, 0.82f), 8),
                HiddenHotspot(HiddenRect(0.40f, 0.34f, 0.72f, 0.90f), 8),
                HiddenHotspot(HiddenRect(0.66f, 0.12f, 0.94f, 0.76f), 8)
            ),
            objectPool = pool.reversed()
        ),
        HiddenSceneDefinition(
            id = "kelp-maze",
            name = "Kelp Maze",
            hotspots = listOf(
                HiddenHotspot(HiddenRect(0.08f, 0.08f, 0.92f, 0.32f), 7),
                HiddenHotspot(HiddenRect(0.14f, 0.36f, 0.48f, 0.92f), 8),
                HiddenHotspot(HiddenRect(0.52f, 0.36f, 0.88f, 0.92f), 8)
            ),
            objectPool = pool.drop(5) + pool.take(5)
        )
    )
}
