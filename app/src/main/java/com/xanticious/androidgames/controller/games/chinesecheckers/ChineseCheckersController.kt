package com.xanticious.androidgames.controller.games.chinesecheckers

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersConfig
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersCoordinate
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersHole
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersMove
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersResult
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersSide
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersState
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

class ChineseCheckersController {
    private val directions = listOf(
        ChineseCheckersCoordinate(1, -1, 0),
        ChineseCheckersCoordinate(1, 0, -1),
        ChineseCheckersCoordinate(0, 1, -1),
        ChineseCheckersCoordinate(-1, 1, 0),
        ChineseCheckersCoordinate(-1, 0, 1),
        ChineseCheckersCoordinate(0, -1, 1)
    )

    fun configFor(difficulty: GameDifficulty): ChineseCheckersConfig = ChineseCheckersConfig(difficulty = difficulty)

    fun initialState(config: ChineseCheckersConfig): ChineseCheckersState {
        val holes = buildHoles()
        val northHome = holes.map { it.coordinate }.filter { it.z < -4 }.toSet()
        val southHome = holes.map { it.coordinate }.filter { it.z > 4 }.toSet()
        val homeRegions = mapOf(
            ChineseCheckersSide.PLAYER to southHome,
            ChineseCheckersSide.AI to northHome
        )
        val targetRegions = mapOf(
            ChineseCheckersSide.PLAYER to northHome,
            ChineseCheckersSide.AI to southHome
        )
        val occupancy = buildMap {
            southHome.forEach { put(it, ChineseCheckersSide.PLAYER) }
            northHome.forEach { put(it, ChineseCheckersSide.AI) }
        }
        return ChineseCheckersState(
            holes = holes,
            connections = buildConnections(holes),
            occupancy = occupancy,
            homeRegions = homeRegions,
            targetRegions = targetRegions,
            currentPlayer = ChineseCheckersSide.PLAYER,
            selectedPeg = null,
            config = config,
            result = null
        )
    }

    fun emptyState(config: ChineseCheckersConfig = configFor(GameDifficulty.MEDIUM)): ChineseCheckersState {
        val initial = initialState(config)
        return initial.copy(occupancy = emptyMap(), message = "Select a marble.")
    }

    fun legalMoves(state: ChineseCheckersState, side: ChineseCheckersSide = state.currentPlayer): List<ChineseCheckersMove> {
        if (state.result != null) return emptyList()
        return state.occupancy.asSequence()
            .filter { it.value == side }
            .flatMap { legalMovesForPeg(state, it.key).asSequence() }
            .toList()
    }

    fun legalMovesForPeg(state: ChineseCheckersState, from: ChineseCheckersCoordinate): List<ChineseCheckersMove> {
        if (state.result != null) return emptyList()
        if (state.occupancy[from] == null) return emptyList()
        val holes = state.holeCoordinates
        val occupied = state.occupancy.keys
        val steps = directions.map { from + it }
            .filter { it in holes && it !in occupied }
            .map { ChineseCheckersMove(from = from, to = it, path = listOf(from, it)) }
        val hops = mutableListOf<ChineseCheckersMove>()
        collectHops(
            from = from,
            current = from,
            path = listOf(from),
            boardHoles = holes,
            baseOccupied = occupied,
            visitedLandings = setOf(from),
            moves = hops
        )
        return steps + hops.distinctBy { it.path }
    }

    fun selectPeg(state: ChineseCheckersState, coordinate: ChineseCheckersCoordinate): ChineseCheckersState {
        val side = state.occupancy[coordinate]
        return if (side == state.currentPlayer && legalMovesForPeg(state, coordinate).isNotEmpty()) {
            state.copy(selectedPeg = coordinate, message = "Choose a highlighted destination.")
        } else {
            state.copy(message = "Select one of your marbles with a legal move.")
        }
    }

    fun applySelectedMove(state: ChineseCheckersState, destination: ChineseCheckersCoordinate): ChineseCheckersState {
        val selected = state.selectedPeg ?: return selectPeg(state, destination)
        val move = legalMovesForPeg(state, selected).firstOrNull { it.to == destination }
        return if (move == null) {
            val occupant = state.occupancy[destination]
            if (occupant == state.currentPlayer) selectPeg(state, destination) else state.copy(message = "Choose a highlighted destination.")
        } else {
            applyMove(state, move)
        }
    }

    fun applyMove(state: ChineseCheckersState, move: ChineseCheckersMove): ChineseCheckersState {
        if (state.result != null) return state
        val side = state.occupancy[move.from] ?: return state.copy(message = "Illegal move.")
        if (side != state.currentPlayer) return state.copy(message = "Illegal move.")
        val legal = legalMovesForPeg(state, move.from).firstOrNull { it.to == move.to && (it.path == move.path || move.path.size == 2) }
            ?: return state.copy(message = "Illegal move.")
        val occupancy = state.occupancy - legal.from + (legal.to to side)
        val moved = state.copy(
            occupancy = occupancy,
            selectedPeg = null,
            turnsPlayed = state.turnsPlayed + 1,
            message = if (legal.isHop) "${side.label()} hopped ${legal.hopCount}." else "${side.label()} stepped."
        )
        val winner = winningSide(moved)
        if (winner != null) {
            return moved.copy(
                result = ChineseCheckersResult(winner = winner, loser = opponent(winner), turnsPlayed = moved.turnsPlayed),
                message = "${winner.label()} wins!"
            )
        }
        val next = opponent(side)
        return moved.copy(currentPlayer = next, message = if (next == ChineseCheckersSide.PLAYER) "Select a marble." else "AI is thinking.")
    }

    fun winningSide(state: ChineseCheckersState): ChineseCheckersSide? = ChineseCheckersSide.entries.firstOrNull { side ->
        val target = state.targetRegions[side].orEmpty()
        target.isNotEmpty() && target.all { state.occupancy[it] == side }
    }

    fun chooseAiMove(state: ChineseCheckersState, random: Random = Random.Default): ChineseCheckersMove? {
        val moves = legalMoves(state, state.currentPlayer)
        if (moves.isEmpty()) return null
        if (state.config.difficulty == GameDifficulty.EASY) return moves[random.nextInt(moves.size)]
        val scored = moves.map { it to moveScore(state, it) }
        val sorted = scored.sortedByDescending { it.second }
        val candidates = when (state.config.difficulty) {
            GameDifficulty.EASY -> sorted
            GameDifficulty.MEDIUM -> sorted.take(max(1, sorted.size / 2))
            GameDifficulty.HARD -> sorted.filter { it.second == sorted.first().second }
        }
        return candidates[random.nextInt(candidates.size)].first
    }

    fun opponent(side: ChineseCheckersSide): ChineseCheckersSide = when (side) {
        ChineseCheckersSide.PLAYER -> ChineseCheckersSide.AI
        ChineseCheckersSide.AI -> ChineseCheckersSide.PLAYER
    }

    fun areAdjacent(a: ChineseCheckersCoordinate, b: ChineseCheckersCoordinate): Boolean = cubeDistance(a, b) == 1

    private fun buildHoles(): List<ChineseCheckersHole> {
        val coordinates = mutableListOf<ChineseCheckersCoordinate>()
        for (x in -8..8) {
            for (y in -8..8) {
                val z = -x - y
                val c = ChineseCheckersCoordinate(x, y, z)
                if (isStarCoordinate(c)) coordinates += c
            }
        }
        val rawPositions = coordinates.associateWith { c ->
            val px = c.x + c.z / 2f
            val py = c.z * (sqrt(3.0).toFloat() / 2f)
            px to py
        }
        val minX = rawPositions.values.minOf { it.first }
        val maxX = rawPositions.values.maxOf { it.first }
        val minY = rawPositions.values.minOf { it.second }
        val maxY = rawPositions.values.maxOf { it.second }
        return coordinates.sortedWith(compareBy<ChineseCheckersCoordinate> { it.z }.thenBy { it.x }).map { c ->
            val raw = rawPositions.getValue(c)
            val homeFor = when {
                c.z > 4 -> ChineseCheckersSide.PLAYER
                c.z < -4 -> ChineseCheckersSide.AI
                else -> null
            }
            val targetFor = when (homeFor) {
                ChineseCheckersSide.PLAYER -> ChineseCheckersSide.AI
                ChineseCheckersSide.AI -> ChineseCheckersSide.PLAYER
                null -> null
            }
            ChineseCheckersHole(
                coordinate = c,
                displayX = ((raw.first - minX) / (maxX - minX)).coerceIn(0f, 1f),
                displayY = ((raw.second - minY) / (maxY - minY)).coerceIn(0f, 1f),
                homeFor = homeFor,
                targetFor = targetFor
            )
        }
    }

    private fun buildConnections(holes: List<ChineseCheckersHole>): List<Pair<ChineseCheckersCoordinate, ChineseCheckersCoordinate>> {
        val coordinates = holes.map { it.coordinate }
        return coordinates.flatMapIndexed { index, coordinate ->
            coordinates.drop(index + 1)
                .filter { areAdjacent(coordinate, it) }
                .map { coordinate to it }
        }
    }

    private fun isStarCoordinate(c: ChineseCheckersCoordinate): Boolean {
        val central = max(max(abs(c.x), abs(c.y)), abs(c.z)) <= 4
        val north = c.z < -4 && c.x in 1..4 && c.y in 1..4
        val south = c.z > 4 && c.x in -4..-1 && c.y in -4..-1
        val east = c.x > 4 && c.y in -4..-1 && c.z in -4..-1
        val west = c.x < -4 && c.y in 1..4 && c.z in 1..4
        val northEast = c.y > 4 && c.x in -4..-1 && c.z in -4..-1
        val southWest = c.y < -4 && c.x in 1..4 && c.z in 1..4
        return central || north || south || east || west || northEast || southWest
    }

    private fun collectHops(
        from: ChineseCheckersCoordinate,
        current: ChineseCheckersCoordinate,
        path: List<ChineseCheckersCoordinate>,
        boardHoles: Set<ChineseCheckersCoordinate>,
        baseOccupied: Set<ChineseCheckersCoordinate>,
        visitedLandings: Set<ChineseCheckersCoordinate>,
        moves: MutableList<ChineseCheckersMove>
    ) {
        val occupied = baseOccupied - from + current
        directions.forEach { direction ->
            val over = current + direction
            val landing = over + direction
            if (over in occupied && landing in boardHoles && landing !in occupied && landing !in visitedLandings) {
                val nextPath = path + landing
                moves += ChineseCheckersMove(from = from, to = landing, path = nextPath)
                collectHops(from, landing, nextPath, boardHoles, baseOccupied, visitedLandings + landing, moves)
            }
        }
    }

    private fun moveScore(state: ChineseCheckersState, move: ChineseCheckersMove): Int {
        val side = state.occupancy[move.from] ?: return 0
        val before = distanceToTarget(state, side, move.from)
        val after = distanceToTarget(state, side, move.to)
        val targetBonus = if (move.to in state.targetRegions[side].orEmpty()) 12 else 0
        val directionBonus = when (side) {
            ChineseCheckersSide.PLAYER -> move.from.z - move.to.z
            ChineseCheckersSide.AI -> move.to.z - move.from.z
        }
        return (before - after) * 20 + directionBonus * 4 + move.hopCount * 8 + targetBonus
    }

    private fun distanceToTarget(state: ChineseCheckersState, side: ChineseCheckersSide, coordinate: ChineseCheckersCoordinate): Int =
        state.targetRegions[side].orEmpty().minOfOrNull { cubeDistance(coordinate, it) } ?: 0

    private fun cubeDistance(a: ChineseCheckersCoordinate, b: ChineseCheckersCoordinate): Int =
        (abs(a.x - b.x) + abs(a.y - b.y) + abs(a.z - b.z)) / 2

    private fun ChineseCheckersSide.label(): String = when (this) {
        ChineseCheckersSide.PLAYER -> "You"
        ChineseCheckersSide.AI -> "AI"
    }

    private operator fun ChineseCheckersCoordinate.plus(other: ChineseCheckersCoordinate): ChineseCheckersCoordinate =
        ChineseCheckersCoordinate(x + other.x, y + other.y, z + other.z)
}
