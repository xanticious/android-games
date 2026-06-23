package com.xanticious.androidgames.view.games.hiddenobjects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.hiddenobjects.HiddenObjectsController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.hiddencommon.HiddenObjectKind
import com.xanticious.androidgames.model.games.hiddencommon.PlacedHiddenObject
import com.xanticious.androidgames.model.games.hiddenobjects.HiddenObjectsScene
import com.xanticious.androidgames.state.games.hiddenobjects.HiddenObjectsPhase
import com.xanticious.androidgames.state.games.hiddenobjects.HiddenObjectsStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun HiddenObjectsGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { HiddenObjectsController() }
    val machine = remember { HiddenObjectsStateMachine() }
    val phase by machine.phase.collectAsState()

    var selectedDifficulty by rememberSaveable { mutableStateOf(difficulty) }
    var timeLimitSeconds by rememberSaveable { mutableFloatStateOf(0f) }
    var showLabels by rememberSaveable { mutableStateOf(true) }
    var zoomAssist by rememberSaveable { mutableStateOf(true) }
    val config = remember(selectedDifficulty, timeLimitSeconds, showLabels, zoomAssist) {
        controller.configFor(selectedDifficulty).copy(
            timeLimitSeconds = timeLimitSeconds,
            showObjectLabels = showLabels,
            zoomAssist = zoomAssist
        )
    }

    var scene by remember { mutableStateOf<HiddenObjectsScene?>(null) }
    var pulse by remember { mutableStateOf(false) }
    var inspectPoint by remember { mutableStateOf<Vec2?>(null) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        zoom = (zoom * zoomChange).coerceIn(0.8f, 2.5f)
        panX += panChange.x
        panY += panChange.y
    }

    LaunchedEffect(Unit) { machine.startGame() }
    LaunchedEffect(phase) {
        when (phase) {
            HiddenObjectsPhase.GENERATING -> {
                scene = controller.generateScene(config)
                zoom = 1f
                panX = 0f
                panY = 0f
                machine.sceneReady()
            }
            HiddenObjectsPhase.HINT_COOLDOWN -> {
                scene = scene?.let(controller::useHint)
                scene?.remainingObjects?.firstOrNull()?.position?.let { target ->
                    zoom = 2f
                    panX = -target.x * 180f
                    panY = -target.y * 180f
                }
                delay((config.hintCooldownSeconds * 1_000f).roundToInt().coerceAtLeast(1).toLong())
                machine.cooldownExpired()
            }
            else -> Unit
        }
    }

    LaunchedEffect(pulse) {
        if (pulse) {
            delay(220L)
            pulse = false
        }
    }

    GameLoop(running = phase == HiddenObjectsPhase.PLAYING || phase == HiddenObjectsPhase.HINT_COOLDOWN) { dt ->
        scene = scene?.let { current ->
            val updated = controller.advanceTime(current, dt)
            if (controller.isTimeExpired(updated, config)) machine.timerExpired()
            updated
        }
    }

    GameScaffold(
        title = "Hidden Objects",
        onExit = onExit,
        hud = {
            if (phase != HiddenObjectsPhase.SETUP && phase != HiddenObjectsPhase.HOW_TO_PLAY && phase != HiddenObjectsPhase.IDLE) {
                val current = scene
                GameHud(
                    left = current?.scene?.name ?: "Scene",
                    center = "${current?.foundCount ?: 0}/${current?.totalCount ?: config.objectCount}",
                    right = timerText(current?.elapsedSeconds ?: 0f, config.timeLimitSeconds)
                )
            }
        },
        status = {
            val current = scene
            when (phase) {
                HiddenObjectsPhase.PLAYING, HiddenObjectsPhase.HINT_COOLDOWN -> if (current != null) {
                    FindList(current, showLabels = config.showObjectLabels, onHint = machine::hintRequested)
                }
                HiddenObjectsPhase.SCENE_COMPLETE -> if (current != null) {
                    VictoryPanel(
                        score = current.foundCount,
                        bestScore = current.totalCount,
                        stars = if (current.wrongTaps == 0) 3 else 2,
                        onReplay = machine::nextScene,
                        onMenu = onExit,
                        headline = "Scene Complete!",
                        primaryLabel = "Next Scene"
                    )
                }
                HiddenObjectsPhase.GAME_OVER -> if (current != null) {
                    DefeatPanel(
                        score = current.foundCount,
                        bestScore = current.totalCount,
                        onTryAgain = machine::retry,
                        onMenu = onExit,
                        headline = "Time Expired"
                    )
                }
                else -> Unit
            }
        }
    ) {
        when (phase) {
            HiddenObjectsPhase.SETUP -> HiddenObjectsSetup(
                difficulty = selectedDifficulty,
                onDifficulty = { selectedDifficulty = it },
                timeLimitSeconds = timeLimitSeconds,
                onTimeLimit = { timeLimitSeconds = it },
                showLabels = showLabels,
                onShowLabels = { showLabels = it },
                zoomAssist = zoomAssist,
                onZoomAssist = { zoomAssist = it },
                onHowToPlay = machine::openHowToPlay,
                onStart = machine::confirmSettings
            )
            HiddenObjectsPhase.HOW_TO_PLAY -> HiddenObjectsHowToPlay(onBack = machine::backToSetup)
            else -> HiddenObjectsBoard(
                scene = scene,
                pulse = pulse,
                zoom = zoom,
                panX = panX,
                panY = panY,
                transformState = transformState,
                inspectPoint = inspectPoint,
                onInspect = { inspectPoint = it },
                onTap = { point ->
                    val current = scene
                    if (current != null && phase == HiddenObjectsPhase.PLAYING) {
                        val result = controller.tap(current, config, point)
                        scene = result.scene
                        if (result.foundObject == null) {
                            pulse = true
                        } else {
                            if (config.zoomAssist) {
                                zoom = 1.6f
                                panX = -result.foundObject.position.x * 100f
                                panY = -result.foundObject.position.y * 100f
                            }
                            if (controller.isWin(result.scene)) machine.allObjectsFound() else machine.objectFound()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun HiddenObjectsSetup(
    difficulty: GameDifficulty,
    onDifficulty: (GameDifficulty) -> Unit,
    timeLimitSeconds: Float,
    onTimeLimit: (Float) -> Unit,
    showLabels: Boolean,
    onShowLabels: (Boolean) -> Unit,
    zoomAssist: Boolean,
    onZoomAssist: (Boolean) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Difficulty")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameDifficulty.entries.forEach { item ->
                FilterChip(selected = difficulty == item, onClick = { onDifficulty(item) }, label = { Text(item.label) })
            }
        }
        Text("Time limit")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0f to "Off", 180f to "3 min", 300f to "5 min", 600f to "10 min").forEach { option ->
                FilterChip(selected = timeLimitSeconds == option.first, onClick = { onTimeLimit(option.first) }, label = { Text(option.second) })
            }
        }
        SettingSwitch("Object labels", showLabels, onShowLabels)
        SettingSwitch("Zoom assist", zoomAssist, onZoomAssist)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStart) { Text("Start") }
            OutlinedButton(onClick = onHowToPlay) { Text("How to Play") }
        }
    }
}

@Composable
private fun HiddenObjectsHowToPlay(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("How to Play", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Study the list below the scene and find every listed object.")
        Text("Tap a matching object to remove it and reveal anything below it.")
        Text("Pinch to zoom, drag with two fingers to pan, and use Hint if stuck.")
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun FindList(scene: HiddenObjectsScene, showLabels: Boolean, onHint: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Find ${scene.totalCount - scene.foundCount} more", fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onHint) { Text("Hint") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            items(scene.objects, key = { it.id }) { obj ->
                val found = obj.id in scene.foundIds
                Text(
                    text = if (showLabels) obj.name else "●",
                    color = if (found) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f) else MaterialTheme.colorScheme.primary,
                    textDecoration = if (found) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun HiddenObjectsBoard(
    scene: HiddenObjectsScene?,
    pulse: Boolean,
    zoom: Float,
    panX: Float,
    panY: Float,
    transformState: androidx.compose.foundation.gestures.TransformableState,
    inspectPoint: Vec2?,
    onInspect: (Vec2?) -> Unit,
    onTap: (Vec2) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .transformable(transformState)
                .pointerInput(zoom, panX, panY) {
                    detectTapGestures(
                        onPress = {
                            tryAwaitRelease()
                            onInspect(null)
                        },
                        onLongPress = { offset ->
                            onInspect(
                                Vec2(
                                    ((offset.x - panX) / zoom / size.width).coerceIn(0f, 1f),
                                    ((offset.y - panY) / zoom / size.height).coerceIn(0f, 1f)
                                )
                            )
                        },
                        onTap = { offset ->
                            onTap(
                                Vec2(
                                    ((offset.x - panX) / zoom / size.width).coerceIn(0f, 1f),
                                    ((offset.y - panY) / zoom / size.height).coerceIn(0f, 1f)
                                )
                            )
                        }
                    )
                }
        ) {
            withTransform({ translate(panX, panY); scale(zoom, zoom) }) {
                drawGeneratedScene()
                scene?.objects?.filterNot { it.id in scene.foundIds }?.forEach { drawPlacedObject(it) }
                inspectPoint?.let { point -> drawInspectionRing(point) }
            }
            if (pulse) drawRect(Dark2.copy(alpha = 0.25f), size = size)
        }
        if (scene == null) Text("Generating scene…", modifier = Modifier.align(Alignment.Center))
    }
}

private fun timerText(elapsed: Float, limit: Float): String =
    if (limit <= 0f) "${elapsed.roundToInt()}s" else "${(limit - elapsed).coerceAtLeast(0f).roundToInt()}s"

private fun DrawScope.drawInspectionRing(point: Vec2) {
    drawCircle(
        color = Aqua3,
        radius = size.minDimension * 0.08f,
        center = Offset(point.x * size.width, point.y * size.height),
        style = Stroke(size.minDimension * 0.006f)
    )
}

private fun DrawScope.drawGeneratedScene() {
    drawRect(GameCourt, size = size)
    drawRect(Dark1.copy(alpha = 0.65f), topLeft = Offset(0f, size.height * 0.68f), size = Size(size.width, size.height * 0.32f))
    repeat(8) { index ->
        val x = size.width * (0.06f + index * 0.13f)
        val top = size.height * (0.18f + (index % 3) * 0.08f)
        drawLine(GameCourtLine.copy(alpha = 0.32f), Offset(x, size.height), Offset(x + size.width * 0.06f, top), strokeWidth = size.minDimension * 0.010f)
    }
    repeat(6) { index ->
        val center = Offset(size.width * (0.08f + index * 0.17f), size.height * (0.72f + (index % 2) * 0.08f))
        drawCircle(Aqua4.copy(alpha = 0.35f), radius = size.minDimension * (0.06f + index * 0.007f), center = center)
    }
}

private fun DrawScope.drawPlacedObject(obj: PlacedHiddenObject) {
    val center = Offset(obj.position.x * size.width, obj.position.y * size.height)
    val r = obj.size * size.minDimension / 2f
    rotate(obj.rotationDegrees, center) {
        when (obj.kind) {
            HiddenObjectKind.BRASS_KEY -> drawKey(center, r)
            HiddenObjectKind.FISH -> drawFish(center, r)
            HiddenObjectKind.SHELL -> drawShell(center, r)
            HiddenObjectKind.STARFISH -> drawStar(center, r)
            HiddenObjectKind.ANCHOR -> drawAnchor(center, r)
            HiddenObjectKind.PEARL -> drawCircle(Aqua0, r * 0.62f, center)
            HiddenObjectKind.GEM -> drawDiamond(center, r, Aqua2)
            HiddenObjectKind.CRAB -> drawCrab(center, r)
            HiddenObjectKind.LEAF, HiddenObjectKind.CORAL_FAN -> drawLeaf(center, r)
            HiddenObjectKind.ROPE -> drawCircle(GameNeutral, r * 0.65f, center, style = Stroke(r * 0.18f))
            HiddenObjectKind.CROWN -> drawCrown(center, r)
            HiddenObjectKind.BELL -> drawBell(center, r)
            HiddenObjectKind.HEART -> drawHeart(center, r)
            HiddenObjectKind.MOON -> drawMoon(center, r)
            HiddenObjectKind.BOTTLE, HiddenObjectKind.LANTERN -> drawBottle(center, r)
            HiddenObjectKind.BOOT -> drawBoot(center, r)
            HiddenObjectKind.COMPASS, HiddenObjectKind.MASK -> drawCompass(center, r)
        }
    }
}

private fun DrawScope.drawKey(c: Offset, r: Float) { drawCircle(GameAccent, r * 0.30f, c.copy(x = c.x - r * 0.35f), style = Stroke(r * 0.12f)); drawLine(GameAccent, c, c.copy(x = c.x + r * 0.65f), r * 0.16f) }
private fun DrawScope.drawFish(c: Offset, r: Float) { drawOval(GamePlayer, Offset(c.x - r * 0.7f, c.y - r * 0.45f), Size(r * 1.35f, r * 0.9f)); drawTriangle(GamePlayer, c.copy(x = c.x + r * 0.65f), r * 0.45f) }
private fun DrawScope.drawShell(c: Offset, r: Float) { drawArc(Aqua1, 180f, 180f, true, Offset(c.x - r, c.y - r * 0.75f), Size(r * 2f, r * 1.55f)); drawLine(Aqua3, c.copy(y = c.y + r * 0.45f), c.copy(y = c.y - r * 0.55f), r * 0.05f) }
private fun DrawScope.drawStar(c: Offset, r: Float) { val p = Path(); repeat(10) { i -> val a = -1.57f + i * 0.628f; val rr = if (i % 2 == 0) r else r * 0.45f; val o = Offset(c.x + cos(a) * rr, c.y + sin(a) * rr); if (i == 0) p.moveTo(o.x, o.y) else p.lineTo(o.x, o.y) }; p.close(); drawPath(p, GameAccent) }
private fun DrawScope.drawAnchor(c: Offset, r: Float) { drawLine(GameNeutral, c.copy(y = c.y - r), c.copy(y = c.y + r * 0.55f), r * 0.13f); drawArc(GameNeutral, 25f, 130f, false, Offset(c.x - r * 0.95f, c.y - r * 0.2f), Size(r * 1.9f, r * 1.9f), style = Stroke(r * 0.13f)) }
private fun DrawScope.drawCrab(c: Offset, r: Float) { drawOval(GameHazard, Offset(c.x - r * 0.65f, c.y - r * 0.45f), Size(r * 1.3f, r * 0.9f)); drawCircle(GameHazard, r * 0.23f, c.copy(x = c.x - r * 0.85f)); drawCircle(GameHazard, r * 0.23f, c.copy(x = c.x + r * 0.85f)) }
private fun DrawScope.drawLeaf(c: Offset, r: Float) { drawOval(GameSuccess, Offset(c.x - r * 0.5f, c.y - r), Size(r, r * 1.8f)); drawLine(Aqua4, c.copy(y = c.y + r * 0.75f), c.copy(y = c.y - r * 0.7f), r * 0.06f) }
private fun DrawScope.drawCrown(c: Offset, r: Float) { val p = Path().apply { moveTo(c.x - r, c.y + r * 0.45f); lineTo(c.x - r * 0.65f, c.y - r * 0.45f); lineTo(c.x, c.y); lineTo(c.x + r * 0.65f, c.y - r * 0.45f); lineTo(c.x + r, c.y + r * 0.45f); close() }; drawPath(p, GameAccent) }
private fun DrawScope.drawBell(c: Offset, r: Float) { drawArc(GameAccent, 180f, 180f, true, Offset(c.x - r * 0.8f, c.y - r * 0.65f), Size(r * 1.6f, r * 1.5f)); drawCircle(GameHazard, r * 0.16f, c.copy(y = c.y + r * 0.65f)) }
private fun DrawScope.drawHeart(c: Offset, r: Float) { drawCircle(GameHazard, r * 0.36f, c.copy(x = c.x - r * 0.25f, y = c.y - r * 0.2f)); drawCircle(GameHazard, r * 0.36f, c.copy(x = c.x + r * 0.25f, y = c.y - r * 0.2f)); drawTriangle(GameHazard, c.copy(y = c.y + r * 0.10f), r * 0.75f) }
private fun DrawScope.drawMoon(c: Offset, r: Float) { drawCircle(Aqua0, r * 0.72f, c); drawCircle(GameCourt, r * 0.72f, c.copy(x = c.x + r * 0.35f)) }
private fun DrawScope.drawBottle(c: Offset, r: Float) { drawRoundRect(Aqua3, Offset(c.x - r * 0.45f, c.y - r * 0.65f), Size(r * 0.9f, r * 1.3f)); drawRect(Aqua2, Offset(c.x - r * 0.18f, c.y - r), Size(r * 0.36f, r * 0.45f)) }
private fun DrawScope.drawBoot(c: Offset, r: Float) { drawRect(GameNeutral, Offset(c.x - r * 0.45f, c.y - r * 0.75f), Size(r * 0.7f, r * 1.2f)); drawRoundRect(GameNeutral, Offset(c.x - r * 0.45f, c.y + r * 0.15f), Size(r * 1.25f, r * 0.45f)) }
private fun DrawScope.drawCompass(c: Offset, r: Float) { drawCircle(Aqua0, r * 0.75f, c, style = Stroke(r * 0.12f)); drawLine(Aqua2, c.copy(x = c.x - r * 0.45f), c.copy(x = c.x + r * 0.45f), r * 0.06f) }
private fun DrawScope.drawTriangle(color: androidx.compose.ui.graphics.Color, c: Offset, r: Float) { val p = Path().apply { moveTo(c.x - r, c.y - r); lineTo(c.x + r, c.y); lineTo(c.x - r, c.y + r); close() }; drawPath(p, color) }
private fun DrawScope.drawDiamond(c: Offset, r: Float, color: androidx.compose.ui.graphics.Color) { val p = Path().apply { moveTo(c.x, c.y - r); lineTo(c.x + r, c.y); lineTo(c.x, c.y + r); lineTo(c.x - r, c.y); close() }; drawPath(p, color) }
