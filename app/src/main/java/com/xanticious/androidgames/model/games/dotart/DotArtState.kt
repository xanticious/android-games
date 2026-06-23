package com.xanticious.androidgames.model.games.dotart

import com.xanticious.androidgames.model.Vec2

enum class DotArtCanvasSize(val dotCount: Int, val label: String) {
    SMALL(15, "Small"),
    MEDIUM(25, "Medium"),
    LARGE(40, "Large")
}

enum class DotArtPaperTone(val label: String) {
    WHITE("White"),
    CREAM("Cream"),
    DARK("Dark")
}

enum class DotArtPaletteColor(val label: String) {
    LAGOON("Lagoon"),
    MINT("Mint"),
    DEEP("Deep"),
    SKY("Sky"),
    NIGHT("Night"),
    REEF("Reef"),
    CORAL("Coral"),
    SUN("Sun"),
    KELP("Kelp"),
    FOAM("Foam"),
    SLATE("Slate"),
    ORANGE("Orange")
}

enum class DotArtBrushSize(val label: String, val normalizedWidth: Float) {
    FINE("Fine", 0.006f),
    MEDIUM("Medium", 0.016f),
    THICK("Thick", 0.036f)
}

data class DotArtConfig(
    val dotCount: Int,
    val minDotSeparation: Float,
    val historyLimit: Int = 50
)

data class DotArtDot(val id: Int, val position: Vec2)

data class DotArtSegment(val startDotId: Int, val endDotId: Int)

data class DotArtRegion(
    val id: String,
    val vertices: List<Vec2>,
    val fill: DotArtPaletteColor? = null
)

data class DotArtFillAction(
    val regionId: String,
    val previousColor: DotArtPaletteColor?,
    val appliedColor: DotArtPaletteColor
)

data class DotArtStroke(
    val points: List<Vec2>,
    val color: DotArtPaletteColor,
    val brushSize: DotArtBrushSize,
    val opacity: Float,
    val eraser: Boolean = false
)

data class DotArtCanvasState(
    val dots: List<DotArtDot>,
    val segments: List<DotArtSegment> = emptyList(),
    val regions: List<DotArtRegion> = emptyList(),
    val fillHistory: List<DotArtFillAction> = emptyList(),
    val strokes: List<DotArtStroke> = emptyList()
) {
    companion object {
        const val OUTSIDE_REGION_ID = "outside"
    }
}
