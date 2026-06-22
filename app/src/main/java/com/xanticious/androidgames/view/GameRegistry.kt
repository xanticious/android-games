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
import com.xanticious.androidgames.view.games.cribbage.CribbageGame
import com.xanticious.androidgames.view.games.deckbuilderfleet.DeckBuilderFleetGame
import com.xanticious.androidgames.view.games.deckbuildersuperhero.DeckBuilderSuperheroGame
import com.xanticious.androidgames.view.games.driftracer.DriftRacerGame
import com.xanticious.androidgames.view.games.endlessrunner.EndlessRunnerGame
import com.xanticious.androidgames.view.games.helicopterdogfight.HelicopterDogfightGame
import com.xanticious.androidgames.view.games.hearts.HeartsGame
import com.xanticious.androidgames.view.games.holeswallowing.HoleSwallowingGame
import com.xanticious.androidgames.view.games.loveletter.LoveLetterGame
import com.xanticious.androidgames.view.games.missilecommand.MissileCommandGame
import com.xanticious.androidgames.view.games.pong.PongGame
import com.xanticious.androidgames.view.games.poker.PokerGame
import com.xanticious.androidgames.view.games.qix.QixGame
import com.xanticious.androidgames.view.games.snakes2d.SnakesGame
import com.xanticious.androidgames.view.games.solitaireclock.SolitaireClockGame
import com.xanticious.androidgames.view.games.solitairefreecell.FreeCellGame
import com.xanticious.androidgames.view.games.solitaireklondike.SolitaireKlondikeGame
import com.xanticious.androidgames.view.games.solitairepyramid.PyramidGame
import com.xanticious.androidgames.view.games.solitairetripeaks.TriPeaksGame
import com.xanticious.androidgames.view.games.solitairetripeaks.TriPeaksTimedGame
import com.xanticious.androidgames.view.games.spacedefender.SpaceDefenderGame
import com.xanticious.androidgames.view.games.spades.SpadesGame

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
    "cribbage" to { difficulty, onExit -> CribbageGame(difficulty, onExit) },
    "deck-builder-fleet" to { difficulty, onExit -> DeckBuilderFleetGame(difficulty, onExit) },
    "deck-builder-superhero" to { difficulty, onExit -> DeckBuilderSuperheroGame(difficulty, onExit) },
    "drift-racer" to { difficulty, onExit -> DriftRacerGame(difficulty, onExit) },
    "endless-runner" to { difficulty, onExit -> EndlessRunnerGame(difficulty, onExit) },
    "helicopter-dogfight" to { difficulty, onExit -> HelicopterDogfightGame(difficulty, onExit) },
    "hearts" to { difficulty, onExit -> HeartsGame(difficulty, onExit) },
    "hole-swallowing-game" to { difficulty, onExit -> HoleSwallowingGame(difficulty, onExit) },
    "love-letter" to { difficulty, onExit -> LoveLetterGame(difficulty, onExit) },
    "missile-command" to { difficulty, onExit -> MissileCommandGame(difficulty, onExit) },
    "pong" to { difficulty, onExit -> PongGame(difficulty, onExit) },
    "poker" to { difficulty, onExit -> PokerGame(difficulty, onExit) },
    "qix" to { difficulty, onExit -> QixGame(difficulty, onExit) },
    "snakes-2d" to { difficulty, onExit -> SnakesGame(difficulty, onExit) },
    "solitaire-clock-timed" to { difficulty, onExit -> SolitaireClockGame(difficulty, onExit) },
    "solitaire-freecell" to { difficulty, onExit -> FreeCellGame(difficulty, onExit) },
    "solitaire-klondike" to { difficulty, onExit -> SolitaireKlondikeGame(difficulty, onExit) },
    "solitaire-pyramid" to { difficulty, onExit -> PyramidGame(difficulty, onExit) },
    "solitaire-tripeaks" to { difficulty, onExit -> TriPeaksGame(difficulty, onExit) },
    "solitaire-tripeaks-timed" to { difficulty, onExit -> TriPeaksTimedGame(difficulty, onExit) },
    "space-defender" to { difficulty, onExit -> SpaceDefenderGame(difficulty, onExit) },
    "spades" to { difficulty, onExit -> SpadesGame(difficulty, onExit) }
)
