package com.xanticious.androidgames.state.games.scrabblechallenge

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

/** High-level challenge phases. */
enum class ChallengePhase {
    SETUP, PLAYING, ROUND_RESULT, SESSION_SUMMARY
}

private sealed class ChallengeState : DefaultState() {
    data object Setup : ChallengeState()
    data object Playing : ChallengeState()
    data object RoundResult : ChallengeState()
    data object SessionSummary : ChallengeState()
}

private sealed interface ChallengeEvent : Event {
    data class SessionStarted(
        val density: BoardDensity,
        val rackDiff: RackDifficulty,
        val wordData: WordData,
        val personalBest: Int
    ) : ChallengeEvent
    data class TilePlaced(val position: Position, val tile: ScrabbleTile) : ChallengeEvent
    data object TileRecalled : ChallengeEvent
    data object RackShuffled : ChallengeEvent
    data object PlaySubmitted : ChallengeEvent
    data object RoundSkipped : ChallengeEvent
    data object NextRound : ChallengeEvent
    data object ViewSummary : ChallengeEvent
    data object NewSession : ChallengeEvent
}

/**
 * State machine for Scrabble Single Player Challenge (10 rounds).
 * Generates random boards/racks, validates plays, shows top-20.
 */
class ScrabbleChallengeStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val random: Random = Random.Default
) {
    private val _phase = MutableStateFlow(ChallengePhase.SETUP)
    val phase: StateFlow<ChallengePhase> = _phase.asStateFlow()

    private val _state = MutableStateFlow(ScrabbleChallengeState())
    val state: StateFlow<ScrabbleChallengeState> = _state.asStateFlow()

    private var currentDensity = BoardDensity.MEDIUM
    private var currentRackDiff = RackDifficulty.BALANCED
    private lateinit var wordData: WordData

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(ChallengeState.Setup) {
            transition<ChallengeEvent.SessionStarted> {
                targetState = ChallengeState.Playing
                onTriggered {
                    startNewSession(_state.value.personalBest)
                    _phase.value = ChallengePhase.PLAYING
                }
            }
        }

        addState(ChallengeState.Playing) {
            transition<ChallengeEvent.TilePlaced> {
                targetState = ChallengeState.Playing
                onTriggered {
                    // placeTile already handled
                }
            }

            transition<ChallengeEvent.TileRecalled> {
                targetState = ChallengeState.Playing
                onTriggered {
                    recallTiles()
                }
            }

            transition<ChallengeEvent.RackShuffled> {
                targetState = ChallengeState.Playing
                onTriggered {
                    shuffleRack()
                }
            }

            transition<ChallengeEvent.PlaySubmitted> {
                targetState = ChallengeState.RoundResult
                onTriggered {
                    submitPlay()
                    _phase.value = ChallengePhase.ROUND_RESULT
                }
            }

            transition<ChallengeEvent.RoundSkipped> {
                targetState = ChallengeState.RoundResult
                onTriggered {
                    skipRound()
                    _phase.value = ChallengePhase.ROUND_RESULT
                }
            }
        }

        addState(ChallengeState.RoundResult) {
            transition<ChallengeEvent.NextRound> {
                targetState = ChallengeState.Playing
                onTriggered {
                    nextRound()
                    _phase.value = ChallengePhase.PLAYING
                }
            }

            transition<ChallengeEvent.ViewSummary> {
                targetState = ChallengeState.SessionSummary
                onTriggered {
                    _phase.value = ChallengePhase.SESSION_SUMMARY
                }
            }
        }

        addState(ChallengeState.SessionSummary) {
            transition<ChallengeEvent.NewSession> {
                targetState = ChallengeState.Setup
                onTriggered {
                    _phase.value = ChallengePhase.SETUP
                    _state.value = ScrabbleChallengeState()
                }
            }
        }
    }

    fun startSession(
        density: BoardDensity,
        rackDiff: RackDifficulty,
        wordData: WordData,
        personalBest: Int
    ) {
        currentDensity = density
        currentRackDiff = rackDiff
        this.wordData = wordData
        _state.value = _state.value.copy(personalBest = personalBest)
        machine.processEventByLaunch(
            ChallengeEvent.SessionStarted(density, rackDiff, wordData, personalBest),
            scope
        )
    }

    fun userPlaceTile(position: Position, tile: ScrabbleTile) {
        placeTile(position, tile)
        machine.processEventByLaunch(ChallengeEvent.TilePlaced(position, tile), scope)
    }

    fun userRecallTiles() {
        machine.processEventByLaunch(ChallengeEvent.TileRecalled, scope)
    }

    fun userShuffleRack() {
        machine.processEventByLaunch(ChallengeEvent.RackShuffled, scope)
    }

    fun userSubmitPlay() {
        machine.processEventByLaunch(ChallengeEvent.PlaySubmitted, scope)
    }

    fun userSkipRound() {
        machine.processEventByLaunch(ChallengeEvent.RoundSkipped, scope)
    }

    fun userNextRound() {
        machine.processEventByLaunch(ChallengeEvent.NextRound, scope)
    }

    fun viewSummary() {
        machine.processEventByLaunch(ChallengeEvent.ViewSummary, scope)
    }

    fun newSession() {
        machine.processEventByLaunch(ChallengeEvent.NewSession, scope)
    }

    private fun startNewSession(personalBest: Int) {
        _state.value = ScrabbleChallengeState(personalBest = personalBest)
        setupRound()
    }

    private fun setupRound() {
        val (board, rack) = generateBoardAndRack()
        val topMoves = ScrabbleRules.generateMoves(board, rack, wordData, maxMoves = 20)

        _state.value = _state.value.copy(
            board = board,
            rack = rack,
            topMoves = topMoves,
            tentativeTiles = emptyList(),
            roundScore = 0
        )
    }

    private fun generateBoardAndRack(): Pair<ScrabbleBoard, List<ScrabbleTile>> {
        // Try a bounded number of random layouts and keep the first that admits a
        // legal play. Falling back to the last attempt (instead of recursing
        // forever) keeps generation terminating even with a sparse dictionary.
        var fallback: Pair<ScrabbleBoard, List<ScrabbleTile>>? = null
        repeat(MAX_GENERATION_ATTEMPTS) {
            val candidate = generateBoardAndRackOnce()
            fallback = candidate
            if (ScrabbleRules.hasAnyMove(candidate.first, candidate.second, wordData)) return candidate
        }
        return fallback ?: (ScrabbleBoard() to ScrabbleTile.createBag().shuffled(random).take(7))
    }

    private fun generateBoardAndRackOnce(): Pair<ScrabbleBoard, List<ScrabbleTile>> {
        val tileCount = when (currentDensity) {
            BoardDensity.SPARSE -> 10
            BoardDensity.MEDIUM -> 20
            BoardDensity.DENSE -> 35
        }

        var board = ScrabbleBoard()
        val allTiles = ScrabbleTile.createBag().shuffled(random)
        val boardTiles = allTiles.take(tileCount)
        val rackTiles = allTiles.drop(tileCount).take(7)

        // Place tiles in connected clusters
        val positions = mutableSetOf<Position>()
        positions.add(Position(7, 7)) // Start at center

        boardTiles.forEachIndexed { index, tile ->
            if (index == 0) {
                board = board.withTile(Position(7, 7), tile)
            } else {
                // Find an empty position adjacent to existing tiles
                val candidates = positions.flatMap { it.adjacentPositions() }
                    .filter { it.isValid() && board.getTile(it) == null }
                    .distinct()

                if (candidates.isNotEmpty()) {
                    val pos = candidates.random(random)
                    board = board.withTile(pos, tile)
                    positions.add(pos)
                }
            }
        }

        // Adjust rack for spicy difficulty
        val finalRack = if (currentRackDiff == RackDifficulty.SPICY) {
            // Replace some tiles with high-value letters
            val spicyLetters = listOf('Q', 'Z', 'X', 'J', 'K')
            rackTiles.mapIndexed { i, tile ->
                if (i < 3 && random.nextBoolean()) {
                    ScrabbleTile.create(spicyLetters.random(random))
                } else {
                    tile
                }
            }
        } else {
            rackTiles
        }

        return board to finalRack
    }

    private fun placeTile(position: Position, tile: ScrabbleTile) {
        val state = _state.value
        if (!state.rack.contains(tile)) return

        val newTentative = state.tentativeTiles + PlacedTile(position, tile)
        val newRack = state.rack.toMutableList().apply { remove(tile) }

        _state.value = state.copy(
            tentativeTiles = newTentative,
            rack = newRack
        )
    }

    private fun recallTiles() {
        val state = _state.value
        val returnedTiles = state.tentativeTiles.map { it.tile }
        _state.value = state.copy(
            tentativeTiles = emptyList(),
            rack = state.rack + returnedTiles
        )
    }

    private fun shuffleRack() {
        val state = _state.value
        _state.value = state.copy(rack = state.rack.shuffled(random))
    }

    private fun submitPlay() {
        val state = _state.value
        if (state.tentativeTiles.isEmpty()) {
            skipRound()
            return
        }

        val direction = inferDirection(state.tentativeTiles)
        val move = ScrabbleMove(state.tentativeTiles, direction)

        val validation = ScrabbleRules.validateMove(state.board, move, wordData)
        if (validation !is ScrabbleRules.MoveValidation.Valid) {
            // Invalid - treat as skip
            skipRound()
            return
        }

        val score = ScrabbleRules.scoreMove(state.board, move)
        _state.value = state.copy(
            roundScore = score,
            totalScore = state.totalScore + score,
            tentativeTiles = emptyList()
        )
    }

    private fun skipRound() {
        val state = _state.value
        _state.value = state.copy(
            roundScore = 0,
            tentativeTiles = emptyList()
        )
    }

    private fun nextRound() {
        val state = _state.value
        _state.value = state.copy(roundNumber = state.roundNumber + 1)
        setupRound()
    }

    private fun inferDirection(tiles: List<PlacedTile>): Direction {
        if (tiles.size <= 1) return Direction.HORIZONTAL
        val rows = tiles.map { it.position.row }.distinct()
        return if (rows.size == 1) Direction.HORIZONTAL else Direction.VERTICAL
    }

    private companion object {
        const val MAX_GENERATION_ATTEMPTS = 40
    }
}
