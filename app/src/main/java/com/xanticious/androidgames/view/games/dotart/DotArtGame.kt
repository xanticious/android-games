package com.xanticious.androidgames.view.games.dotart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.dotart.DotArtController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.dotart.DotArtBrushSize
import com.xanticious.androidgames.model.games.dotart.DotArtCanvasSize
import com.xanticious.androidgames.model.games.dotart.DotArtCanvasState
import com.xanticious.androidgames.model.games.dotart.DotArtConfig
import com.xanticious.androidgames.model.games.dotart.DotArtPaletteColor
import com.xanticious.androidgames.model.games.dotart.DotArtPaperTone
import com.xanticious.androidgames.model.games.dotart.DotArtRegion
import com.xanticious.androidgames.model.games.dotart.DotArtStroke
import com.xanticious.androidgames.state.games.dotart.DotArtPhase
import com.xanticious.androidgames.state.games.dotart.DotArtStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import kotlin.random.Random

private enum class DotArtSetupStep { SETTINGS, HOW_TO, PLAYING }

@Composable
fun DotArtGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { DotArtController() }
    val machine = remember { DotArtStateMachine() }
    val phase by machine.phase.collectAsState()

    var setupStep by rememberSaveable { mutableStateOf(DotArtSetupStep.SETTINGS) }
    var canvasSize by rememberSaveable { mutableStateOf(sizeFor(difficulty)) }
    var paperTone by rememberSaveable { mutableStateOf(DotArtPaperTone.WHITE) }
    var showOutlines by rememberSaveable { mutableStateOf(true) }
    var brushOpacity by rememberSaveable { mutableFloatStateOf(0.8f) }
    var selectedColor by rememberSaveable { mutableStateOf(DotArtPaletteColor.LAGOON) }
    var brushSize by rememberSaveable { mutableStateOf(DotArtBrushSize.MEDIUM) }
    var eraser by rememberSaveable { mutableStateOf(false) }

    val baseConfig = remember(difficulty) { controller.configFor(difficulty) }
    val config = remember(baseConfig, canvasSize) {
        val sizeConfig = controller.configFor(difficultyFor(canvasSize))
        DotArtConfig(canvasSize.dotCount, sizeConfig.minDotSeparation, baseConfig.historyLimit)
    }

    var artState by remember { mutableStateOf(controller.startCanvas(config, Random.Default)) }
    var dragStartDotId by remember { mutableStateOf<Int?>(null) }
    var currentStroke by remember { mutableStateOf(emptyList<Vec2>()) }

    LaunchedEffect(setupStep) {
        if (setupStep == DotArtSetupStep.PLAYING && phase == DotArtPhase.IDLE) {
            machine.startCanvas()
        }
    }

    if (setupStep == DotArtSetupStep.PLAYING && phase == DotArtPhase.FINISHED) {
        Box(modifier = Modifier.fillMaxSize()) {
            DotArtCanvas(
                state = artState,
                phase = phase,
                paperTone = paperTone,
                showOutlines = false,
                currentStroke = emptyList(),
                selectedColor = selectedColor,
                brushSize = brushSize,
                brushOpacity = brushOpacity,
                eraser = eraser,
                modifier = Modifier.fillMaxSize()
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Dark1.copy(alpha = 0.92f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = {
                    artState = controller.startCanvas(config, Random.Default)
                    machine.newCanvas()
                }) { Text("New Canvas") }
                OutlinedButton(onClick = {
                    machine.backToMenu()
                    onExit()
                }) { Text("Back to Menu") }
            }
        }
        return
    }

    GameScaffold(
        title = "Dot Art",
        onExit = onExit,
        hud = {
            GameHud(
                left = phaseLabel(phase),
                center = if (setupStep == DotArtSetupStep.PLAYING) "Creative canvas" else "Setup",
                right = if (phase == DotArtPhase.CONNECT) "${controller.unconnectedDotCount(artState)} open" else "No score"
            )
        },
        status = {
            when (setupStep) {
                DotArtSetupStep.SETTINGS -> SettingsPanel(
                    canvasSize = canvasSize,
                    paperTone = paperTone,
                    showOutlines = showOutlines,
                    brushOpacity = brushOpacity,
                    onCanvasSize = { canvasSize = it },
                    onPaperTone = { paperTone = it },
                    onShowOutlines = { showOutlines = it },
                    onBrushOpacity = { brushOpacity = it },
                    onNext = { setupStep = DotArtSetupStep.HOW_TO }
                )
                DotArtSetupStep.HOW_TO -> HowToPanel(
                    onStart = {
                        artState = controller.startCanvas(config, Random.Default)
                        setupStep = DotArtSetupStep.PLAYING
                    }
                )
                DotArtSetupStep.PLAYING -> ControlsPanel(
                    phase = phase,
                    controller = controller,
                    state = artState,
                    selectedColor = selectedColor,
                    brushSize = brushSize,
                    eraser = eraser,
                    onColor = { selectedColor = it; eraser = false },
                    onBrushSize = { brushSize = it },
                    onEraser = { eraser = true },
                    onUndo = {
                        when (phase) {
                            DotArtPhase.CONNECT -> { artState = controller.undoSegment(artState); machine.lineUndo() }
                            DotArtPhase.FILL -> { artState = controller.undoFill(artState); machine.fillUndo() }
                            DotArtPhase.BRUSH -> { artState = controller.undoStroke(artState); machine.strokeUndo() }
                            else -> {}
                        }
                    },
                    onNext = {
                        when (phase) {
                            DotArtPhase.CONNECT -> if (controller.canAdvanceToFill(artState)) machine.phase1Complete()
                            DotArtPhase.FILL -> machine.phase2Complete()
                            DotArtPhase.BRUSH -> machine.drawingDone()
                            else -> {}
                        }
                    },
                    onNew = {
                        artState = controller.startCanvas(config, Random.Default)
                        machine.newCanvas()
                    },
                    onMenu = {
                        machine.backToMenu()
                        onExit()
                    }
                )
            }
        }
    ) {
        if (setupStep == DotArtSetupStep.PLAYING) {
            DotArtCanvas(
                state = artState,
                phase = phase,
                paperTone = paperTone,
                showOutlines = showOutlines,
                currentStroke = currentStroke,
                selectedColor = selectedColor,
                brushSize = brushSize,
                brushOpacity = brushOpacity,
                eraser = eraser,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(phase, artState.dots, artState.segments) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val point = offset.toNormalized(size.width.toFloat(), size.height.toFloat())
                                if (phase == DotArtPhase.CONNECT) {
                                    val threshold = 24.dp.toPx() / minOf(size.width, size.height).toFloat()
                                    dragStartDotId = controller.nearestDot(artState.dots, point, threshold)?.id
                                } else if (phase == DotArtPhase.BRUSH) {
                                    currentStroke = listOf(point)
                                }
                            },
                            onDragEnd = {
                                if (phase == DotArtPhase.BRUSH) {
                                    artState = controller.recordStroke(
                                        artState, currentStroke, selectedColor, brushSize, brushOpacity, eraser
                                    )
                                    if (eraser) machine.strokeErased() else machine.strokeDrawn()
                                    currentStroke = emptyList()
                                } else if (phase == DotArtPhase.CONNECT) {
                                    val startId = dragStartDotId
                                    val endPoint = currentStroke.lastOrNull()
                                    val threshold = 24.dp.toPx() / minOf(size.width, size.height).toFloat()
                                    val endId = endPoint?.let { controller.nearestDot(artState.dots, it, threshold)?.id }
                                    if (startId != null && endId != null && startId != endId) {
                                        artState = controller.connectDots(artState, startId, endId)
                                        machine.lineDrawn()
                                    }
                                    dragStartDotId = null
                                    currentStroke = emptyList()
                                }
                            },
                            onDragCancel = {
                                dragStartDotId = null
                                currentStroke = emptyList()
                            }
                        ) { change, _ ->
                            val point = change.position.toNormalized(size.width.toFloat(), size.height.toFloat())
                            currentStroke = currentStroke + point
                        }
                    }
                    .pointerInput(phase, selectedColor) {
                        detectTapGestures { offset ->
                            if (phase == DotArtPhase.FILL) {
                                val region = controller.regionAt(artState, offset.toNormalized(size.width.toFloat(), size.height.toFloat()))
                                artState = controller.applyFill(artState, region.id, selectedColor)
                                machine.regionFilled()
                            }
                        }
                    }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(paperColor(paperTone)), contentAlignment = Alignment.Center) {
                Text("Create a calm dot-art canvas", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    canvasSize: DotArtCanvasSize,
    paperTone: DotArtPaperTone,
    showOutlines: Boolean,
    brushOpacity: Float,
    onCanvasSize: (DotArtCanvasSize) -> Unit,
    onPaperTone: (DotArtPaperTone) -> Unit,
    onShowOutlines: (Boolean) -> Unit,
    onBrushOpacity: (Float) -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleMedium)
        OptionRow(DotArtCanvasSize.entries.map { it.label }, DotArtCanvasSize.entries.indexOf(canvasSize)) { onCanvasSize(DotArtCanvasSize.entries[it]) }
        OptionRow(DotArtPaperTone.entries.map { it.label }, DotArtPaperTone.entries.indexOf(paperTone)) { onPaperTone(DotArtPaperTone.entries[it]) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onShowOutlines(!showOutlines) }) { Text(if (showOutlines) "Outlines on" else "Outlines off") }
            Text("Brush opacity ${(brushOpacity * 100).toInt()}%")
        }
        Slider(value = brushOpacity, onValueChange = onBrushOpacity, valueRange = 0.4f..1f, steps = 2)
        Button(onClick = onNext, modifier = Modifier.align(Alignment.End)) { Text("How to Play") }
    }
}

@Composable
private fun HowToPanel(onStart: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("How to Play", style = MaterialTheme.typography.titleMedium)
        Text("1. Drag dot to dot until every dot has a line.")
        Text("2. Pick colors, then tap regions to flood-fill them.")
        Text("3. Draw freehand strokes or erase the stroke layer, then tap Done.")
        Button(onClick = onStart, modifier = Modifier.align(Alignment.End)) { Text("Start Canvas") }
    }
}

@Composable
private fun ControlsPanel(
    phase: DotArtPhase,
    controller: DotArtController,
    state: DotArtCanvasState,
    selectedColor: DotArtPaletteColor,
    brushSize: DotArtBrushSize,
    eraser: Boolean,
    onColor: (DotArtPaletteColor) -> Unit,
    onBrushSize: (DotArtBrushSize) -> Unit,
    onEraser: () -> Unit,
    onUndo: () -> Unit,
    onNext: () -> Unit,
    onNew: () -> Unit,
    onMenu: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (phase == DotArtPhase.FILL || phase == DotArtPhase.BRUSH) {
            PaletteRow(selectedColor, eraser, phase == DotArtPhase.BRUSH, onColor, onEraser)
        }
        if (phase == DotArtPhase.BRUSH) {
            OptionRow(DotArtBrushSize.entries.map { it.label }, DotArtBrushSize.entries.indexOf(brushSize)) { onBrushSize(DotArtBrushSize.entries[it]) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (phase == DotArtPhase.FINISHED) {
                Button(onClick = onNew) { Text("New Canvas") }
                OutlinedButton(onClick = onMenu) { Text("Back to Menu") }
            } else {
                OutlinedButton(onClick = onUndo) { Text("Undo") }
                val canAdvance = phase != DotArtPhase.CONNECT || controller.canAdvanceToFill(state)
                Button(onClick = onNext, enabled = canAdvance) {
                    Text(if (phase == DotArtPhase.BRUSH) "Done" else if (canAdvance) "Next →" else "${controller.unconnectedDotCount(state)} dots left")
                }
            }
        }
    }
}

@Composable
private fun DotArtCanvas(
    state: DotArtCanvasState,
    phase: DotArtPhase,
    paperTone: DotArtPaperTone,
    showOutlines: Boolean,
    currentStroke: List<Vec2>,
    selectedColor: DotArtPaletteColor,
    brushSize: DotArtBrushSize,
    brushOpacity: Float,
    eraser: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.background(Dark1)) {
        val side = minOf(size.width, size.height)
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val paper = paperColor(paperTone)
        drawRect(color = paper, topLeft = Offset(left, top), size = Size(side, side))
        drawRegions(state.regions, side, left, top, paper)
        drawSegments(state, side, left, top)
        if (phase == DotArtPhase.FILL && showOutlines) drawRegionOutlines(state.regions, side, left, top)
        drawDots(state, side, left, top)
        state.strokes.forEach { drawStrokePath(it, side, left, top, paper) }
        if (currentStroke.size > 1) {
            drawStrokePath(DotArtStroke(currentStroke, selectedColor, brushSize, brushOpacity, eraser), side, left, top, paper)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRegions(
    regions: List<DotArtRegion>,
    side: Float,
    left: Float,
    top: Float,
    paper: Color
) {
    regions.forEach { region ->
        val fill = region.fill ?: return@forEach
        if (region.id == DotArtCanvasState.OUTSIDE_REGION_ID) {
            drawRect(color = paletteColor(fill), topLeft = Offset(left, top), size = Size(side, side))
        }
    }
    regions.filter { it.id != DotArtCanvasState.OUTSIDE_REGION_ID }.forEach { region ->
        region.fill?.let { drawPath(pathFor(region.vertices, side, left, top, close = true), paletteColor(it)) }
    }
    if (regions.firstOrNull { it.id == DotArtCanvasState.OUTSIDE_REGION_ID && it.fill == null } != null) {
        drawRect(color = paper.copy(alpha = 0f), topLeft = Offset(left, top), size = Size(side, side))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRegionOutlines(regions: List<DotArtRegion>, side: Float, left: Float, top: Float) {
    regions.filter { it.id != DotArtCanvasState.OUTSIDE_REGION_ID }.forEach { region ->
        drawPath(pathFor(region.vertices, side, left, top, close = true), Aqua4.copy(alpha = 0.25f), style = Stroke(width = side * 0.004f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSegments(state: DotArtCanvasState, side: Float, left: Float, top: Float) {
    state.segments.forEach { segment ->
        val start = state.dots.firstOrNull { it.id == segment.startDotId }?.position ?: return@forEach
        val end = state.dots.firstOrNull { it.id == segment.endDotId }?.position ?: return@forEach
        drawLine(color = Dark2, start = start.toOffset(side, left, top), end = end.toOffset(side, left, top), strokeWidth = side * 0.008f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDots(state: DotArtCanvasState, side: Float, left: Float, top: Float) {
    state.dots.forEach { dot ->
        val center = dot.position.toOffset(side, left, top)
        drawCircle(color = Dark0.copy(alpha = 0.16f), radius = side * 0.018f, center = center + Offset(side * 0.004f, side * 0.004f))
        drawCircle(color = Aqua3, radius = side * 0.016f, center = center)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStrokePath(stroke: DotArtStroke, side: Float, left: Float, top: Float, paper: Color) {
    val color = if (stroke.eraser) paper else paletteColor(stroke.color).copy(alpha = stroke.opacity)
    drawPath(pathFor(stroke.points, side, left, top), color, style = Stroke(width = stroke.brushSize.normalizedWidth * side))
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun PaletteRow(
    selected: DotArtPaletteColor,
    eraser: Boolean,
    showEraser: Boolean,
    onColor: (DotArtPaletteColor) -> Unit,
    onEraser: () -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DotArtPaletteColor.entries.forEach { color ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(paletteColor(color), CircleShape)
                    .border(if (selected == color && !eraser) 3.dp else 1.dp, if (selected == color && !eraser) Dark1 else Aqua4, CircleShape)
                    .clickable { onColor(color) }
            )
        }
        if (showEraser) {
            OutlinedButton(onClick = onEraser, colors = ButtonDefaults.outlinedButtonColors(contentColor = if (eraser) GameHazard else Aqua4)) {
                Text("⌫")
            }
        }
    }
}

@Composable
private fun OptionRow(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (selected) Aqua0 else Aqua0.copy(alpha = 0f),
                contentColor = if (selected) Dark2 else MaterialTheme.colorScheme.onSurface
            )
            OutlinedButton(onClick = { onSelected(index) }, colors = colors, shape = RoundedCornerShape(12.dp)) { Text(label) }
        }
    }
}

private fun Offset.toNormalized(width: Float, height: Float): Vec2 {
    val side = minOf(width, height)
    val left = (width - side) / 2f
    val top = (height - side) / 2f
    return Vec2(((x - left) / side).coerceIn(0f, 1f), ((y - top) / side).coerceIn(0f, 1f))
}

private fun Vec2.toOffset(side: Float, left: Float, top: Float): Offset = Offset(left + x * side, top + y * side)

private fun pathFor(points: List<Vec2>, side: Float, left: Float, top: Float, close: Boolean = false): Path {
    val first = points.firstOrNull() ?: return Path()
    return Path().apply {
        moveTo(left + first.x * side, top + first.y * side)
        points.drop(1).forEach { lineTo(left + it.x * side, top + it.y * side) }
        if (close && points.size > 2) close()
    }
}

private fun paletteColor(color: DotArtPaletteColor): Color = when (color) {
    DotArtPaletteColor.LAGOON -> Aqua3
    DotArtPaletteColor.MINT -> Aqua1
    DotArtPaletteColor.DEEP -> Aqua4
    DotArtPaletteColor.SKY -> Aqua2
    DotArtPaletteColor.NIGHT -> Dark2
    DotArtPaletteColor.REEF -> GamePlayer
    DotArtPaletteColor.CORAL -> GameEnemy
    DotArtPaletteColor.SUN -> GameAccent
    DotArtPaletteColor.KELP -> GameSuccess
    DotArtPaletteColor.FOAM -> Aqua0
    DotArtPaletteColor.SLATE -> GameNeutral
    DotArtPaletteColor.ORANGE -> GameHazard
}

private fun paperColor(tone: DotArtPaperTone): Color = when (tone) {
    DotArtPaperTone.WHITE -> Aqua0
    DotArtPaperTone.CREAM -> Aqua1.copy(alpha = 0.22f)
    DotArtPaperTone.DARK -> Dark0
}

private fun phaseLabel(phase: DotArtPhase): String = when (phase) {
    DotArtPhase.IDLE -> "Idle"
    DotArtPhase.CONNECT -> "Phase 1 / 3"
    DotArtPhase.FILL -> "Phase 2 / 3"
    DotArtPhase.BRUSH -> "Phase 3 / 3"
    DotArtPhase.FINISHED -> "Finished"
}

private fun sizeFor(difficulty: GameDifficulty): DotArtCanvasSize = when (difficulty) {
    GameDifficulty.EASY -> DotArtCanvasSize.SMALL
    GameDifficulty.MEDIUM -> DotArtCanvasSize.MEDIUM
    GameDifficulty.HARD -> DotArtCanvasSize.LARGE
}

private fun difficultyFor(size: DotArtCanvasSize): GameDifficulty = when (size) {
    DotArtCanvasSize.SMALL -> GameDifficulty.EASY
    DotArtCanvasSize.MEDIUM -> GameDifficulty.MEDIUM
    DotArtCanvasSize.LARGE -> GameDifficulty.HARD
}
