package com.xanticious.androidgames.view.games.matchthree

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.matchthree.MatchThreeController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.matchthree.GemType
import com.xanticious.androidgames.model.games.matchthree.MatchThreeBoard
import com.xanticious.androidgames.model.games.matchthree.MatchThreeState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.PuzzleHighlight
import com.xanticious.androidgames.ui.theme.PuzzleHueBlue
import com.xanticious.androidgames.ui.theme.PuzzleHueGreen
import com.xanticious.androidgames.ui.theme.PuzzleHueOrange
import com.xanticious.androidgames.ui.theme.PuzzleHueTeal
import com.xanticious.androidgames.ui.theme.PuzzleHueViolet
import com.xanticious.androidgames.ui.theme.PuzzleHueYellow
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.puzzle.OptionChips
import com.xanticious.androidgames.view.common.puzzle.PuzzleActionBar
import com.xanticious.androidgames.view.common.puzzle.PuzzleBoard
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Match Three (Zen) — swap adjacent gems to create matches of 3 or more. Endless;
 * no timer, no lose condition. Reshuffles automatically when no valid move exists.
 * (`design/puzzle-games/match-three/match-three-design.md`)
 */
@Composable
fun MatchThreeGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { MatchThreeController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val defaultSize = when (difficulty) {
        GameDifficulty.EASY -> 6
        GameDifficulty.MEDIUM -> 7
        GameDifficulty.HARD -> 8
    }
    var boardSize by remember { mutableIntStateOf(defaultSize) }
    var gemTypes by remember { mutableIntStateOf(6) }
    var gameState by remember {
        mutableStateOf(MatchThreeState(board = MatchThreeBoard.empty(defaultSize, defaultSize)))
    }

    fun deal() {
        gameState = MatchThreeState(
            board = controller.newBoard(boardSize, boardSize, gemTypes, Random.Default),
            gemTypes = gemTypes
        )
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Match Three", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Match Three",
                onHowToPlay = machine::openHowToPlay,
                onStart = { deal(); machine.startGame() }
            ) {
                OptionChips(
                    label = "Board size",
                    options = controller.boardSizeOptions,
                    selected = boardSize,
                    onSelect = { boardSize = it },
                    labelOf = { "${it}×$it" }
                )
                OptionChips(
                    label = "Gem types",
                    options = controller.gemTypeOptions,
                    selected = gemTypes,
                    onSelect = { gemTypes = it },
                    labelOf = { "$it types" }
                )
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Match Three", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                Text("Tap a gem to select it, then tap an adjacent gem to swap them.")
                Text("If the swap creates a line of 3 or more matching gems, they clear and new ones drop in from above.")
                Text("If no match is created, the gems snap back. Invalid swaps do not cost a move.")
                Text("Cascades fire automatically — when falling gems land in matching positions, those are cleared too.")
                Text("If no valid swap exists, the board reshuffles automatically.")
                Text("There is nothing to win or lose — play as long as you like.")
            }
        }

        else -> {
            var selectedCell by remember { mutableStateOf<GridPos?>(null) }
            var dragStart by remember { mutableStateOf<GridPos?>(null) }

            fun performSwap(a: GridPos, b: GridPos) {
                val result = controller.applySwap(
                    gameState.board, a, b, gameState.gemTypes, Random.Default
                )
                if (result.valid) {
                    val needsReshuffle = !controller.hasValidMove(result.board)
                    val finalBoard = if (needsReshuffle)
                        controller.reshuffle(result.board, Random.Default)
                    else result.board
                    gameState = gameState.copy(
                        board = finalBoard,
                        selected = null,
                        totalCleared = gameState.totalCleared + result.totalCleared,
                        swapCount = gameState.swapCount + 1,
                        cascadeBest = maxOf(gameState.cascadeBest, result.cascadeCount)
                    )
                }
            }

            fun onCellTap(pos: GridPos) {
                val sel = selectedCell
                when {
                    sel == null -> selectedCell = pos
                    sel == pos -> selectedCell = null
                    controller.areAdjacent(sel, pos) -> {
                        selectedCell = null
                        performSwap(sel, pos)
                    }
                    else -> selectedCell = pos
                }
            }

            GameScaffold(
                title = "Match Three",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Gems: ${gameState.totalCleared}",
                        center = "${boardSize}×$boardSize",
                        right = "Cascade: ${gameState.cascadeBest}"
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        PuzzleActionBar(
                            status = "Swaps: ${gameState.swapCount}",
                            onNew = { deal() }
                        )
                    }
                }
            ) {
                GemBoard(
                    board = gameState.board,
                    selectedCell = selectedCell,
                    onCellTap = ::onCellTap,
                    onCellDrag = { pos ->
                        if (dragStart == null) {
                            dragStart = pos
                        } else {
                            val start = dragStart!!
                            if (start != pos && controller.areAdjacent(start, pos)) {
                                dragStart = null
                                selectedCell = null
                                performSwap(start, pos)
                            }
                        }
                    },
                    onDragEnd = { dragStart = null }
                )
            }
        }
    }
}

// ── Shared gem board composable ───────────────────────────────────────────────

@Composable
internal fun GemBoard(
    board: MatchThreeBoard,
    selectedCell: GridPos?,
    onCellTap: ((GridPos) -> Unit)?,
    modifier: Modifier = Modifier,
    onCellDrag: ((GridPos) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null
) {
    PuzzleBoard(
        rows = board.rows,
        cols = board.cols,
        modifier = modifier,
        onCellTap = onCellTap,
        onCellDrag = onCellDrag,
        onDragEnd = onDragEnd,
        drawGridLines = true
    ) {
        val gem = board.get(current) ?: return@PuzzleBoard
        val isSelected = current == selectedCell
        val inset = cellSize * 0.1f
        val r = cellSize / 2f - inset
        val col = gemColor(gem)

        if (isSelected) {
            // Selection ring behind the gem
            drawCircle(
                color = Color.White,
                radius = r + cellSize * 0.07f,
                center = center,
                style = Stroke(width = cellSize * 0.06f)
            )
        }

        when (gem) {
            GemType.AQUA -> drawCircle(col, r, center)
            GemType.TEAL -> drawDiamond(col, center, r)
            GemType.CORAL -> drawTriangle(col, center, r)
            GemType.GREEN -> drawHexagon(col, center, r)
            GemType.SAND -> drawStar(col, center, r, points = 5)
            GemType.DEEP -> drawRoundRect(
                color = col,
                topLeft = Offset(center.x - r * 0.82f, center.y - r * 0.82f),
                size = androidx.compose.ui.geometry.Size(r * 1.64f, r * 1.64f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.28f)
            )
        }

        // Inner highlight for depth
        drawCircle(
            color = Color.White.copy(alpha = 0.22f),
            radius = r * 0.38f,
            center = Offset(center.x - r * 0.18f, center.y - r * 0.18f)
        )

        if (isSelected) {
            drawCircle(
                color = PuzzleHighlight.copy(alpha = 0.35f),
                radius = r,
                center = center
            )
        }
    }
}

internal fun gemColor(gem: GemType): androidx.compose.ui.graphics.Color = when (gem) {
    GemType.AQUA  -> PuzzleHueTeal
    GemType.TEAL  -> PuzzleHueBlue
    GemType.CORAL -> PuzzleHueOrange
    GemType.GREEN -> PuzzleHueGreen
    GemType.SAND  -> PuzzleHueYellow
    GemType.DEEP  -> PuzzleHueViolet
}

// ── Shape drawing helpers (all are DrawScope extension functions) ──────────────

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDiamond(
    color: Color,
    center: Offset,
    r: Float
) {
    val path = Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + r * 0.72f, center.y)
        lineTo(center.x, center.y + r)
        lineTo(center.x - r * 0.72f, center.y)
        close()
    }
    drawPath(path, color)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTriangle(
    color: Color,
    center: Offset,
    r: Float
) {
    val path = Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + r * 0.866f, center.y + r * 0.5f)
        lineTo(center.x - r * 0.866f, center.y + r * 0.5f)
        close()
    }
    drawPath(path, color)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHexagon(
    color: Color,
    center: Offset,
    r: Float
) {
    val path = Path()
    for (i in 0 until 6) {
        val angle = (PI / 6 + i * PI / 3).toFloat()
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(
    color: Color,
    center: Offset,
    r: Float,
    points: Int = 5
) {
    val innerR = r * 0.42f
    val path = Path()
    for (i in 0 until points * 2) {
        val angle = (i * PI / points - PI / 2).toFloat()
        val radius = if (i % 2 == 0) r else innerR
        val x = center.x + radius * cos(angle)
        val y = center.y + radius * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}
