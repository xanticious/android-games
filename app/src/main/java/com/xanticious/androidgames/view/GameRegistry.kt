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
import com.xanticious.androidgames.view.games.dominosa.DominosaGame
import com.xanticious.androidgames.view.games.flood.FloodGame
import com.xanticious.androidgames.view.games.helicopterdogfight.HelicopterDogfightGame
import com.xanticious.androidgames.view.games.hearts.HeartsGame
import com.xanticious.androidgames.view.games.holeswallowing.HoleSwallowingGame
import com.xanticious.androidgames.view.games.jigsaw.JigsawGame
import com.xanticious.androidgames.view.games.lightup.LightUpGame
import com.xanticious.androidgames.view.games.logicgrid.LogicGridGame
import com.xanticious.androidgames.view.games.missilecommand.MissileCommandGame
import com.xanticious.androidgames.view.games.minesweeper.MinesweeperGame
import com.xanticious.androidgames.view.games.nonogram.NonogramGame
import com.xanticious.androidgames.view.games.numberlink.NumberlinkGame
import com.xanticious.androidgames.view.games.pathfinder.PathfinderGame
import com.xanticious.androidgames.view.games.mastermind.MastermindGame
import com.xanticious.androidgames.view.games.matchthree.MatchThreeArcadeGame
import com.xanticious.androidgames.view.games.matchthree.MatchThreeGame
import com.xanticious.androidgames.view.games.pegsolitaire.PegSolitaireGame
import com.xanticious.androidgames.view.games.pentomino.PentominoGame
import com.xanticious.androidgames.view.games.pipes.PipesGame
import com.xanticious.androidgames.view.games.idleanimalmerge.IdleAnimalMergeGame
import com.xanticious.androidgames.view.games.idlebounce.IdleBounceGame
import com.xanticious.androidgames.view.games.idlecombattraining.IdleCombatTrainingGame
import com.xanticious.androidgames.view.games.idlefarmers.IdleFarmersGame
import com.xanticious.androidgames.view.games.idlegeneticalgorithm.IdleGeneticAlgorithmGame
import com.xanticious.androidgames.view.games.idleplayerpiano.IdlePlayerPianoGame
import com.xanticious.androidgames.view.games.melodymaster.MelodyMasterGame
import com.xanticious.androidgames.view.games.morsecode.MorseCodeGame
import com.xanticious.androidgames.view.games.morsedecoder.MorseDecoderGame
import com.xanticious.androidgames.view.games.loveletter.LoveLetterGame
import com.xanticious.androidgames.view.games.dotart.DotArtGame
import com.xanticious.androidgames.view.games.driftracer.DriftRacerGame
import com.xanticious.androidgames.view.games.endlessrunner.EndlessRunnerGame
import com.xanticious.androidgames.view.games.helicopterdogfight.HelicopterDogfightGame
import com.xanticious.androidgames.view.games.hiddenobject.HiddenObjectGame
import com.xanticious.androidgames.view.games.hiddenobjects.HiddenObjectsGame
import com.xanticious.androidgames.view.games.holeswallowing.HoleSwallowingGame
import com.xanticious.androidgames.view.games.memorylanes.MemoryLanesGame
import com.xanticious.androidgames.view.games.missilecommand.MissileCommandGame
import com.xanticious.androidgames.view.games.paircollector.PairCollectorGame
import com.xanticious.androidgames.view.games.piratetreasuremaze.PirateTreasureMazeGame
import com.xanticious.androidgames.view.games.planetexplorer.PlanetExplorerGame
import com.xanticious.androidgames.view.games.pong.PongGame
import com.xanticious.androidgames.view.games.poker.PokerGame
import com.xanticious.androidgames.view.games.qix.QixGame
import com.xanticious.androidgames.view.games.skyscrapers.SkyscrapersGame
import com.xanticious.androidgames.view.games.slidingpuzzle.SlidingPuzzleGame
import com.xanticious.androidgames.view.games.sokoban.SokobanGame
import com.xanticious.androidgames.view.games.sudokucolors.SudokuColorsGame
import com.xanticious.androidgames.view.games.simcityblocks.SimCityBlocksGame
import com.xanticious.androidgames.view.games.snakes2d.SnakesGame
import com.xanticious.androidgames.view.games.solitaireclock.SolitaireClockGame
import com.xanticious.androidgames.view.games.solitairefreecell.FreeCellGame
import com.xanticious.androidgames.view.games.solitaireklondike.SolitaireKlondikeGame
import com.xanticious.androidgames.view.games.solitairepyramid.PyramidGame
import com.xanticious.androidgames.view.games.solitairetripeaks.TriPeaksGame
import com.xanticious.androidgames.view.games.solitairetripeaks.TriPeaksTimedGame
import com.xanticious.androidgames.view.games.spacedefender.SpaceDefenderGame
import com.xanticious.androidgames.view.games.twentyfortyeight.TwentyFortyEightGame
import com.xanticious.androidgames.view.games.taprhythm.TapRhythmGame
import com.xanticious.androidgames.view.games.spades.SpadesGame
import com.xanticious.androidgames.view.games.ageofempirelite.AgeOfEmpiresLiteGame
import com.xanticious.androidgames.view.games.anomalydefense.AnomalyDefenseGame
import com.xanticious.androidgames.view.games.basedefense.BaseDefenseGame
import com.xanticious.androidgames.view.games.empireskirmish.EmpireSkirmishGame
import com.xanticious.androidgames.view.games.randomizeddice.RandomizedDiceTdGame
import com.xanticious.androidgames.view.games.roguecaverns.RogueCavernsGame
import com.xanticious.androidgames.view.games.treasuremapper.TreasureMapperGame

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
    "dot-art" to { difficulty, onExit -> DotArtGame(difficulty, onExit) },
    "endless-runner" to { difficulty, onExit -> EndlessRunnerGame(difficulty, onExit) },
    "dominosa" to { difficulty, onExit -> DominosaGame(difficulty, onExit) },
    "flood" to { difficulty, onExit -> FloodGame(difficulty, onExit) },
    "helicopter-dogfight" to { difficulty, onExit -> HelicopterDogfightGame(difficulty, onExit) },
    "hearts" to { difficulty, onExit -> HeartsGame(difficulty, onExit) },
    "hole-swallowing-game" to { difficulty, onExit -> HoleSwallowingGame(difficulty, onExit) },
    "love-letter" to { difficulty, onExit -> LoveLetterGame(difficulty, onExit) },
    "hidden-object" to { difficulty, onExit -> HiddenObjectGame(difficulty, onExit) },
    "hidden-objects" to { difficulty, onExit -> HiddenObjectsGame(difficulty, onExit) },
    "hole-swallowing-game" to { difficulty, onExit -> HoleSwallowingGame(difficulty, onExit) },
    "jigsaw" to { difficulty, onExit -> JigsawGame(difficulty, onExit) },
    "light-up" to { difficulty, onExit -> LightUpGame(difficulty, onExit) },
    "logic-grid" to { difficulty, onExit -> LogicGridGame(difficulty, onExit) },
    "missile-command" to { difficulty, onExit -> MissileCommandGame(difficulty, onExit) },
    "mastermind" to { difficulty, onExit -> MastermindGame(difficulty, onExit) },
    "match-three" to { difficulty, onExit -> MatchThreeGame(difficulty, onExit) },
    "match-three-arcade" to { difficulty, onExit -> MatchThreeArcadeGame(difficulty, onExit) },
    "minesweeper" to { difficulty, onExit -> MinesweeperGame(difficulty, onExit) },
    "nonogram" to { difficulty, onExit -> NonogramGame(difficulty, onExit) },
    "numberlink" to { difficulty, onExit -> NumberlinkGame(difficulty, onExit) },
    "pathfinder" to { difficulty, onExit -> PathfinderGame(difficulty, onExit) },
    "pong" to { difficulty, onExit -> PongGame(difficulty, onExit) },
    "peg-solitaire" to { difficulty, onExit -> PegSolitaireGame(difficulty, onExit) },
    "pentomino" to { difficulty, onExit -> PentominoGame(difficulty, onExit) },
    "pipes" to { difficulty, onExit -> PipesGame(difficulty, onExit) },
    "qix" to { difficulty, onExit -> QixGame(difficulty, onExit) },
    "skyscrapers" to { difficulty, onExit -> SkyscrapersGame(difficulty, onExit) },
    "sliding-puzzle" to { difficulty, onExit -> SlidingPuzzleGame(difficulty, onExit) },
    "sokoban" to { difficulty, onExit -> SokobanGame(difficulty, onExit) },
    "sudoku-colors" to { difficulty, onExit -> SudokuColorsGame(difficulty, onExit) },
    "snakes-2d" to { difficulty, onExit -> SnakesGame(difficulty, onExit) },
    "space-defender" to { difficulty, onExit -> SpaceDefenderGame(difficulty, onExit) },
    "twenty-forty-eight" to { difficulty, onExit -> TwentyFortyEightGame(difficulty, onExit) }
    "idle-animal-merge" to { difficulty, onExit -> IdleAnimalMergeGame(difficulty, onExit) },
    "idle-bounce" to { difficulty, onExit -> IdleBounceGame(difficulty, onExit) },
    "idle-combat-training" to { difficulty, onExit -> IdleCombatTrainingGame(difficulty, onExit) },
    "idle-farmers" to { difficulty, onExit -> IdleFarmersGame(difficulty, onExit) },
    "idle-genetic-algorithm" to { difficulty, onExit -> IdleGeneticAlgorithmGame(difficulty, onExit) },
    "idle-player-piano" to { difficulty, onExit -> IdlePlayerPianoGame(difficulty, onExit) },
    "melody-master" to { difficulty, onExit -> MelodyMasterGame(difficulty, onExit) },
    "missile-command" to { difficulty, onExit -> MissileCommandGame(difficulty, onExit) },
    "morse-code" to { difficulty, onExit -> MorseCodeGame(difficulty, onExit) },
    "morse-decoder" to { difficulty, onExit -> MorseDecoderGame(difficulty, onExit) },
    "memory-lanes" to { difficulty, onExit -> MemoryLanesGame(difficulty, onExit) },
    "missile-command" to { difficulty, onExit -> MissileCommandGame(difficulty, onExit) },
    "pair-collector" to { difficulty, onExit -> PairCollectorGame(difficulty, onExit) },
    "pirate-treasure-maze" to { difficulty, onExit -> PirateTreasureMazeGame(difficulty, onExit) },
    "planet-explorer" to { difficulty, onExit -> PlanetExplorerGame(difficulty, onExit) },
    "pong" to { difficulty, onExit -> PongGame(difficulty, onExit) },
    "poker" to { difficulty, onExit -> PokerGame(difficulty, onExit) },
    "qix" to { difficulty, onExit -> QixGame(difficulty, onExit) },
    "sim-city-blocks" to { difficulty, onExit -> SimCityBlocksGame(difficulty, onExit) },
    "snakes-2d" to { difficulty, onExit -> SnakesGame(difficulty, onExit) },
    "tap-rhythm" to { difficulty, onExit -> TapRhythmGame(difficulty, onExit) }
    "solitaire-clock-timed" to { difficulty, onExit -> SolitaireClockGame(difficulty, onExit) },
    "solitaire-freecell" to { difficulty, onExit -> FreeCellGame(difficulty, onExit) },
    "solitaire-klondike" to { difficulty, onExit -> SolitaireKlondikeGame(difficulty, onExit) },
    "solitaire-pyramid" to { difficulty, onExit -> PyramidGame(difficulty, onExit) },
    "solitaire-tripeaks" to { difficulty, onExit -> TriPeaksGame(difficulty, onExit) },
    "solitaire-tripeaks-timed" to { difficulty, onExit -> TriPeaksTimedGame(difficulty, onExit) },
    "space-defender" to { difficulty, onExit -> SpaceDefenderGame(difficulty, onExit) },
    "spades" to { difficulty, onExit -> SpadesGame(difficulty, onExit) }
    "space-defender" to { difficulty, onExit -> SpaceDefenderGame(difficulty, onExit) },
    "age-of-empires-lite" to { difficulty, onExit -> AgeOfEmpiresLiteGame(difficulty, onExit) },
    "anomaly-defense" to { difficulty, onExit -> AnomalyDefenseGame(difficulty, onExit) },
    "base-defense" to { difficulty, onExit -> BaseDefenseGame(difficulty, onExit) },
    "empire-skirmish" to { difficulty, onExit -> EmpireSkirmishGame(difficulty, onExit) },
    "randomized-dice-td" to { difficulty, onExit -> RandomizedDiceTdGame(difficulty, onExit) },
    "rogue-caverns" to { difficulty, onExit -> RogueCavernsGame(difficulty, onExit) }
    "treasure-mapper" to { difficulty, onExit -> TreasureMapperGame(difficulty, onExit) }
)
