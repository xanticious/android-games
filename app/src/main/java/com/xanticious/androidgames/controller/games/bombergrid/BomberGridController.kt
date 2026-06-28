package com.xanticious.androidgames.controller.games.bombergrid

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.bombergrid.BomberCharacter
import com.xanticious.androidgames.model.games.bombergrid.BomberGridConfig
import com.xanticious.androidgames.model.games.bombergrid.BomberGridState
import com.xanticious.androidgames.model.games.bombergrid.BomberTeam
import com.xanticious.androidgames.model.games.bombergrid.CharacterStatus
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Pure Bomber Grid rules engine — no Android or Compose imports.
 *
 * Responsibilities:
 *  - Difficulty configuration
 *  - Procedural terrain generation
 *  - Character movement with abyss detection
 *  - Aim / power adjustments
 *  - Projectile physics (gravity + wind)
 *  - Explosion blast (terrain carving + character damage)
 *  - Fall resolution (characters settle to new terrain surface, abyss = elimination)
 *  - Turn rotation across surviving characters
 *  - Win detection
 *  - AI decision-making (scaled by difficulty noise)
 */
class BomberGridController {

    // ─── Configuration ────────────────────────────────────────────────────────

    fun configFor(difficulty: GameDifficulty): BomberGridConfig = when (difficulty) {
        GameDifficulty.EASY -> BomberGridConfig(
            gravity = 9.8f,
            wind = 1.5f,
            moveRange = 6,
            blastRadius = 3.5f,
            projectileSpeed = 22f,
            aiAimNoise = 0.55f
        )
        GameDifficulty.MEDIUM -> BomberGridConfig(
            gravity = 9.8f,
            wind = 1.0f,
            moveRange = 6,
            blastRadius = 3.5f,
            projectileSpeed = 22f,
            aiAimNoise = 0.30f
        )
        GameDifficulty.HARD -> BomberGridConfig(
            gravity = 9.8f,
            wind = 0.5f,
            moveRange = 6,
            blastRadius = 3.5f,
            projectileSpeed = 22f,
            aiAimNoise = 0.10f
        )
    }

    // ─── Terrain generation ───────────────────────────────────────────────────

    /**
     * Produces a randomised side-view terrain with ledges, plateaus, and a few
     * abyss gaps.  Spawn zones near both edges are always solid.
     */
    fun generateTerrain(config: BomberGridConfig, random: Random = Random.Default): List<Int> {
        val cols = config.terrainCols
        val terrain = IntArray(cols)

        // Random-walk base height.
        var h = config.terrainMaxHeight / 2
        for (col in 0 until cols) {
            val delta = random.nextInt(5) - 2      // −2..+2
            h = (h + delta).coerceIn(config.terrainMinHeight, config.terrainMaxHeight)
            terrain[col] = h
        }

        // Carve two abyss gaps in the middle third (away from spawn zones).
        val gapStart = cols / 4
        val gapEnd = cols * 3 / 4
        repeat(2) {
            val pos = gapStart + random.nextInt(gapEnd - gapStart - 2)
            val width = 2 + random.nextInt(3)
            for (c in pos until minOf(pos + width, gapEnd)) {
                terrain[c] = 0
            }
        }

        // Ensure spawn areas are solid so starting characters aren't on abyss.
        val safeLeft = 14
        val safeRight = cols - 15
        for (c in 0 until safeLeft) {
            if (terrain[c] <= 0) terrain[c] = config.terrainMinHeight
        }
        for (c in safeRight until cols) {
            if (terrain[c] <= 0) terrain[c] = config.terrainMinHeight
        }

        return terrain.toList()
    }

    // ─── Movement ─────────────────────────────────────────────────────────────

    /**
     * Moves the active character one step in [direction] (−1 = left, +1 = right).
     * Consumes one unit of [BomberGridState.moveBudget].  Walking into an abyss
     * column immediately eliminates the character.
     */
    fun moveCharacter(state: BomberGridState, config: BomberGridConfig, direction: Int): BomberGridState {
        val char = state.characters.getOrNull(state.activeCharIndex) ?: return state
        if (state.moveBudget <= 0) return state
        if (char.status == CharacterStatus.ELIMINATED) return state

        val step = direction.coerceIn(-1, 1)
        if (step == 0) return state

        val newCol = (char.col + step).coerceIn(0, state.terrain.size - 1)
        val newTerrainHeight = state.terrain.getOrElse(newCol) { 0 }
        val newStatus = if (newTerrainHeight <= 0) CharacterStatus.ELIMINATED else char.status
        val newRow = if (newTerrainHeight <= 0) 0 else newTerrainHeight

        val updated = char.copy(col = newCol, row = newRow, status = newStatus)
        val newChars = state.characters.map { if (it.id == char.id) updated else it }

        return state.copy(characters = newChars, moveBudget = state.moveBudget - 1)
    }

    // ─── Aiming ───────────────────────────────────────────────────────────────

    /** Adjusts aim angle by [angleDelta] degrees, clamped to [5, 175]. */
    fun adjustAim(state: BomberGridState, angleDelta: Float): BomberGridState =
        state.copy(aimAngleDeg = (state.aimAngleDeg + angleDelta).coerceIn(5f, 175f))

    /** Adjusts launch power by [powerDelta] (fraction), clamped to [0.05, 1.0]. */
    fun adjustPower(state: BomberGridState, powerDelta: Float): BomberGridState =
        state.copy(aimPower = (state.aimPower + powerDelta).coerceIn(0.05f, 1.0f))

    // ─── Projectile ───────────────────────────────────────────────────────────

    /**
     * Sets [BomberGridState.activeProjectile] and [BomberGridState.projectileVelocity]
     * based on the current aim angle and power.  The projectile launches from just
     * above the active character's head.
     *
     * Convention: angle 0° = horizontal right, 90° = straight up, 180° = horizontal left.
     * The y-axis grows upward in world space.
     */
    fun launchProjectile(state: BomberGridState, config: BomberGridConfig): BomberGridState {
        val char = state.characters.getOrNull(state.activeCharIndex) ?: return state
        val startPos = Vec2(char.col.toFloat(), char.row.toFloat() + 1.5f)
        val angleRad = (state.aimAngleDeg * PI / 180.0).toFloat()
        val speed = config.projectileSpeed * state.aimPower
        val vx = speed * cos(angleRad)
        val vy = speed * sin(angleRad)
        return state.copy(
            activeProjectile = startPos,
            projectileVelocity = Vec2(vx, vy)
        )
    }

    /**
     * Advances the in-flight projectile by [dt] seconds under gravity and wind.
     * Returns the updated state.  Call [hasImpacted] to check landing.
     */
    fun stepProjectile(state: BomberGridState, config: BomberGridConfig, dt: Float): BomberGridState {
        val pos = state.activeProjectile ?: return state
        val vel = state.projectileVelocity
        val newVel = Vec2(
            vel.x + state.wind * dt,
            vel.y - config.gravity * dt
        )
        val newPos = pos + newVel * dt
        return state.copy(activeProjectile = newPos, projectileVelocity = newVel)
    }

    /**
     * Returns true if [state]'s in-flight projectile has hit terrain, gone out
     * of horizontal bounds, or dropped below the abyss floor.
     */
    fun hasImpacted(state: BomberGridState): Boolean {
        val pos = state.activeProjectile ?: return false
        val col = pos.x.toInt()
        return when {
            pos.x < 0f || pos.x >= state.terrain.size.toFloat() -> true
            pos.y < 0f -> true
            col in state.terrain.indices && pos.y <= state.terrain[col].toFloat() -> true
            else -> false
        }
    }

    /**
     * Pre-computes the full projectile trajectory for aiming assist (canvas overlay).
     * Returns world-space positions up to terrain impact or terrain/bounds exit.
     */
    fun computeTrajectory(
        startPos: Vec2,
        angleDeg: Float,
        power: Float,
        config: BomberGridConfig,
        terrain: List<Int>,
        stepDt: Float = 0.04f,
        maxSteps: Int = 300
    ): List<Vec2> {
        val angleRad = (angleDeg * PI / 180.0).toFloat()
        val speed = config.projectileSpeed * power
        var vx = speed * cos(angleRad)
        var vy = speed * sin(angleRad)
        var x = startPos.x
        var y = startPos.y
        val path = mutableListOf(Vec2(x, y))

        repeat(maxSteps) {
            vx += config.wind * stepDt
            vy -= config.gravity * stepDt
            x += vx * stepDt
            y += vy * stepDt
            path.add(Vec2(x, y))
            val col = x.toInt()
            val outOfBounds = x < 0f || x >= terrain.size.toFloat()
            val hitGround = !outOfBounds && col in terrain.indices && y <= terrain[col].toFloat()
            val hitAbyss = y < 0f
            if (outOfBounds || hitGround || hitAbyss) return path
        }
        return path
    }

    // ─── Explosion ────────────────────────────────────────────────────────────

    /**
     * Resolves a bomb explosion centred at [center]:
     *  1. Carves a hemispherical crater into the terrain.
     *  2. Damages characters within [BomberGridConfig.blastRadius]:
     *     HEALTHY → STUNNED, STUNNED → ELIMINATED.
     */
    fun resolveExplosion(
        state: BomberGridState,
        config: BomberGridConfig,
        center: Vec2
    ): BomberGridState {
        val radius = config.blastRadius

        val newTerrain = state.terrain.mapIndexed { col, height ->
            val dx = col.toFloat() - center.x
            if (abs(dx) > radius) {
                height
            } else {
                val carveDepth = sqrt((radius * radius - dx * dx).toDouble()).toFloat()
                val craterBottom = center.y - carveDepth
                minOf(height, maxOf(0, craterBottom.toInt()))
            }
        }

        val newChars = state.characters.map { char ->
            if (char.status == CharacterStatus.ELIMINATED) return@map char
            val dist = Vec2(char.col.toFloat(), char.row.toFloat()).distanceTo(center)
            if (dist <= radius) {
                when (char.status) {
                    CharacterStatus.HEALTHY -> char.copy(status = CharacterStatus.STUNNED)
                    CharacterStatus.STUNNED -> char.copy(status = CharacterStatus.ELIMINATED)
                    CharacterStatus.ELIMINATED -> char
                }
            } else char
        }

        return state.copy(
            terrain = newTerrain,
            characters = newChars,
            activeProjectile = null,
            projectileVelocity = Vec2.ZERO,
            explosionCenter = center,
            explosionRadius = radius
        )
    }

    // ─── Fall resolution ──────────────────────────────────────────────────────

    /**
     * Settles each living character to the current terrain surface at their column.
     * A character whose column is abyss (height ≤ 0) is immediately eliminated.
     */
    fun resolveFalls(state: BomberGridState): BomberGridState {
        val newChars = state.characters.map { char ->
            if (char.status == CharacterStatus.ELIMINATED) return@map char
            val terrainHeight = state.terrain.getOrElse(char.col) { 0 }
            if (terrainHeight <= 0) {
                char.copy(status = CharacterStatus.ELIMINATED, row = 0)
            } else {
                char.copy(row = terrainHeight)
            }
        }
        return state.copy(characters = newChars)
    }

    // ─── Win detection ────────────────────────────────────────────────────────

    /**
     * Returns the winning team if one side has had all its characters eliminated,
     * or null when the match is still ongoing.
     */
    fun winner(state: BomberGridState): BomberTeam? {
        val playerAlive = state.characters.any { it.team == BomberTeam.PLAYER && it.status != CharacterStatus.ELIMINATED }
        val aiAlive = state.characters.any { it.team == BomberTeam.AI && it.status != CharacterStatus.ELIMINATED }
        return when {
            !aiAlive && playerAlive -> BomberTeam.PLAYER
            !playerAlive && aiAlive -> BomberTeam.AI
            !aiAlive && !playerAlive -> BomberTeam.AI  // edge: both wiped — AI "wins" by default
            else -> null
        }
    }

    // ─── Turn rotation ────────────────────────────────────────────────────────

    /**
     * Advances to the next surviving character on the opposite team, cycling
     * through that team's roster in id order.  Resets the move budget and aim.
     * Increments [BomberGridState.round] each time play returns to the player.
     */
    fun advanceTurn(state: BomberGridState, config: BomberGridConfig): BomberGridState {
        val nextTeam = if (state.activeTeam == BomberTeam.PLAYER) BomberTeam.AI else BomberTeam.PLAYER
        val survivors = state.characters.filter { it.team == nextTeam && it.status != CharacterStatus.ELIMINATED }
        if (survivors.isEmpty()) return state

        val lastId = if (nextTeam == BomberTeam.PLAYER) state.playerCharCursor else state.aiCharCursor
        val lastPos = survivors.indexOfFirst { it.id == lastId }
        // When lastId is not found (−1 or eliminated), (−1 + 1) % n = 0 → first survivor.
        val nextPos = (lastPos + 1) % survivors.size
        val nextChar = survivors[nextPos]
        val nextIdx = state.characters.indexOf(nextChar)

        val newPlayerCursor = if (nextTeam == BomberTeam.PLAYER) nextChar.id else state.playerCharCursor
        val newAiCursor = if (nextTeam == BomberTeam.AI) nextChar.id else state.aiCharCursor
        val newRound = if (nextTeam == BomberTeam.PLAYER) state.round + 1 else state.round

        return state.copy(
            activeTeam = nextTeam,
            activeCharIndex = nextIdx,
            playerCharCursor = newPlayerCursor,
            aiCharCursor = newAiCursor,
            round = newRound,
            moveBudget = config.moveRange,
            aimAngleDeg = 45f,
            aimPower = 0.5f,
            activeProjectile = null,
            projectileVelocity = Vec2.ZERO,
            explosionCenter = null,
            explosionRadius = 0f
        )
    }

    // ─── AI decisions ─────────────────────────────────────────────────────────

    /**
     * Moves the AI character toward the nearest living player character until
     * the move budget is exhausted (or no closer target exists).
     */
    fun computeAiMove(state: BomberGridState, config: BomberGridConfig, random: Random = Random.Default): BomberGridState {
        val targets = state.characters.filter { it.team == BomberTeam.PLAYER && it.status != CharacterStatus.ELIMINATED }
        if (targets.isEmpty()) return state

        var current = state
        while (current.moveBudget > 0) {
            val activeChar = current.characters.getOrNull(current.activeCharIndex) ?: break
            if (activeChar.status == CharacterStatus.ELIMINATED) break
            val target = targets.minByOrNull { abs(it.col - activeChar.col) } ?: break
            val dir = when {
                target.col > activeChar.col -> 1
                target.col < activeChar.col -> -1
                else -> break
            }
            current = moveCharacter(current, config, dir)
        }
        return current
    }

    /**
     * Computes an angle (degrees) and power for the AI to fire toward the nearest
     * living player character.  Noise scaled by [BomberGridConfig.aiAimNoise] is
     * added to simulate imperfect aim.
     */
    fun computeAiShot(
        state: BomberGridState,
        config: BomberGridConfig,
        random: Random = Random.Default
    ): Pair<Float, Float> {
        val shooter = state.characters.getOrNull(state.activeCharIndex)
            ?: return Pair(45f, 0.5f)
        val targets = state.characters.filter { it.team == BomberTeam.PLAYER && it.status != CharacterStatus.ELIMINATED }
        val target = targets.minByOrNull { abs(it.col - shooter.col) }
            ?: return Pair(45f, 0.5f)

        val dx = (target.col - shooter.col).toFloat()
        val dist = abs(dx)
        // Base angle: ~45° toward the target, nudged by height difference.
        val dy = (target.row - shooter.row).toFloat()
        val baseAngleDeg = if (dx >= 0) {
            (45f - dy * 0.4f).coerceIn(15f, 75f)
        } else {
            (135f + dy * 0.4f).coerceIn(105f, 165f)
        }
        val basePower = (dist / config.terrainCols.toFloat() * 1.8f + 0.25f).coerceIn(0.25f, 1.0f)

        val noiseDeg = config.aiAimNoise * (180f / PI.toFloat())
        val angle = (baseAngleDeg + (random.nextFloat() - 0.5f) * noiseDeg).coerceIn(5f, 175f)
        val power = (basePower + (random.nextFloat() - 0.5f) * config.aiAimNoise).coerceIn(0.1f, 1.0f)
        return Pair(angle, power)
    }
}
