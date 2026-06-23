package com.xanticious.androidgames.model.games.hiddencommon

import com.xanticious.androidgames.model.Vec2

enum class HiddenObjectKind(val label: String) {
    ANCHOR("Anchor"),
    BELL("Bell"),
    BOOT("Boot"),
    BOTTLE("Bottle"),
    BRASS_KEY("Brass Key"),
    COMPASS("Compass"),
    CORAL_FAN("Coral Fan"),
    CRAB("Crab"),
    CROWN("Crown"),
    FISH("Fish"),
    GEM("Gem"),
    HEART("Heart"),
    LANTERN("Lantern"),
    LEAF("Leaf"),
    MASK("Mask"),
    MOON("Moon"),
    PEARL("Pearl"),
    ROPE("Rope"),
    SHELL("Shell"),
    STARFISH("Starfish")
}

data class HiddenRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val center: Vec2 get() = Vec2((left + right) / 2f, (top + bottom) / 2f)

    fun contains(point: Vec2): Boolean =
        point.x in left..right && point.y in top..bottom

    fun inset(amount: Float): HiddenRect = HiddenRect(
        left = (left + amount).coerceAtMost(right),
        top = (top + amount).coerceAtMost(bottom),
        right = (right - amount).coerceAtLeast(left),
        bottom = (bottom - amount).coerceAtLeast(top)
    )
}

data class HiddenObjectAsset(
    val kind: HiddenObjectKind,
    val sizeMin: Float,
    val sizeMax: Float,
    val rotationMin: Float,
    val rotationMax: Float,
    val minimumVisibleFraction: Float = 0.25f
) {
    val name: String get() = kind.label
}

data class HiddenHotspot(
    val rect: HiddenRect,
    val maxObjects: Int
)

data class HiddenSceneDefinition(
    val id: String,
    val name: String,
    val hotspots: List<HiddenHotspot>,
    val objectPool: List<HiddenObjectAsset>
)

data class PlacedHiddenObject(
    val id: Int,
    val asset: HiddenObjectAsset,
    val position: Vec2,
    val size: Float,
    val rotationDegrees: Float,
    val zIndex: Int,
    val visibleFraction: Float
) {
    val kind: HiddenObjectKind get() = asset.kind
    val name: String get() = asset.name
}
