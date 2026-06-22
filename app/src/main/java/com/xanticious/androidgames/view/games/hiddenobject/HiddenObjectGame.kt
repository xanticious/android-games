package com.xanticious.androidgames.view.games.hiddenobject

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
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.hiddenobject.HiddenObjectController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.hiddencommon.HiddenObjectKind
import com.xanticious.androidgames.model.games.hiddencommon.PlacedHiddenObject
import com.xanticious.androidgames.model.games.hiddenobject.HiddenObjectRound
import com.xanticious.androidgames.model.games.hiddenobject.HiddenObjectTapResult
import com.xanticious.androidgames.state.games.hiddenobject.HiddenObjectPhase
import com.xanticious.androidgames.state.games.hiddenobject.HiddenObjectStateMachine
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
import kotlin.math.roundToInt

@Composable
fun HiddenObjectGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { HiddenObjectController() }
    val machine = remember { HiddenObjectStateMachine() }
    val phase by machine.phase.collectAsState()

    var selectedDifficulty by rememberSaveable { mutableStateOf(difficulty) }
    var timeLimitSeconds by rememberSaveable { mutableFloatStateOf(0f) }
    var showLabel by rememberSaveable { mutableStateOf(true) }
    var wrongPulse by rememberSaveable { mutableStateOf(true) }
    val config = remember(selectedDifficulty, timeLimitSeconds, showLabel, wrongPulse) {
        controller.configFor(selectedDifficulty).copy(
            timeLimitSeconds = timeLimitSeconds,
            showObjectLabel = showLabel,
            wrongTapPulse = wrongPulse
        )
    }

    var round by remember { mutableStateOf<HiddenObjectRound?>(null) }
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
            HiddenObjectPhase.GENERATING -> {
                round = controller.generateRound(config)
                zoom = 1f
                panX = 0f
                panY = 0f
                machine.sceneReady()
            }
            HiddenObjectPhase.HINT_COOLDOWN -> {
                round = round?.let(controller::useHint)
                round?.target?.position?.let { target ->
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

    GameLoop(running = phase == HiddenObjectPhase.PLAYING || phase == HiddenObjectPhase.HINT_COOLDOWN) { dt ->
        round = round?.let { current ->
            val updated = controller.advanceTime(current, dt)
            if (controller.isTimeExpired(updated, config)) machine.timerExpired()
            updated
        }
    }

    GameScaffold(
        title = "Hidden Object",
        onExit = onExit,
        hud = {
            if (phase != HiddenObjectPhase.SETUP && phase != HiddenObjectPhase.HOW_TO_PLAY && phase != HiddenObjectPhase.IDLE) {
                val current = round
                GameHud(
                    left = current?.scene?.name ?: "Scene",
                    center = timerText(current?.elapsedSeconds ?: 0f, config.timeLimitSeconds),
                    right = if (phase == HiddenObjectPhase.HINT_COOLDOWN) "Hint cooling" else "Hint ready"
                )
            }
        },
        status = {
            val current = round
            when (phase) {
                HiddenObjectPhase.PLAYING, HiddenObjectPhase.HINT_COOLDOWN -> if (current != null) {
                    ObjectBanner(current, showLabel = config.showObjectLabel, onHint = machine::hintRequested)
                }
                HiddenObjectPhase.ROUND_COMPLETE -> if (current != null) {
                    VictoryPanel(
                        score = current.wrongTaps,
                        bestScore = current.wrongTaps,
                        stars = if (current.wrongTaps == 0 && current.hintsUsed == 0) 3 else 2,
                        onReplay = machine::nextRound,
                        onMenu = onExit,
                        headline = "Found ${current.target.name}!",
                        primaryLabel = "Next"
                    )
                }
                HiddenObjectPhase.GAME_OVER -> if (current != null) {
                    DefeatPanel(
                        score = current.wrongTaps,
                        bestScore = current.wrongTaps,
                        onTryAgain = machine::retry,
                        onMenu = onExit,
                        headline = "Time Expired: ${current.target.name}"
                    )
                }
                else -> Unit
            }
        }
    ) {
        when (phase) {
            HiddenObjectPhase.SETUP -> HiddenObjectSetup(
                difficulty = selectedDifficulty,
                onDifficulty = { selectedDifficulty = it },
                timeLimitSeconds = timeLimitSeconds,
                onTimeLimit = { timeLimitSeconds = it },
                showLabel = showLabel,
                onShowLabel = { showLabel = it },
                wrongPulse = wrongPulse,
                onWrongPulse = { wrongPulse = it },
                onHowToPlay = machine::openHowToPlay,
                onStart = machine::confirmSettings
            )
            HiddenObjectPhase.HOW_TO_PLAY -> HiddenObjectHowToPlay(onBack = machine::backToSetup)
            else -> HiddenObjectBoard(
                round = round,
                pulse = pulse,
                zoom = zoom,
                panX = panX,
                panY = panY,
                transformState = transformState,
                inspectPoint = inspectPoint,
                onInspect = { inspectPoint = it },
                onTap = { point ->
                    val current = round
                    if (current != null && phase == HiddenObjectPhase.PLAYING) {
                        val result = controller.tap(current, config, point)
                        round = result.first
                        if (result.second == HiddenObjectTapResult.FOUND) machine.objectFound() else {
                            if (config.wrongTapPulse) pulse = true
                            machine.wrongTap()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun HiddenObjectSetup(
    difficulty: GameDifficulty,
    onDifficulty: (GameDifficulty) -> Unit,
    timeLimitSeconds: Float,
    onTimeLimit: (Float) -> Unit,
    showLabel: Boolean,
    onShowLabel: (Boolean) -> Unit,
    wrongPulse: Boolean,
    onWrongPulse: (Boolean) -> Unit,
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
            listOf(0f to "Off", 30f to "30 s", 60f to "1 min", 120f to "2 min").forEach { option ->
                FilterChip(selected = timeLimitSeconds == option.first, onClick = { onTimeLimit(option.first) }, label = { Text(option.second) })
            }
        }
        SettingSwitch("Object label", showLabel, onShowLabel)
        SettingSwitch("Wrong tap pulse", wrongPulse, onWrongPulse)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStart) { Text("Start") }
            OutlinedButton(onClick = onHowToPlay) { Text("How to Play") }
        }
    }
}

@Composable
private fun HiddenObjectHowToPlay(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("How to Play", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Read the banner below the scene to learn which object to find.")
        Text("Pinch to zoom, drag with two fingers to pan, and tap the hidden object.")
        Text("Wrong taps have no penalty. Use Hint to zoom near the target, then keep searching.")
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
private fun ObjectBanner(round: HiddenObjectRound, showLabel: Boolean, onHint: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (showLabel) "Find: ${round.target.name}" else "Find the object",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedButton(onClick = onHint) { Text("Hint") }
    }
}

@Composable
private fun HiddenObjectBoard(
    round: HiddenObjectRound?,
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
                            val normalized = Vec2(
                                ((offset.x - panX) / zoom / size.width).coerceIn(0f, 1f),
                                ((offset.y - panY) / zoom / size.height).coerceIn(0f, 1f)
                            )
                            onTap(normalized)
                        }
                    )
                }
        ) {
            withTransform({ translate(panX, panY); scale(zoom, zoom) }) {
                drawGeneratedScene()
                round?.objects?.forEach { drawPlacedObject(it) }
                inspectPoint?.let { point -> drawInspectionRing(point) }
            }
            if (pulse) drawRect(GameHazard.copy(alpha = 0.18f), size = size)
        }
        if (round == null) Text("Generating scene…", modifier = Modifier.align(Alignment.Center))
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
    drawRect(Dark1.copy(alpha = 0.65f), topLeft = Offset(0f, size.height * 0.65f), size = Size(size.width, size.height * 0.35f))
    repeat(7) { index ->
        val x = size.width * (0.08f + index * 0.14f)
        drawLine(GameCourtLine.copy(alpha = 0.35f), Offset(x, size.height), Offset(x + size.width * 0.04f, size.height * 0.25f), strokeWidth = size.minDimension * 0.012f)
    }
    repeat(5) { index ->
        val center = Offset(size.width * (0.12f + index * 0.18f), size.height * (0.70f + (index % 2) * 0.10f))
        drawCircle(Aqua4.copy(alpha = 0.35f), radius = size.minDimension * (0.08f + index * 0.008f), center = center)
    }
}

private fun DrawScope.drawPlacedObject(obj: PlacedHiddenObject) {
    val center = Offset(obj.position.x * size.width, obj.position.y * size.height)
    val radius = obj.size * size.minDimension / 2f
    rotate(obj.rotationDegrees, center) {
        when (obj.kind) {
            HiddenObjectKind.ANCHOR -> drawAnchor(center, radius)
            HiddenObjectKind.BELL -> drawBell(center, radius)
            HiddenObjectKind.BOOT -> drawBoot(center, radius)
            HiddenObjectKind.BOTTLE -> drawBottle(center, radius)
            HiddenObjectKind.BRASS_KEY -> drawKey(center, radius)
            HiddenObjectKind.COMPASS -> drawCompass(center, radius)
            HiddenObjectKind.CORAL_FAN -> drawCoral(center, radius)
            HiddenObjectKind.CRAB -> drawCrab(center, radius)
            HiddenObjectKind.CROWN -> drawCrown(center, radius)
            HiddenObjectKind.FISH -> drawFish(center, radius)
            HiddenObjectKind.GEM -> drawGem(center, radius)
            HiddenObjectKind.HEART -> drawHeart(center, radius)
            HiddenObjectKind.LANTERN -> drawLantern(center, radius)
            HiddenObjectKind.LEAF -> drawLeaf(center, radius)
            HiddenObjectKind.MASK -> drawMask(center, radius)
            HiddenObjectKind.MOON -> drawMoon(center, radius)
            HiddenObjectKind.PEARL -> drawPearl(center, radius)
            HiddenObjectKind.ROPE -> drawRope(center, radius)
            HiddenObjectKind.SHELL -> drawShell(center, radius)
            HiddenObjectKind.STARFISH -> drawStar(center, radius)
        }
    }
}

private fun DrawScope.drawKey(c: Offset, r: Float) { drawCircle(GameAccent, r * 0.32f, c.copy(x = c.x - r * 0.35f), style = Stroke(r * 0.12f)); drawLine(GameAccent, c, c.copy(x = c.x + r * 0.65f), r * 0.16f); drawLine(GameAccent, c.copy(x = c.x + r * 0.35f), c.copy(x = c.x + r * 0.35f, y = c.y + r * 0.35f), r * 0.12f) }
private fun DrawScope.drawFish(c: Offset, r: Float) { drawOval(GamePlayer, c.topLeft(r, 0.62f), Size(r * 1.35f, r * 0.85f)); drawTriangle(GamePlayer, c.copy(x = c.x + r * 0.75f), r * 0.55f); drawCircle(Dark2, r * 0.07f, c.copy(x = c.x - r * 0.35f, y = c.y - r * 0.10f)) }
private fun DrawScope.drawShell(c: Offset, r: Float) { drawArc(Aqua1, 180f, 180f, true, c.topLeft(r, 1f), Size(r * 2f, r * 1.6f)); repeat(4) { drawLine(Aqua3, c.copy(y = c.y + r * 0.55f), c.copy(x = c.x - r + it * r * 0.65f, y = c.y - r * 0.15f), r * 0.05f) } }
private fun DrawScope.drawStar(c: Offset, r: Float) { val p = Path(); repeat(10) { i -> val a = -1.57f + i * 0.628f; val rr = if (i % 2 == 0) r else r * 0.45f; val o = Offset(c.x + kotlin.math.cos(a) * rr, c.y + kotlin.math.sin(a) * rr); if (i == 0) p.moveTo(o.x, o.y) else p.lineTo(o.x, o.y) }; p.close(); drawPath(p, GameAccent) }
private fun DrawScope.drawAnchor(c: Offset, r: Float) { drawLine(GameNeutral, c.copy(y = c.y - r), c.copy(y = c.y + r * 0.55f), r * 0.13f); drawCircle(GameNeutral, r * 0.22f, c.copy(y = c.y - r * 0.8f), style = Stroke(r * 0.10f)); drawArc(GameNeutral, 25f, 130f, false, c.topLeft(r, 0.95f), Size(r * 1.9f, r * 1.9f), style = Stroke(r * 0.13f)) }
private fun DrawScope.drawPearl(c: Offset, r: Float) { drawCircle(Aqua0, r * 0.62f, c); drawCircle(Aqua1.copy(alpha = 0.7f), r * 0.18f, c.copy(x = c.x - r * 0.18f, y = c.y - r * 0.18f)) }
private fun DrawScope.drawBottle(c: Offset, r: Float) { drawRoundRect(Aqua3, c.topLeft(r, 0.45f), Size(r * 0.9f, r * 1.45f)); drawRect(Aqua2, c.copy(x = c.x - r * 0.18f, y = c.y - r).toTopLeft(), Size(r * 0.36f, r * 0.45f)) }
private fun DrawScope.drawBoot(c: Offset, r: Float) { drawRect(GameNeutral, c.copy(x = c.x - r * 0.45f, y = c.y - r * 0.75f).toTopLeft(), Size(r * 0.7f, r * 1.2f)); drawRoundRect(GameNeutral, c.copy(x = c.x - r * 0.45f, y = c.y + r * 0.15f).toTopLeft(), Size(r * 1.25f, r * 0.45f)) }
private fun DrawScope.drawCompass(c: Offset, r: Float) { drawCircle(Aqua0, r * 0.75f, c, style = Stroke(r * 0.12f)); drawLine(GameHazard, c.copy(y = c.y + r * 0.55f), c.copy(y = c.y - r * 0.55f), r * 0.08f); drawLine(Aqua2, c.copy(x = c.x - r * 0.45f), c.copy(x = c.x + r * 0.45f), r * 0.06f) }
private fun DrawScope.drawCrab(c: Offset, r: Float) { drawOval(GameHazard, c.topLeft(r, 0.65f), Size(r * 1.3f, r)); drawCircle(GameHazard, r * 0.25f, c.copy(x = c.x - r * 0.85f)); drawCircle(GameHazard, r * 0.25f, c.copy(x = c.x + r * 0.85f)) }
private fun DrawScope.drawGem(c: Offset, r: Float) { drawDiamond(c, r, Aqua2); drawPath(Path().apply { moveTo(c.x, c.y - r); lineTo(c.x, c.y + r); lineTo(c.x + r, c.y); close() }, Aqua1.copy(alpha = 0.55f)) }
private fun DrawScope.drawLeaf(c: Offset, r: Float) { drawOval(GameSuccess, c.topLeft(r, 0.55f), Size(r * 1.1f, r * 1.8f)); drawLine(Aqua4, c.copy(y = c.y + r * 0.75f), c.copy(y = c.y - r * 0.7f), r * 0.06f) }
private fun DrawScope.drawRope(c: Offset, r: Float) { drawCircle(GameNeutral, r * 0.65f, c, style = Stroke(r * 0.18f)); drawCircle(GameCourt, r * 0.30f, c) }
private fun DrawScope.drawCrown(c: Offset, r: Float) { val p = Path().apply { moveTo(c.x - r, c.y + r * 0.45f); lineTo(c.x - r * 0.75f, c.y - r * 0.45f); lineTo(c.x - r * 0.25f, c.y + r * 0.05f); lineTo(c.x, c.y - r * 0.65f); lineTo(c.x + r * 0.25f, c.y + r * 0.05f); lineTo(c.x + r * 0.75f, c.y - r * 0.45f); lineTo(c.x + r, c.y + r * 0.45f); close() }; drawPath(p, GameAccent) }
private fun DrawScope.drawBell(c: Offset, r: Float) { drawArc(GameAccent, 180f, 180f, true, c.topLeft(r, 0.8f), Size(r * 1.6f, r * 1.5f)); drawCircle(GameHazard, r * 0.16f, c.copy(y = c.y + r * 0.65f)) }
private fun DrawScope.drawHeart(c: Offset, r: Float) { drawCircle(GameHazard, r * 0.38f, c.copy(x = c.x - r * 0.28f, y = c.y - r * 0.2f)); drawCircle(GameHazard, r * 0.38f, c.copy(x = c.x + r * 0.28f, y = c.y - r * 0.2f)); drawTriangle(GameHazard, c.copy(y = c.y + r * 0.10f), r * 0.82f) }
private fun DrawScope.drawMoon(c: Offset, r: Float) { drawCircle(Aqua0, r * 0.72f, c); drawCircle(GameCourt, r * 0.72f, c.copy(x = c.x + r * 0.35f, y = c.y - r * 0.08f)) }
private fun DrawScope.drawMask(c: Offset, r: Float) { drawOval(Aqua3, c.topLeft(r, 0.75f), Size(r * 1.5f, r)); drawCircle(Dark2, r * 0.18f, c.copy(x = c.x - r * 0.35f)); drawCircle(Dark2, r * 0.18f, c.copy(x = c.x + r * 0.35f)) }
private fun DrawScope.drawLantern(c: Offset, r: Float) { drawRoundRect(GameAccent, c.topLeft(r, 0.65f), Size(r * 1.3f, r * 1.2f)); drawRect(Dark2.copy(alpha = 0.35f), c.copy(x = c.x - r * 0.35f, y = c.y - r * 0.35f).toTopLeft(), Size(r * 0.7f, r * 0.7f)) }
private fun DrawScope.drawCoral(c: Offset, r: Float) { drawLine(Aqua2, c.copy(y = c.y + r), c.copy(y = c.y - r), r * 0.12f); drawLine(Aqua2, c, c.copy(x = c.x - r * 0.65f, y = c.y - r * 0.45f), r * 0.10f); drawLine(Aqua2, c.copy(y = c.y - r * 0.15f), c.copy(x = c.x + r * 0.65f, y = c.y - r * 0.65f), r * 0.10f) }

private fun Offset.topLeft(r: Float, scale: Float) = Offset(x - r * scale, y - r * scale)
private fun Offset.toTopLeft() = Offset(x, y)
private fun DrawScope.drawTriangle(color: androidx.compose.ui.graphics.Color, c: Offset, r: Float) { val p = Path().apply { moveTo(c.x - r, c.y - r); lineTo(c.x + r, c.y); lineTo(c.x - r, c.y + r); close() }; drawPath(p, color) }
private fun DrawScope.drawDiamond(c: Offset, r: Float, color: androidx.compose.ui.graphics.Color) { val p = Path().apply { moveTo(c.x, c.y - r); lineTo(c.x + r, c.y); lineTo(c.x, c.y + r); lineTo(c.x - r, c.y); close() }; drawPath(p, color) }
