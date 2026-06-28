package com.xanticious.androidgames.view.games.sokoban

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.sokoban.SokobanController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.puzzle.Direction
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.model.games.puzzle.step
import com.xanticious.androidgames.model.games.sokoban.SokobanCell
import com.xanticious.androidgames.model.games.sokoban.SokobanLevel
import com.xanticious.androidgames.model.games.sokoban.SokobanState
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.PuzzleCell
import com.xanticious.androidgames.ui.theme.PuzzleHighlight
import com.xanticious.androidgames.ui.theme.PuzzlePlayerAlt
import com.xanticious.androidgames.ui.theme.PuzzleSolved
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.puzzle.DifficultyChips
import com.xanticious.androidgames.view.common.puzzle.HowToPlaySection
import com.xanticious.androidgames.view.common.puzzle.PuzzleActionBar
import com.xanticious.androidgames.view.common.puzzle.PuzzleBoard
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen

/**
 * Sokoban — push every crate onto every goal to solve the level
 * (`design/puzzle-games/sokoban`). Self-configured: owns Settings, How to Play,
 * and the board screen via the shared [PuzzleStateMachine].
 *
 * Input: tap a cell orthogonally adjacent to the worker to step/push that way.
 */
@Composable
fun SokobanGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { SokobanController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val levelSet = remember(difficulty) {
        when (difficulty) {
            GameDifficulty.EASY -> controller.allLevels.subList(0, 2)
            GameDifficulty.MEDIUM -> controller.allLevels.subList(2, 4)
            GameDifficulty.HARD -> controller.allLevels.subList(4, 6)
        }
    }

    var selectedDifficulty by remember { mutableStateOf(difficulty) }
    var levelIndex by rememberSaveable { mutableIntStateOf(0) }
    var state by remember {
        mutableStateOf(controller.startLevel(levelSet[0], 0))
    }

    fun loadLevel(idx: Int) {
        val i = idx.coerceIn(0, levelSet.lastIndex)
        levelIndex = i
        state = controller.startLevel(levelSet[i], i)
    }

    val levelSetName = when (difficulty) {
        GameDifficulty.EASY -> "Starter"
        GameDifficulty.MEDIUM -> "Classic"
        GameDifficulty.HARD -> "Expert"
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Sokoban", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Sokoban",
                onHowToPlay = machine::openHowToPlay,
                onStart = { loadLevel(0); machine.startGame() }
            ) {
                Text("Level set: $levelSetName (${levelSet.size} levels)")
                Text("Tap a cell next to the worker to move. Walk into a crate to push it.")
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Sokoban", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                HowToPlaySection(title = "Goal") {
                    Text("Push every crate (□) onto every goal spot (◎). The level is solved when all crates are on goals.")
                }
                HowToPlaySection(title = "Movement") {
                    Text("Tap a cell that is directly up, down, left, or right of the worker to move there.")
                    Text("Walking into a crate pushes it one cell in the same direction — but only if there is empty space behind it.")
                }
                HowToPlaySection(title = "Tips") {
                    Text("Crates can never be pulled. Use Undo to reverse a bad push, or Reset to restart the level from scratch.")
                }
            }
        }

        else -> {
            val solved = phase == PuzzlePhase.SOLVED
            val hasNextLevel = levelIndex < levelSet.lastIndex

            GameScaffold(
                title = "Sokoban",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Moves ${state.moves}",
                        center = "Lvl ${levelIndex + 1}/${levelSet.size}",
                        right = "${controller.boxesOnGoals(state)}/${state.level.goals.size} crates"
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (solved) {
                            VictoryPanel(
                                score = state.moves,
                                bestScore = state.moves,
                                stars = if (!state.usedUndo) 3 else 2,
                                onReplay = {
                                    if (hasNextLevel) loadLevel(levelIndex + 1)
                                    else loadLevel(0)
                                    machine.retry()
                                },
                                onMenu = machine::newGame,
                                headline = "Level Clear!",
                                primaryLabel = if (hasNextLevel) "Next Level" else "Play Again"
                            )
                        } else {
                            PuzzleActionBar(
                                status = "Pushes ${state.pushes}",
                                onUndo = {
                                    state = controller.undo(state)
                                },
                                undoEnabled = state.canUndo,
                                onReset = {
                                    state = controller.startLevel(state.level, state.levelIndex)
                                },
                                onNew = {
                                    if (hasNextLevel) loadLevel(levelIndex + 1)
                                    else loadLevel(0)
                                }
                            )
                        }
                    }
                }
            ) {
                SokobanBoard(
                    state = state,
                    enabled = !solved,
                    onCellTap = { tapped ->
                        val direction = adjacentDirection(state.player, tapped)
                        if (direction != null) {
                            state = controller.move(state, direction)
                            if (controller.isSolved(state)) machine.solved()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SokobanBoard(
    state: SokobanState,
    enabled: Boolean,
    onCellTap: (GridPos) -> Unit
) {
    val level = state.level
    PuzzleBoard(
        rows = level.rows,
        cols = level.cols,
        drawGridLines = false,
        maxCellSize = 72.dp,
        onCellTap = if (enabled) onCellTap else null
    ) {
        val cellType = state.cell(current)
        val hasBox = current in state.boxes
        val isPlayer = current == state.player
        val isGoal = cellType == SokobanCell.GOAL

        // Background: walls are solid, floors and goals share the same light base.
        when (cellType) {
            SokobanCell.WALL -> drawRect(
                color = GameNeutral,
                topLeft = topLeft,
                size = cellSquare
            )
            SokobanCell.FLOOR, SokobanCell.GOAL -> drawRect(
                color = PuzzleCell,
                topLeft = topLeft,
                size = cellSquare
            )
        }

        // Goal marker: a ring drawn when the goal spot is uncovered.
        if (isGoal && !hasBox) {
            drawCircle(
                color = GameAccent,
                radius = cellSize * 0.22f,
                center = center,
                style = Stroke(width = cellSize * 0.09f)
            )
        }

        // Crate: rounded rectangle; tinted when sitting on a goal.
        if (hasBox) {
            val inset = cellSize * 0.1f
            val r = rect(inset)
            drawRoundRect(
                color = if (isGoal) PuzzleSolved else PuzzleHighlight,
                topLeft = Offset(r.left, r.top),
                size = Size(r.width, r.height),
                cornerRadius = CornerRadius(cellSize * 0.15f)
            )
        }

        // Worker: a filled circle on top of whatever is beneath.
        if (isPlayer) {
            drawCircle(
                color = PuzzlePlayerAlt,
                radius = cellSize * 0.32f,
                center = center
            )
        }
    }
}

/** Returns the [Direction] from [from] to [to] when they are orthogonally adjacent, else null. */
private fun adjacentDirection(from: GridPos, to: GridPos): Direction? =
    Direction.entries.firstOrNull { from.step(it) == to }
