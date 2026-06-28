package com.xanticious.androidgames.model.games.bombergrid

import com.xanticious.androidgames.model.Vec2

/** Which side a character belongs to. */
enum class BomberTeam { PLAYER, AI }

/** Health progression for each character. */
enum class CharacterStatus { HEALTHY, STUNNED, ELIMINATED }

/**
 * One combatant on the battlefield.
 * [col] is the column index in the terrain array; [row] is the terrain height
 * at that column (characters always stand on the surface).
 */
data class BomberCharacter(
    val id: Int,
    val team: BomberTeam,
    val col: Int,
    val row: Int,
    val status: CharacterStatus = CharacterStatus.HEALTHY,
    val name: String
)

/**
 * Tuning knobs derived from [com.xanticious.androidgames.model.GameDifficulty].
 * All fields are pure data — no Android imports.
 */
data class BomberGridConfig(
    /** Number of terrain columns. */
    val terrainCols: Int = 60,
    /** Maximum column height. */
    val terrainMaxHeight: Int = 18,
    /** Minimum column height (never abyss at generation time). */
    val terrainMinHeight: Int = 4,
    /** Gravitational acceleration in world-units/s². */
    val gravity: Float = 9.8f,
    /** Constant horizontal wind in world-units/s² (positive = rightward). */
    val wind: Float = 0f,
    /** Maximum columns an active character may move per turn. */
    val moveRange: Int = 6,
    /** Blast radius in world units. */
    val blastRadius: Float = 3.5f,
    /** Maximum projectile speed at full power (world-units/s). */
    val projectileSpeed: Float = 22f,
    /**
     * Angular noise added to the AI's ideal aim angle (radians).
     * Higher = sloppier AI.
     */
    val aiAimNoise: Float = 0.30f
)

/**
 * Complete, immutable snapshot of a Bomber Grid match.
 *
 * Terrain is a [List] of integer column heights; index 0 = leftmost column.
 * A height of 0 means the column is open abyss — stepping there eliminates a
 * character immediately.  Characters stand at `row == terrain[col]`.
 *
 * World coordinates: x grows rightward (column index), y grows upward (height).
 *
 * [activeCharIndex] is a direct index into [characters].
 * [playerCharCursor] / [aiCharCursor] hold the `id` of the last character that
 * acted on each team so turn rotation can continue correctly even after
 * eliminations.  Initial value −1 means no character on that team has acted yet.
 */
data class BomberGridState(
    val terrain: List<Int>,
    val characters: List<BomberCharacter>,
    val activeTeam: BomberTeam,
    val activeCharIndex: Int,
    val playerCharCursor: Int,
    val aiCharCursor: Int,
    val round: Int,
    val wind: Float,
    val moveBudget: Int,
    /** Aim angle in degrees.  0 = horizontal right, 90 = straight up, 180 = horizontal left. */
    val aimAngleDeg: Float,
    /** Launch power fraction in [0, 1]. */
    val aimPower: Float,
    /** World position of the in-flight projectile, or null when idle. */
    val activeProjectile: Vec2?,
    val projectileVelocity: Vec2,
    /** Centre of the most recent explosion, or null. */
    val explosionCenter: Vec2?,
    val explosionRadius: Float,
    /** Non-null once a team has been fully eliminated. */
    val winner: BomberTeam?
) {
    /** Convenience reference to the character whose turn it currently is. */
    val activeCharacter: BomberCharacter?
        get() = characters.getOrNull(activeCharIndex)

    companion object {
        /**
         * Builds the opening state from a freshly generated [terrain].
         * Player characters are placed near the left edge; AI near the right.
         */
        fun initial(config: BomberGridConfig, terrain: List<Int>): BomberGridState {
            val playerChars = listOf("Alpha", "Bravo", "Charlie").mapIndexed { i, name ->
                val col = (2 + i * 4).coerceIn(0, terrain.size - 1)
                BomberCharacter(id = i, team = BomberTeam.PLAYER, col = col, row = terrain[col], name = name)
            }
            val aiChars = listOf("Skull", "Viper", "Tank").mapIndexed { i, name ->
                val col = (terrain.size - 3 - i * 4).coerceIn(0, terrain.size - 1)
                BomberCharacter(id = i + 3, team = BomberTeam.AI, col = col, row = terrain[col], name = name)
            }
            return BomberGridState(
                terrain = terrain,
                characters = playerChars + aiChars,
                activeTeam = BomberTeam.PLAYER,
                activeCharIndex = 0,
                playerCharCursor = 0,
                aiCharCursor = -1,
                round = 1,
                wind = config.wind,
                moveBudget = config.moveRange,
                aimAngleDeg = 45f,
                aimPower = 0.5f,
                activeProjectile = null,
                projectileVelocity = Vec2.ZERO,
                explosionCenter = null,
                explosionRadius = 0f,
                winner = null
            )
        }
    }
}
