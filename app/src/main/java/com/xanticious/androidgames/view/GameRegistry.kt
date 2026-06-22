package com.xanticious.androidgames.view

import androidx.compose.runtime.Composable
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.view.games.asteroids.AsteroidsGame
import com.xanticious.androidgames.view.games.bombergrid.BomberGridGame
import com.xanticious.androidgames.view.games.brickbreaker.BrickBreakerGame
import com.xanticious.androidgames.view.games.brickbreakerarcade.BrickBreakerArcadeGame
import com.xanticious.androidgames.view.games.brickbreakercannon.BrickBreakerCannonGame
import com.xanticious.androidgames.view.games.brickbreakercannonarcade.BrickBreakerCannonArcadeGame
import com.xanticious.androidgames.view.games.bubblesarcade.BubblesPopArcadeGame
import com.xanticious.androidgames.view.games.bubblespop.BubblesPopGame
import com.xanticious.androidgames.view.games.bubblessnakearcade.BubblesPopSnakeArcadeGame
import com.xanticious.androidgames.view.games.driftracer.DriftRacerGame
import com.xanticious.androidgames.view.games.endlessrunner.EndlessRunnerGame
import com.xanticious.androidgames.view.games.helicopterdogfight.HelicopterDogfightGame
import com.xanticious.androidgames.view.games.holeswallowing.HoleSwallowingGame
import com.xanticious.androidgames.view.games.idleanimalmerge.IdleAnimalMergeGame
import com.xanticious.androidgames.view.games.idlebounce.IdleBounceGame
import com.xanticious.androidgames.view.games.idlecombattraining.IdleCombatTrainingGame
import com.xanticious.androidgames.view.games.idlefarmers.IdleFarmersGame
import com.xanticious.androidgames.view.games.idlegeneticalgorithm.IdleGeneticAlgorithmGame
import com.xanticious.androidgames.view.games.idleplayerpiano.IdlePlayerPianoGame
import com.xanticious.androidgames.view.games.missilecommand.MissileCommandGame
import com.xanticious.androidgames.view.games.pong.PongGame
import com.xanticious.androidgames.view.games.qix.QixGame
import com.xanticious.androidgames.view.games.snakes2d.SnakesGame
import com.xanticious.androidgames.view.games.spacedefender.SpaceDefenderGame

/**
 * Central dispatch from a game id to its in-game composable.
 *
 * This is the single integration point for playable games: [MainActivity] looks
 * up the id of the launched game here and, if present, renders the real game
 * (passing the chosen [GameDifficulty] and an `onExit` callback). Ids absent
 * from this map fall back to the generic stub screen.
 *
 * Each entry's composable lives in its own `view/games/<id>/` package, so games
 * can be added independently without touching one another's files.
 */
val actionGameRegistry: Map<String, @Composable (GameDifficulty, () -> Unit) -> Unit> = mapOf(
    "asteroids" to { difficulty, onExit -> AsteroidsGame(difficulty, onExit) },
    "bomber-grid" to { difficulty, onExit -> BomberGridGame(difficulty, onExit) },
    "brick-breaker" to { difficulty, onExit -> BrickBreakerGame(difficulty, onExit) },
    "brick-breaker-arcade" to { difficulty, onExit -> BrickBreakerArcadeGame(difficulty, onExit) },
    "brick-breaker-cannon" to { difficulty, onExit -> BrickBreakerCannonGame(difficulty, onExit) },
    "brick-breaker-cannon-arcade" to { difficulty, onExit -> BrickBreakerCannonArcadeGame(difficulty, onExit) },
    "bubbles-pop" to { difficulty, onExit -> BubblesPopGame(difficulty, onExit) },
    "bubbles-pop-arcade" to { difficulty, onExit -> BubblesPopArcadeGame(difficulty, onExit) },
    "bubbles-pop-snake-arcade" to { difficulty, onExit -> BubblesPopSnakeArcadeGame(difficulty, onExit) },
    "drift-racer" to { difficulty, onExit -> DriftRacerGame(difficulty, onExit) },
    "endless-runner" to { difficulty, onExit -> EndlessRunnerGame(difficulty, onExit) },
    "helicopter-dogfight" to { difficulty, onExit -> HelicopterDogfightGame(difficulty, onExit) },
    "hole-swallowing-game" to { difficulty, onExit -> HoleSwallowingGame(difficulty, onExit) },
    "idle-animal-merge" to { difficulty, onExit -> IdleAnimalMergeGame(difficulty, onExit) },
    "idle-bounce" to { difficulty, onExit -> IdleBounceGame(difficulty, onExit) },
    "idle-combat-training" to { difficulty, onExit -> IdleCombatTrainingGame(difficulty, onExit) },
    "idle-farmers" to { difficulty, onExit -> IdleFarmersGame(difficulty, onExit) },
    "idle-genetic-algorithm" to { difficulty, onExit -> IdleGeneticAlgorithmGame(difficulty, onExit) },
    "idle-player-piano" to { difficulty, onExit -> IdlePlayerPianoGame(difficulty, onExit) },
    "missile-command" to { difficulty, onExit -> MissileCommandGame(difficulty, onExit) },
    "pong" to { difficulty, onExit -> PongGame(difficulty, onExit) },
    "qix" to { difficulty, onExit -> QixGame(difficulty, onExit) },
    "snakes-2d" to { difficulty, onExit -> SnakesGame(difficulty, onExit) },
    "space-defender" to { difficulty, onExit -> SpaceDefenderGame(difficulty, onExit) }
)
