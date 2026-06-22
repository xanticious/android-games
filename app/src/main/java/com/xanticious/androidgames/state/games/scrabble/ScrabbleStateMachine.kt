package com.xanticious.androidgames.state.games.scrabble

import com.xanticious.androidgames.controller.games.scrabble.ScrabbleRules
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.scrabble.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.DefaultState
import ru.nsk.kstatemachine.state.addInitialState
import ru.nsk.kstatemachine.state.addState
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.statemachine.createStateMachineBlocking
import ru.nsk.kstatemachine.statemachine.processEventByLaunch
import ru.nsk.kstatemachine.transition.onTriggered
import kotlin.random.Random

/** High-level Scrabble game phases. */
enum class ScrabblePhase {
    SETUP, PLAYING, AI_THINKING, GAME_OVER
}

private sealed class GameState : DefaultState() {
    data object Setup : GameState()
    data object Playing : GameState()
    data object AiThinking : GameState()
    data object GameOver : GameState()
}

private sealed interface GameEvent : Event {
    data class GameStarted(val difficulty: ScrabbleDifficulty, val wordData: WordData) : GameEvent
    data class TilePlaced(val position: Position, val tile: ScrabbleTile) : GameEvent
    data object TileRecalled : GameEvent
    data object RackShuffled : GameEvent
    data object PlaySubmitted : GameEvent
    data object PassTurn : GameEvent
    data object AiMoveComplete : GameEvent
    data object GameEnded : GameEvent
    data object Rematch : GameEvent
}

/**
 * State machine for regular Scrabble (vs AI or solitaire).
 * Manages turn flow, move validation, scoring, and AI moves.
 */
class ScrabbleStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val random: Random = Random.Default
) {
    private val _phase = MutableStateFlow(ScrabblePhase.SETUP)
    val phase: StateFlow<ScrabblePhase> = _phase.asStateFlow()

    private val _gameState = MutableStateFlow(ScrabbleGameState())
    val gameState: StateFlow<ScrabbleGameState> = _gameState.asStateFlow()

    private var currentDifficulty = ScrabbleDifficulty.MEDIUM
    private lateinit var wordData: WordData

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(GameState.Setup) {
            transition<GameEvent.GameStarted> {
                targetState = GameState.Playing
                onTriggered {
                    startNewGame()
                    _phase.value = ScrabblePhase.PLAYING
                }
            }
        }

        addState(GameState.Playing) {
            transition<GameEvent.TilePlaced> {
                targetState = GameState.Playing
                onTriggered {
                    // placeTile already handled
                }
            }

            transition<GameEvent.TileRecalled> {
                targetState = GameState.Playing
                onTriggered {
                    recallTiles()
                }
            }

            transition<GameEvent.RackShuffled> {
                targetState = GameState.Playing
                onTriggered {
                    shuffleRack()
                }
            }

            transition<GameEvent.PlaySubmitted> {
                targetState = GameState.Playing
                onTriggered {
                    submitPlay()
                }
            }

            transition<GameEvent.PassTurn> {
                targetState = GameState.AiThinking
                onTriggered {
                    passTurn()
                    _phase.value = ScrabblePhase.AI_THINKING
                }
            }

            transition<GameEvent.GameEnded> {
                targetState = GameState.GameOver
                onTriggered {
                    _phase.value = ScrabblePhase.GAME_OVER
                }
            }
        }

        addState(GameState.AiThinking) {
            transition<GameEvent.AiMoveComplete> {
                targetState = GameState.Playing
                onTriggered {
                    makeAiMove()
                    _phase.value = ScrabblePhase.PLAYING
                }
            }

            transition<GameEvent.GameEnded> {
                targetState = GameState.GameOver
                onTriggered {
                    _phase.value = ScrabblePhase.GAME_OVER
                }
            }
        }

        addState(GameState.GameOver) {
            transition<GameEvent.Rematch> {
                targetState = GameState.Setup
                onTriggered {
                    _phase.value = ScrabblePhase.SETUP
                    _gameState.value = ScrabbleGameState()
                }
            }
        }
    }

    fun startGame(difficulty: ScrabbleDifficulty, wordData: WordData) {
        currentDifficulty = difficulty
        this.wordData = wordData
        machine.processEventByLaunch(GameEvent.GameStarted(difficulty, wordData), scope)
    }

    fun userPlaceTile(position: Position, tile: ScrabbleTile) {
        placeTile(position, tile)
        machine.processEventByLaunch(GameEvent.TilePlaced(position, tile), scope)
    }

    fun userRecallTiles() {
        machine.processEventByLaunch(GameEvent.TileRecalled, scope)
    }

    fun userShuffleRack() {
        machine.processEventByLaunch(GameEvent.RackShuffled, scope)
    }

    fun userSubmitPlay() {
        machine.processEventByLaunch(GameEvent.PlaySubmitted, scope)
    }

    fun pass() {
        machine.processEventByLaunch(GameEvent.PassTurn, scope)
    }

    fun aiMoveComplete() {
        machine.processEventByLaunch(GameEvent.AiMoveComplete, scope)
    }

    fun endGame() {
        machine.processEventByLaunch(GameEvent.GameEnded, scope)
    }

    fun rematch() {
        machine.processEventByLaunch(GameEvent.Rematch, scope)
    }

    private fun startNewGame() {
        val shuffled = ScrabbleTile.createBag().shuffled(random)
        val (playerRack, afterPlayer) = ScrabbleRules.drawTiles(emptyList(), shuffled, random)
        val (aiRack, finalBag) = ScrabbleRules.drawTiles(emptyList(), afterPlayer, random)

        _gameState.value = ScrabbleGameState(
            playerRack = playerRack,
            aiRack = aiRack,
            bag = finalBag
        )
    }

    private fun placeTile(position: Position, tile: ScrabbleTile) {
        val state = _gameState.value
        if (state.currentTurn != ScrabbleGameState.Player.HUMAN) return

        // Check if tile is in player rack
        if (!state.playerRack.contains(tile)) return

        val newTentative = state.tentativeTiles + PlacedTile(position, tile)
        val newRack = state.playerRack.toMutableList().apply { remove(tile) }

        _gameState.value = state.copy(
            tentativeTiles = newTentative,
            playerRack = newRack
        )
    }

    private fun recallTiles() {
        val state = _gameState.value
        val returnedTiles = state.tentativeTiles.map { it.tile }
        _gameState.value = state.copy(
            tentativeTiles = emptyList(),
            playerRack = state.playerRack + returnedTiles
        )
    }

    private fun shuffleRack() {
        val state = _gameState.value
        _gameState.value = state.copy(
            playerRack = state.playerRack.shuffled(random)
        )
    }

    private fun submitPlay() {
        val state = _gameState.value
        if (state.tentativeTiles.isEmpty()) return

        val direction = inferDirection(state.tentativeTiles)
        val move = ScrabbleMove(state.tentativeTiles, direction)

        val validation = ScrabbleRules.validateMove(state.board, move, wordData)
        if (validation !is ScrabbleRules.MoveValidation.Valid) {
            // Move invalid - keep tentative tiles for now
            return
        }

        val score = ScrabbleRules.scoreMove(state.board, move)
        val newBoard = state.board.withTiles(state.tentativeTiles)
        val (newRack, newBag) = ScrabbleRules.drawTiles(state.playerRack, state.bag, random)

        _gameState.value = state.copy(
            board = newBoard,
            playerRack = newRack,
            bag = newBag,
            playerScore = state.playerScore + score,
            tentativeTiles = emptyList(),
            lastMove = move.copy(score = score),
            currentTurn = ScrabbleGameState.Player.AI
        )

        // Trigger AI move
        machine.processEventByLaunch(GameEvent.AiMoveComplete, scope)
    }

    private fun passTurn() {
        val state = _gameState.value
        _gameState.value = state.copy(
            currentTurn = ScrabbleGameState.Player.AI,
            tentativeTiles = emptyList()
        )
    }

    private fun makeAiMove() {
        val state = _gameState.value

        val moves = ScrabbleRules.generateMoves(
            state.board,
            state.aiRack,
            wordData,
            maxMoves = when (currentDifficulty) {
                ScrabbleDifficulty.EASY -> 5
                ScrabbleDifficulty.MEDIUM -> 20
                ScrabbleDifficulty.HARD -> 100
            }
        )

        val selectedMove = when (currentDifficulty) {
            ScrabbleDifficulty.EASY -> moves.randomOrNull(random)
            ScrabbleDifficulty.MEDIUM -> moves.getOrNull(random.nextInt(0, 3.coerceAtMost(moves.size)))
            ScrabbleDifficulty.HARD -> moves.firstOrNull()
        }

        if (selectedMove != null) {
            val newBoard = state.board.withTiles(selectedMove.tiles)
            val usedTiles = selectedMove.tiles.map { it.tile }
            val remainingRack = state.aiRack.toMutableList().apply {
                usedTiles.forEach { remove(it) }
            }
            val (newAiRack, newBag) = ScrabbleRules.drawTiles(remainingRack, state.bag, random)

            _gameState.value = state.copy(
                board = newBoard,
                aiRack = newAiRack,
                bag = newBag,
                aiScore = state.aiScore + selectedMove.score,
                lastMove = selectedMove,
                currentTurn = ScrabbleGameState.Player.HUMAN
            )
        } else {
            // AI passes
            _gameState.value = state.copy(
                currentTurn = ScrabbleGameState.Player.HUMAN
            )
        }

        // Check game over
        if (state.bag.isEmpty() && (state.playerRack.isEmpty() || state.aiRack.isEmpty())) {
            val winner = if (state.playerScore > state.aiScore) {
                ScrabbleGameState.Player.HUMAN
            } else {
                ScrabbleGameState.Player.AI
            }
            _gameState.value = _gameState.value.copy(
                gameOver = true,
                winner = winner
            )
            machine.processEventByLaunch(GameEvent.GameEnded, scope)
        }
    }

    private fun inferDirection(tiles: List<PlacedTile>): Direction {
        if (tiles.size <= 1) return Direction.HORIZONTAL
        val rows = tiles.map { it.position.row }.distinct()
        return if (rows.size == 1) Direction.HORIZONTAL else Direction.VERTICAL
    }
}
