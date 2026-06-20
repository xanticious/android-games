package com.xanticious.androidgames.model.games.bubblespop

/** Which of the three Bubbles Pop game modes this configuration represents. */
enum class BubblesVariant { TURN_BASED, ARCADE, SNAKE_ARCADE }

/** The seven possible bubble colors (indices map to Color-palette tokens in the view). */
enum class BubbleColor { CYAN, RED, YELLOW, ORANGE, GRAY, TEAL, LAVENDER }

/** Special types layered on top of a bubble's base color. */
enum class BubbleType {
    NORMAL,
    BOMB,     // on pop: also destroys all 6 adjacent bubbles
    RAINBOW,  // matches any color when fired or in the cluster
    STONE,    // cannot be matched; must be disconnected from the cluster
    POWER_UP  // contains a collectible power-up (see BubblePowerUp)
}

/** Power-up effects embedded in POWER_UP-type bubbles. */
enum class BubblePowerUp {
    LIGHTNING,    // destroys an entire column (grid) or all bubbles of one color (snake)
    COLOR_BOMB,   // destroys all bubbles of the targeted color in the grid
    SLOW_TIME,    // halves descent / chain speed for 10 s
    WILD_SHOT,    // next fired bubble acts as RAINBOW
    SHIELD,       // absorbs one danger-line crossing
    REVERSE,      // (snake) briefly reverses chain direction
    BOMB_BLAST,   // (snake) destroys a 5-bubble radius around impact
    COLOR_STORM,  // (snake) all chain bubbles become one color for 3 s
}

/** One active power-up with remaining duration in seconds. */
data class ActivePowerUp(
    val type: BubblePowerUp,
    val remainingSeconds: Float,
)

// ─── Grid-based types (TURN_BASED + ARCADE) ─────────────────────────────────

/** A bubble fixed in the hex grid. */
data class GridCell(
    val col: Int,
    val row: Int,
    val color: BubbleColor,
    val type: BubbleType = BubbleType.NORMAL,
    val powerUp: BubblePowerUp? = null,
)

/** A bubble currently flying from the cannon toward the grid. */
data class FlyingBubble(
    val x: Float,
    val y: Float,
    val dx: Float,  // normalized-coord units per second
    val dy: Float,
    val color: BubbleColor,
    val type: BubbleType = BubbleType.NORMAL,
)

/**
 * Full game state for the TURN_BASED and ARCADE variants.
 * All positions are in normalized [0, 1] coordinates.
 */
data class BubblesGridState(
    /** Occupied hex cells, keyed by (col, row). */
    val grid: Map<Pair<Int, Int>, GridCell>,
    val cannonBubble: BubbleColor,
    val cannonBubbleType: BubbleType,
    val nextBubble: BubbleColor,
    val nextBubbleType: BubbleType,
    /** Null when nothing is in flight. */
    val flying: FlyingBubble?,
    val score: Int,
    val level: Int,
    val lives: Int,
    /** Turn-based: consecutive shots without a match. */
    val missStreak: Int,
    /** Seconds until the cannon is ready again (0 = ready). */
    val cannonCooldown: Float,
    /** How far the cluster has descended from its starting position (normalized y). */
    val topOffset: Float,
    val activePowerUps: List<ActivePowerUp>,
    val comboMultiplier: Float,
    /** Seconds remaining in the current combo window. */
    val comboTimer: Float,
    val wildShotActive: Boolean,
    val shieldActive: Boolean,
    val bestScore: Int,
)

/** Events produced by [com.xanticious.androidgames.controller.games.bubblespop.BubblesPopController.stepGrid]. */
sealed interface BubblesGridEvent {
    data object None : BubblesGridEvent
    data class BubblePopped(val count: Int, val scoreGained: Int) : BubblesGridEvent
    data class BubbleFallen(val count: Int, val scoreGained: Int) : BubblesGridEvent
    data object ClusterEmpty : BubblesGridEvent
    /** Cluster crossed the danger line and game is over (no lives remain). */
    data object ClusterCrossedDangerLine : BubblesGridEvent
    /** A life was lost but the game continues (arcade only). */
    data object LifeLost : BubblesGridEvent
}

// ─── Snake-arcade types ──────────────────────────────────────────────────────

/** A single bubble in the snake chain, located at path-distance [t] from the chain's tail. */
data class ChainBubble(
    val t: Float,
    val color: BubbleColor,
    val type: BubbleType = BubbleType.NORMAL,
    val powerUp: BubblePowerUp? = null,
)

/** A bubble flying from the snake launcher toward the chain. */
data class SnakeFlyingBubble(
    val x: Float,
    val y: Float,
    val dx: Float,
    val dy: Float,
    val color: BubbleColor,
    val type: BubbleType = BubbleType.NORMAL,
)

/**
 * Full game state for the SNAKE_ARCADE variant.
 * The chain is sorted by [ChainBubble.t] ascending (0 = tail/far from exit, max = head/near exit).
 */
data class BubblesSnakeState(
    val chain: List<ChainBubble>,
    val launcherAngle: Float,
    val cannonBubble: BubbleColor,
    val cannonBubbleType: BubbleType,
    val nextBubble: BubbleColor,
    val nextBubbleType: BubbleType,
    val flying: SnakeFlyingBubble?,
    /** Remaining bubble-swaps this level. */
    val swapRemaining: Int,
    val score: Int,
    val level: Int,
    val lives: Int,
    val cannonCooldown: Float,
    val chainSpeed: Float,
    val activePowerUps: List<ActivePowerUp>,
    /** > 0 while the chain is moving in reverse. */
    val reverseTimer: Float,
    val colorStormTimer: Float,
    val colorStormColor: BubbleColor?,
    /** Extra bubbles added to chain when a shot has no match. */
    val backfirePenalty: Int,
    val bestScore: Int,
)

/** Events produced by [com.xanticious.androidgames.controller.games.bubblespop.BubblesPopController.stepSnake]. */
sealed interface BubblesSnakeEvent {
    data object None : BubblesSnakeEvent
    data class BubblePopped(val count: Int, val scoreGained: Int) : BubblesSnakeEvent
    data object ChainCleared : BubblesSnakeEvent
    /** One or more bubbles exited through the vortex. */
    data object BubbleExited : BubblesSnakeEvent
    /** Shot had no match; penalty bubbles were added. */
    data object Backfire : BubblesSnakeEvent
}

// ─── Shared configuration ────────────────────────────────────────────────────

/** Difficulty-specific tuning shared by all three Bubbles Pop variants. */
data class BubblesPopConfig(
    val variant: BubblesVariant,
    val colorsInPlay: Int,
    val startingRows: Int,
    val missesPerDescend: Int,
    val descentSpeed: Float,
    val cannonCooldown: Float,
    val initialLives: Int,
    val chainLength: Int,
    val initialChainSpeed: Float,
    val chainSpeedIncrement: Float,
    val backfirePenalty: Int,
    val powerUpFrequency: Float,
    /** Minimum game level at which BOMB / STONE specials appear. */
    val specialBubbleLevel: Int,
)
